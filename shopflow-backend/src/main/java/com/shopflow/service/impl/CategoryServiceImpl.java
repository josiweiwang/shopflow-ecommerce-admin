package com.shopflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.constant.CommonConstants;
import com.shopflow.common.result.ResultCode;
import com.shopflow.convert.CategoryConverter;
import com.shopflow.dto.category.CategorySaveDTO;
import com.shopflow.dto.category.CategoryUpdateDTO;
import com.shopflow.entity.Category;
import com.shopflow.exception.BizException;
import com.shopflow.mapper.CategoryMapper;
import com.shopflow.mapper.ProductMapper;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.CategoryService;
import com.shopflow.vo.category.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现。
 *
 * <p>分类是典型的「读多写极少」数据：后台几乎每次进入商品页都要拉一次分类树，
 * 因此整棵树缓存在 Redis 中，写操作后删除缓存，由下一次读请求重建。
 *
 * @author shopflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    /** 一级分类 */
    private static final int LEVEL_FIRST = 1;

    /** 二级分类：暂不支持更深层级，避免树查询与商品归属校验复杂化 */
    private static final int LEVEL_SECOND = 2;

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final CategoryMapper categoryMapper;

    private final ProductMapper productMapper;

    private final CategoryConverter categoryConverter;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final Random random = new Random();

    @Override
    public List<CategoryVO> tree() {
        List<CategoryVO> cached = readCache();
        if (cached != null) {
            return cached;
        }
        List<Category> categories = categoryMapper.selectList(Wrappers.lambdaQuery(Category.class)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        List<CategoryVO> tree = buildTree(categories);
        writeCache(tree);
        return tree;
    }

    @Override
    public CategoryVO detail(Long id) {
        Category category = getExisting(id);
        return categoryConverter.toVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategorySaveDTO dto) {
        int level = resolveLevel(dto.getParentId());
        assertNameAvailable(dto.getParentId(), dto.getName(), null);

        Category category = new Category();
        category.setParentId(dto.getParentId());
        category.setName(dto.getName());
        category.setLevel(level);
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setIcon(dto.getIcon() == null ? "" : dto.getIcon());
        category.setStatus(dto.getStatus() == null ? CommonConstants.STATUS_ENABLED : dto.getStatus());
        categoryMapper.insert(category);

        evictCacheAfterCommit();
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CategoryUpdateDTO dto) {
        Category existing = getExisting(dto.getId());
        int level = resolveLevel(dto.getParentId());
        assertNameAvailable(dto.getParentId(), dto.getName(), dto.getId());

        // 自身不能作为自己的父分类
        if (dto.getId().equals(dto.getParentId())) {
            throw new BizException(ResultCode.PARAM_INVALID, "父分类不能是自己");
        }
        // 已有子分类的一级分类不允许再变更层级，否则子分类会变成孤儿
        if (existing.getParentId() == 0 && !dto.getParentId().equals(existing.getParentId())) {
            Long childCount = categoryMapper.selectCount(Wrappers.lambdaQuery(Category.class)
                    .eq(Category::getParentId, dto.getId()));
            if (childCount != null && childCount > 0) {
                throw new BizException(ResultCode.PARAM_INVALID, "该分类下存在子分类，无法变更层级");
            }
        }

        Category update = new Category();
        update.setId(dto.getId());
        update.setParentId(dto.getParentId());
        update.setName(dto.getName());
        update.setLevel(level);
        update.setSort(dto.getSort());
        update.setIcon(dto.getIcon());
        update.setStatus(dto.getStatus());
        categoryMapper.updateById(update);

        evictCacheAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getExisting(id);

        Long childCount = categoryMapper.selectCount(Wrappers.lambdaQuery(Category.class)
                .eq(Category::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(ResultCode.CATEGORY_HAS_PRODUCT, "该分类下存在子分类，无法删除");
        }
        Long productCount = productMapper.countByCategoryId(id);
        if (productCount != null && productCount > 0) {
            throw new BizException(ResultCode.CATEGORY_HAS_PRODUCT);
        }

        categoryMapper.softDeleteById(id);
        evictCacheAfterCommit();
        log.info("删除分类成功 | categoryId={} | operatorId={}", id, SecurityUtils.getUserIdOrZero());
    }

    // ==================== 内部方法 ====================

    private Category getExisting(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    /**
     * 根据父分类解析层级：父为 0 则是一级分类，否则为二级分类。
     * 超过两级直接拒绝，保证树结构始终可控。
     */
    private int resolveLevel(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return LEVEL_FIRST;
        }
        Category parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND, "父分类不存在");
        }
        if (parent.getLevel() != null && parent.getLevel() >= LEVEL_SECOND) {
            throw new BizException(ResultCode.PARAM_INVALID, "分类最多支持两级");
        }
        return LEVEL_SECOND;
    }

    private void assertNameAvailable(Long parentId, String name, Long excludeId) {
        Long count = categoryMapper.selectCount(Wrappers.lambdaQuery(Category.class)
                .eq(Category::getParentId, parentId)
                .eq(Category::getName, name)
                .ne(excludeId != null, Category::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.CATEGORY_NAME_EXISTS);
        }
    }

    /** 一次遍历分组后挂载子节点，避免递归查库（N+1） */
    private List<CategoryVO> buildTree(List<Category> categories) {
        Map<Long, List<CategoryVO>> childrenMap = categories.stream()
                .map(categoryConverter::toVO)
                .collect(Collectors.groupingBy(CategoryVO::getParentId));

        List<CategoryVO> roots = new ArrayList<>(childrenMap.getOrDefault(0L, new ArrayList<>()));
        for (CategoryVO node : childrenMap.values().stream().flatMap(List::stream).toList()) {
            node.setChildren(childrenMap.getOrDefault(node.getId(), new ArrayList<>()));
        }
        roots.sort(Comparator.comparing(CategoryVO::getSort).thenComparing(CategoryVO::getId));
        return roots;
    }

    /**
     * 缓存读取：Redis 不可用时降级查库，不让缓存故障影响可用性。
     */
    private List<CategoryVO> readCache() {
        try {
            String json = stringRedisTemplate.opsForValue().get(CacheKeys.CATEGORY_TREE);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<List<CategoryVO>>() {
            });
        } catch (Exception ex) {
            log.warn("读取分类树缓存失败，降级查询数据库 | error={}", ex.getMessage());
            return null;
        }
    }

    private void writeCache(List<CategoryVO> tree) {
        try {
            // TTL 加随机抖动，避免大量缓存同时失效造成缓存雪崩
            long ttlSeconds = CACHE_TTL.toSeconds() + random.nextInt(600);
            stringRedisTemplate.opsForValue().set(CacheKeys.CATEGORY_TREE,
                    objectMapper.writeValueAsString(tree), Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("写入分类树缓存失败 | error={}", ex.getMessage());
        }
    }

    /**
     * 事务提交后再删缓存（延迟双删的一部分）。
     *
     * <p>如果在事务提交前删缓存，可能出现「缓存已删、事务还没提交」的窗口期：
     * 此时其他请求读到旧数据并回填缓存，导致缓存长期脏。放到提交后删除可显著缩小这个窗口。
     */
    private void evictCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictCache();
            }
        });
    }

    private void evictCache() {
        try {
            stringRedisTemplate.delete(CacheKeys.CATEGORY_TREE);
        } catch (Exception ex) {
            log.warn("删除分类树缓存失败 | error={}", ex.getMessage());
        }
    }
}