package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopflow.common.constant.CommonConstants;
import com.shopflow.common.enums.InventoryBizType;
import com.shopflow.common.enums.StockDeductResult;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.ResultCode;
import com.shopflow.common.util.TransactionUtils;
import com.shopflow.convert.InventoryConverter;
import com.shopflow.dto.inventory.InventoryAdjustDTO;
import com.shopflow.dto.inventory.InventoryInboundDTO;
import com.shopflow.dto.inventory.InventoryLogQueryDTO;
import com.shopflow.dto.inventory.InventoryQueryDTO;
import com.shopflow.entity.Inventory;
import com.shopflow.entity.InventoryLog;
import com.shopflow.entity.Product;
import com.shopflow.exception.BizException;
import com.shopflow.mapper.InventoryLogMapper;
import com.shopflow.mapper.InventoryMapper;
import com.shopflow.mapper.ProductMapper;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.InventoryService;
import com.shopflow.service.StockCacheService;
import com.shopflow.vo.inventory.InventoryLogVO;
import com.shopflow.vo.inventory.InventoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存服务实现。
 *
 * <p>防超卖的分工：
 * <ol>
 *     <li>Redis Lua 原子预扣 —— 第一道防线，挡住绝大多数并发；</li>
 *     <li>MySQL 条件更新（{@code AND available_stock >= #{quantity}}）—— 最终防线，
 *         缓存失效、数据不一致或 Redis 故障时依旧不会出现负库存；</li>
 *     <li>数据库 CHECK 约束与库存流水对账 —— 兜底。</li>
 * </ol>
 * 三层都通过才会返回下单成功，任何一层失败都会抛出业务异常并回滚事务。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;

    private final InventoryLogMapper inventoryLogMapper;

    private final ProductMapper productMapper;

    private final InventoryConverter inventoryConverter;

    private final StockCacheService stockCacheService;

    @Override
    public InventoryVO getByProductId(Long productId) {
        Inventory inventory = getExisting(productId);
        InventoryVO vo = inventoryConverter.toVO(inventory);

        Product product = productMapper.selectById(productId);
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setSku(product.getSku());
        }
        // 下单是「先扣 Redis 再改库」，因此缓存值可能领先于数据库值；
        // 这里优先返回缓存值，并显式标记来源，避免使用者误判。
        Integer cachedStock = stockCacheService.get(productId);
        if (cachedStock != null) {
            vo.setAvailableStock(cachedStock);
            vo.setFromCache(true);
        } else {
            vo.setFromCache(false);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long productId, InventoryInboundDTO dto) {
        getExisting(productId);
        int changed = inventoryMapper.adjustStock(productId, dto.getQuantity());
        if (changed == 0) {
            throw new BizException(ResultCode.INVENTORY_NOT_FOUND, "入库失败，库存记录不存在");
        }
        Inventory after = getExisting(productId);
        writeLog(productId, "", InventoryBizType.INBOUND, dto.getQuantity(),
                after.getAvailableStock() - dto.getQuantity(), after.getAvailableStock(),
                buildRemark(dto.getRemark(), "入库"));
        evictCacheAfterCommit(productId);
        log.info("商品入库 | productId={} | quantity={} | availableStock={} | operatorId={}",
                productId, dto.getQuantity(), after.getAvailableStock(), SecurityUtils.getUserIdOrZero());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long productId, InventoryAdjustDTO dto) {
        if (dto.getDelta() == null || dto.getDelta() == 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "调整数量不能为 0");
        }
        Inventory before = getExisting(productId);
        int changed = inventoryMapper.adjustStock(productId, dto.getDelta());
        if (changed == 0) {
            throw new BizException(ResultCode.STOCK_INSUFFICIENT, "调整后可用库存不能为负数");
        }
        Inventory after = getExisting(productId);
        writeLog(productId, "", InventoryBizType.ADJUST, dto.getDelta(),
                before.getAvailableStock(), after.getAvailableStock(),
                buildRemark(dto.getRemark(), "盘点调整"));
        evictCacheAfterCommit(productId);
        log.info("库存盘点调整 | productId={} | delta={} | {} -> {} | operatorId={}",
                productId, dto.getDelta(), before.getAvailableStock(), after.getAvailableStock(),
                SecurityUtils.getUserIdOrZero());
    }

    @Override
    public PageResult<InventoryVO> page(InventoryQueryDTO query) {
        Page<Inventory> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<Inventory> wrapper = Wrappers.lambdaQuery(Inventory.class)
                .eq(query.getProductId() != null, Inventory::getProductId, query.getProductId())
                .apply(Boolean.TRUE.equals(query.getLowStockOnly()),
                        "available_stock <= warn_stock")
                .orderByAsc(Inventory::getAvailableStock);
        Page<Inventory> result = inventoryMapper.selectPage(page, wrapper);

        List<InventoryVO> records = result.getRecords().stream()
                .map(inventoryConverter::toVO)
                .toList();
        fillProductInfo(records);
        records.forEach(vo -> vo.setFromCache(false));
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<InventoryLogVO> pageLogs(InventoryLogQueryDTO query) {
        Page<InventoryLog> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<InventoryLog> wrapper = Wrappers.lambdaQuery(InventoryLog.class)
                .eq(query.getProductId() != null, InventoryLog::getProductId, query.getProductId())
                .eq(query.getBizType() != null, InventoryLog::getBizType, query.getBizType())
                .eq(query.getOrderNo() != null && !query.getOrderNo().isBlank(),
                        InventoryLog::getOrderNo, query.getOrderNo())
                .orderByDesc(InventoryLog::getId);
        Page<InventoryLog> result = inventoryLogMapper.selectPage(page, wrapper);

        List<InventoryLogVO> records = result.getRecords().stream()
                .map(inventoryConverter::toLogVO)
                .toList();
        fillProductName(records);
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Long productId, int quantity, String orderNo) {
        // 第一道防线：Redis 原子预扣
        StockDeductResult cacheResult = stockCacheService.deduct(productId, quantity);
        if (cacheResult == StockDeductResult.INSUFFICIENT) {
            return false;
        }
        if (cacheResult == StockDeductResult.CACHE_MISS) {
            // 缓存未命中：用数据库当前值预热后重试一次，避免首次请求直接落库
            Inventory inventory = getExisting(productId);
            stockCacheService.initIfAbsent(productId, inventory.getAvailableStock());
            StockDeductResult retry = stockCacheService.deduct(productId, quantity);
            if (retry == StockDeductResult.INSUFFICIENT) {
                return false;
            }
            if (retry == StockDeductResult.SUCCESS) {
                return applyDbLock(productId, quantity, orderNo, true);
            }
        }
        if (cacheResult == StockDeductResult.SUCCESS) {
            return applyDbLock(productId, quantity, orderNo, true);
        }
        // 缓存被禁用或 Redis 异常：直接走数据库条件更新
        return applyDbLock(productId, quantity, orderNo, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmDeduct(Long productId, int quantity, String orderNo) {
        Inventory before = getExisting(productId);
        int changed = inventoryMapper.confirmDeduct(productId, quantity);
        if (changed == 0) {
            throw new BizException(ResultCode.DATA_VERSION_CONFLICT,
                    "库存扣减失败，锁定库存不足，orderNo=" + orderNo);
        }
        Inventory after = getExisting(productId);
        writeLog(productId, orderNo, InventoryBizType.DEDUCT, -quantity,
                before.getAvailableStock(), after.getAvailableStock(), "支付成功，锁定库存转为实际扣减");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(Long productId, int quantity, String orderNo, String remark) {
        Inventory before = getExisting(productId);
        int changed = inventoryMapper.releaseStock(productId, quantity);
        if (changed == 0) {
            throw new BizException(ResultCode.DATA_VERSION_CONFLICT,
                    "库存释放失败，锁定库存不足，orderNo=" + orderNo);
        }
        Inventory after = getExisting(productId);
        // 缓存同步加回可用库存，保持缓存与数据库一致
        stockCacheService.release(productId, quantity);
        writeLog(productId, orderNo, InventoryBizType.RELEASE, quantity,
                before.getAvailableStock(), after.getAvailableStock(), remark);
    }

    // ==================== 内部方法 ====================

    /**
     * 执行数据库条件更新。
     *
     * @param rollbackCache 数据库扣减失败时是否需要把 Redis 预扣的数量加回去
     */
    private boolean applyDbLock(Long productId, int quantity, String orderNo, boolean rollbackCache) {
        int changed = inventoryMapper.lockStock(productId, quantity);
        if (changed == 0) {
            if (rollbackCache) {
                // 缓存预扣成功但数据库不足，说明缓存与数据库不一致，必须把缓存加回去
                stockCacheService.release(productId, quantity);
                stockCacheService.evict(productId);
                log.warn("缓存与数据库库存不一致，已回滚缓存预扣 | productId={} | quantity={} | orderNo={}",
                        productId, quantity, orderNo);
            }
            return false;
        }
        Inventory after = getExisting(productId);
        writeLog(productId, orderNo, InventoryBizType.LOCK, -quantity,
                after.getAvailableStock() + quantity, after.getAvailableStock(), "下单锁定库存");
        return true;
    }

    private Inventory getExisting(Long productId) {
        Inventory inventory = inventoryMapper.selectOne(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getProductId, productId));
        if (inventory == null) {
            throw new BizException(ResultCode.INVENTORY_NOT_FOUND);
        }
        return inventory;
    }

    private void writeLog(Long productId, String orderNo, InventoryBizType bizType, int quantity,
                          int beforeAvailable, int afterAvailable, String remark) {
        InventoryLog logEntity = new InventoryLog();
        logEntity.setProductId(productId);
        logEntity.setOrderNo(orderNo == null ? "" : orderNo);
        logEntity.setBizType(bizType.getCode());
        logEntity.setQuantity(quantity);
        logEntity.setBeforeAvailable(beforeAvailable);
        logEntity.setAfterAvailable(afterAvailable);
        logEntity.setOperatorId(SecurityUtils.getUserIdOrZero());
        logEntity.setRemark(remark == null ? "" : remark);
        inventoryLogMapper.insert(logEntity);
    }

    private void evictCacheAfterCommit(Long productId) {
        TransactionUtils.afterCommit(() -> stockCacheService.evict(productId));
    }

    private String buildRemark(String remark, String defaultRemark) {
        return (remark == null || remark.isBlank()) ? defaultRemark : remark;
    }

    private void fillProductInfo(List<InventoryVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> productIds = records.stream().map(InventoryVO::getProductId).toList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
        records.forEach(vo -> {
            Product product = productMap.get(vo.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setSku(product.getSku());
            }
        });
    }

    private void fillProductName(List<InventoryLogVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> productIds = records.stream()
                .map(InventoryLogVO::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return;
        }
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
        records.forEach(vo -> {
            Product product = productMap.get(vo.getProductId());
            vo.setProductName(product == null ? "商品已删除" : product.getName());
        });
    }
}