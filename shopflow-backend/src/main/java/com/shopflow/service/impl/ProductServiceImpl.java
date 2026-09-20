package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.enums.ProductStatus;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.ResultCode;
import com.shopflow.common.util.TransactionUtils;
import com.shopflow.convert.ProductConverter;
import com.shopflow.dto.product.ProductQueryDTO;
import com.shopflow.dto.product.ProductSaveDTO;
import com.shopflow.dto.product.ProductUpdateDTO;
import com.shopflow.entity.Category;
import com.shopflow.entity.Inventory;
import com.shopflow.entity.Product;
import com.shopflow.exception.BizException;
import com.shopflow.mapper.CategoryMapper;
import com.shopflow.mapper.InventoryMapper;
import com.shopflow.mapper.ProductMapper;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.ProductService;
import com.shopflow.service.StockCacheService;
import com.shopflow.vo.product.ProductDetailVO;
import com.shopflow.vo.product.ProductPageVO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 商品服务实现。
 *
 * <p>商品详情是典型的「热点读」场景，因此采用 Cache-Aside 策略：
 * <ol>
 *     <li>先查缓存，命中直接返回；</li>
 *     <li>未命中时对不存在的 ID 缓存空值，防止缓存穿透；</li>
 *     <li>回源时用 Redis 分布式锁做互斥，防止热点 key 失效瞬间大量请求同时打到数据库（缓存击穿）；</li>
 *     <li>缓存 TTL 加随机抖动，避免批量 key 同时过期（缓存雪崩）；</li>
 *     <li>写操作后删除缓存（而不是更新缓存），并在事务提交后再删一次（延迟双删）。</li>
 * </ol>
 *
 * @author shopflow
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    /** 商品详情缓存基础有效期 */
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);

    /** 缓存 TTL 随机抖动的秒数上限 */
    private static final int DETAIL_CACHE_JITTER_SECONDS = 300;

    /** 空值占位缓存有效期（防穿透） */
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(5);

    /** 回源互斥锁有效期 */
    private static final Duration REBUILD_LOCK_TTL = Duration.ofSeconds(10);

    /** 未抢到锁时的等待重试次数 */
    private static final int LOCK_WAIT_RETRY_TIMES = 3;

    /** 未抢到锁时的等待间隔（毫秒） */
    private static final long LOCK_WAIT_INTERVAL_MS = 50L;

    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final InventoryMapper inventoryMapper;

    private final ProductConverter productConverter;

    private final StockCacheService stockCacheService;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisScript<Long> unlockScript;

    private final ObjectMapper objectMapper;

    /** 商品详情缓存开关：压测时关闭以量化缓存收益 */
    private final boolean detailCacheEnabled;

    /** 缓存命中/未命中计数器：让"缓存到底有没有用"变成可观测的数字，而不是靠感觉 */
    private final Counter cacheHitCounter;

    private final Counter cacheMissCounter;

    private final Counter cacheNullHitCounter;

    private final Counter cacheRebuildCounter;

    private final Random random = new Random();

    public ProductServiceImpl(ProductMapper productMapper,
                              CategoryMapper categoryMapper,
                              InventoryMapper inventoryMapper,
                              ProductConverter productConverter,
                              StockCacheService stockCacheService,
                              StringRedisTemplate stringRedisTemplate,
                              @Qualifier("unlockScript") RedisScript<Long> unlockScript,
                              ObjectMapper objectMapper,
                              @Value("${shopflow.product.detail-cache-enabled:true}") boolean detailCacheEnabled,
                              MeterRegistry meterRegistry) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.inventoryMapper = inventoryMapper;
        this.productConverter = productConverter;
        this.stockCacheService = stockCacheService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.unlockScript = unlockScript;
        this.objectMapper = objectMapper;
        this.detailCacheEnabled = detailCacheEnabled;
        this.cacheHitCounter = meterRegistry.counter("shopflow.product.cache.hit");
        this.cacheMissCounter = meterRegistry.counter("shopflow.product.cache.miss");
        this.cacheNullHitCounter = meterRegistry.counter("shopflow.product.cache.null_hit");
        this.cacheRebuildCounter = meterRegistry.counter("shopflow.product.cache.rebuild");
    }

    @Override
    public PageResult<ProductPageVO> page(ProductQueryDTO query) {
        Page<ProductPageVO> page = new Page<>(query.safePageNum(), query.safePageSize());
        IPage<ProductPageVO> result = productMapper.selectPageByCondition(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ProductDetailVO detail(Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "商品ID不能为空");
        }
        if (!detailCacheEnabled) {
            // 压测对比用：关闭缓存后每次请求都回源数据库，用于量化缓存带来的收益
            return loadDetailFromDb(id);
        }
        String cacheKey = CacheKeys.productDetail(id);
        ProductDetailVO cached = readDetailCache(cacheKey);
        if (cached != null) {
            cacheHitCounter.increment();
            return cached;
        }
        cacheMissCounter.increment();
        if (hasNullPlaceholder(id)) {
            cacheNullHitCounter.increment();
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }

        String lockKey = CacheKeys.productRebuildLock(id);
        String lockToken = UUID.randomUUID().toString();
        boolean locked = tryLock(lockKey, lockToken);
        try {
            if (locked) {
                // 双重检查：可能在我抢锁期间已有其他线程回填了缓存
                ProductDetailVO doubleCheck = readDetailCache(cacheKey);
                if (doubleCheck != null) {
                    return doubleCheck;
                }
                cacheRebuildCounter.increment();
                ProductDetailVO loaded = loadDetailFromDb(id);
                writeDetailCache(cacheKey, loaded);
                return loaded;
            }
            // 未抢到锁：短暂轮询等待持有者回填缓存，避免大量请求同时涌向数据库
            for (int i = 0; i < LOCK_WAIT_RETRY_TIMES; i++) {
                sleepQuietly(LOCK_WAIT_INTERVAL_MS);
                ProductDetailVO retry = readDetailCache(cacheKey);
                if (retry != null) {
                    return retry;
                }
            }
            // 兜底：直接回源，可用性优先于缓存保护
            return loadDetailFromDb(id);
        } finally {
            if (locked) {
                releaseLock(lockKey, lockToken);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSaveDTO dto) {
        assertCategoryExists(dto.getCategoryId());
        assertSkuAvailable(dto.getSku(), null);

        Product product = new Product();
        applyBaseFields(product, dto);
        product.setSales(0);
        productMapper.insert(product);

        // 商品与库存一起创建：避免出现「有商品没库存」的脏数据
        int initStock = dto.getInitStock() == null ? 0 : dto.getInitStock();
        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setTotalStock(initStock);
        inventory.setAvailableStock(initStock);
        inventory.setLockedStock(0);
        inventory.setWarnStock(dto.getWarnStock() == null ? 10 : dto.getWarnStock());
        inventory.setVersion(0);
        inventoryMapper.insert(inventory);

        TransactionUtils.afterCommit(() -> stockCacheService.initIfAbsent(product.getId(), initStock));
        log.info("新增商品成功 | productId={} | sku={} | initStock={} | operatorId={}",
                product.getId(), product.getSku(), initStock, SecurityUtils.getUserIdOrZero());
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProductUpdateDTO dto) {
        getExisting(dto.getId());
        assertCategoryExists(dto.getCategoryId());
        assertSkuAvailable(dto.getSku(), dto.getId());

        Product update = new Product();
        update.setId(dto.getId());
        applyBaseFields(update, dto);
        productMapper.updateById(update);

        // 延迟双删：先删一次，事务提交后再删一次
        evictDetail(dto.getId());
        TransactionUtils.afterCommit(() -> evictDetail(dto.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getExisting(id);
        productMapper.softDeleteById(id);
        TransactionUtils.afterCommit(() -> {
            evictDetail(id);
            stockCacheService.evict(id);
        });
        log.info("删除商品成功 | productId={} | operatorId={}", id, SecurityUtils.getUserIdOrZero());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Product existing = getExisting(id);
        if (ProductStatus.fromCode(status) == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "商品状态不合法");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(status);
        productMapper.updateById(update);
        TransactionUtils.afterCommit(() -> evictDetail(id));
        log.info("商品状态变更 | productId={} | {} -> {} | operatorId={}",
                id, existing.getStatus(), status, SecurityUtils.getUserIdOrZero());
    }

    // ==================== 内部方法 ====================

    private void applyBaseFields(Product product, ProductSaveDTO dto) {
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setSku(dto.getSku());
        product.setSubtitle(dto.getSubtitle() == null ? "" : dto.getSubtitle());
        product.setMainImage(dto.getMainImage() == null ? "" : dto.getMainImage());
        product.setDetail(dto.getDetail());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice() == null ? dto.getPrice() : dto.getOriginalPrice());
        product.setStatus(dto.getStatus() == null ? ProductStatus.OFF_SHELF.getCode() : dto.getStatus());
        product.setSort(dto.getSort() == null ? 0 : dto.getSort());
    }

    private Product getExisting(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void assertCategoryExists(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }
    }

    private void assertSkuAvailable(String sku, Long excludeId) {
        Long count = productMapper.selectCount(Wrappers.lambdaQuery(Product.class)
                .eq(Product::getSku, sku)
                .ne(excludeId != null, Product::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.SKU_EXISTS);
        }
    }

    /**
     * 回源数据库组装商品详情，并顺带预热库存缓存。
     */
    private ProductDetailVO loadDetailFromDb(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            // 缓存空值，防止同一个不存在的 ID 被反复查询（缓存穿透）
            cacheNullPlaceholder(id);
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        Inventory inventory = inventoryMapper.selectOne(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getProductId, id));
        ProductDetailVO vo = productConverter.toDetailVO(product);
        if (inventory != null) {
            vo.setAvailableStock(inventory.getAvailableStock());
            vo.setLockedStock(inventory.getLockedStock());
            vo.setTotalStock(inventory.getTotalStock());
            vo.setWarnStock(inventory.getWarnStock());
            // SETNX 预热：不会覆盖已经发生的扣减结果
            stockCacheService.initIfAbsent(id, inventory.getAvailableStock());
        }
        return vo;
    }

    private ProductDetailVO readDetailCache(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, ProductDetailVO.class);
        } catch (Exception ex) {
            log.warn("读取商品详情缓存失败，降级查询数据库 | key={} | error={}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void writeDetailCache(String cacheKey, ProductDetailVO vo) {
        if (!detailCacheEnabled) {
            return;
        }
        try {
            long ttlSeconds = DETAIL_CACHE_TTL.toSeconds() + random.nextInt(DETAIL_CACHE_JITTER_SECONDS);
            stringRedisTemplate.opsForValue()
                    .set(cacheKey, objectMapper.writeValueAsString(vo), Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("写入商品详情缓存失败 | key={} | error={}", cacheKey, ex.getMessage());
        }
    }

    private void cacheNullPlaceholder(Long productId) {
        if (!detailCacheEnabled) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(CacheKeys.productDetailNull(productId), "1", NULL_CACHE_TTL);
        } catch (Exception ex) {
            log.warn("写入商品空值缓存失败 | productId={} | error={}", productId, ex.getMessage());
        }
    }

    private boolean hasNullPlaceholder(Long productId) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(CacheKeys.productDetailNull(productId)));
        } catch (Exception ex) {
            return false;
        }
    }

    private void evictDetail(Long productId) {
        try {
            stringRedisTemplate.delete(List.of(CacheKeys.productDetail(productId),
                    CacheKeys.productDetailNull(productId)));
        } catch (Exception ex) {
            log.warn("删除商品缓存失败 | productId={} | error={}", productId, ex.getMessage());
        }
    }

    private boolean tryLock(String lockKey, String token) {
        try {
            return Boolean.TRUE.equals(
                    stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, REBUILD_LOCK_TTL));
        } catch (Exception ex) {
            log.warn("获取缓存重建锁失败，直接回源 | key={} | error={}", lockKey, ex.getMessage());
            return false;
        }
    }

    private void releaseLock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(unlockScript, List.of(lockKey), token);
        } catch (Exception ex) {
            log.warn("释放缓存重建锁失败 | key={} | error={}", lockKey, ex.getMessage());
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}