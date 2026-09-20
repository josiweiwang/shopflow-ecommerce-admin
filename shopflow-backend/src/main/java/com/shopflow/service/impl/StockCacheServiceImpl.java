package com.shopflow.service.impl;

import com.shopflow.common.constant.CacheKeys;
import com.shopflow.common.enums.StockDeductResult;
import com.shopflow.service.StockCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 库存缓存实现。
 *
 * <p>所有扣减/释放操作都通过 Lua 脚本在 Redis 内原子执行，
 * 保证并发请求不会出现「同时读到还剩 1 件、各自扣减成功」的超卖问题。
 *
 * @author shopflow
 */
@Slf4j
@Service
public class StockCacheServiceImpl implements StockCacheService {

    /** Lua 返回值：缓存未命中 */
    private static final long RESULT_CACHE_MISS = -1L;

    /** Lua 返回值：执行成功 */
    private static final long RESULT_SUCCESS = 1L;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisScript<Long> stockLockScript;

    private final RedisScript<Long> stockReleaseScript;

    private final boolean redisCacheEnabled;

    public StockCacheServiceImpl(StringRedisTemplate stringRedisTemplate,
                                 @Qualifier("stockLockScript") RedisScript<Long> stockLockScript,
                                 @Qualifier("stockReleaseScript") RedisScript<Long> stockReleaseScript,
                                 @Value("${shopflow.inventory.redis-cache-enabled:true}") boolean redisCacheEnabled) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.stockLockScript = stockLockScript;
        this.stockReleaseScript = stockReleaseScript;
        this.redisCacheEnabled = redisCacheEnabled;
    }

    @Override
    public boolean enabled() {
        return redisCacheEnabled;
    }

    @Override
    public void initIfAbsent(Long productId, int availableStock) {
        if (!redisCacheEnabled) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue()
                    .setIfAbsent(CacheKeys.inventoryStock(productId), String.valueOf(availableStock));
        } catch (Exception ex) {
            log.warn("库存缓存预热失败 | productId={} | error={}", productId, ex.getMessage());
        }
    }

    @Override
    public Integer get(Long productId) {
        if (!redisCacheEnabled) {
            return null;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(CacheKeys.inventoryStock(productId));
            return value == null ? null : Integer.valueOf(value);
        } catch (Exception ex) {
            log.warn("库存缓存读取失败 | productId={} | error={}", productId, ex.getMessage());
            return null;
        }
    }

    @Override
    public StockDeductResult deduct(Long productId, int quantity) {
        if (!redisCacheEnabled) {
            return StockDeductResult.DISABLED;
        }
        try {
            Long result = stringRedisTemplate.execute(stockLockScript,
                    List.of(CacheKeys.inventoryStock(productId)), String.valueOf(quantity));
            if (result == null || result == RESULT_CACHE_MISS) {
                return StockDeductResult.CACHE_MISS;
            }
            return result == RESULT_SUCCESS ? StockDeductResult.SUCCESS : StockDeductResult.INSUFFICIENT;
        } catch (Exception ex) {
            // Redis 故障降级：直接走数据库条件更新，保证下单链路仍然可用
            log.warn("库存预扣异常，降级为数据库扣减 | productId={} | error={}", productId, ex.getMessage());
            return StockDeductResult.DISABLED;
        }
    }

    @Override
    public void release(Long productId, int quantity) {
        if (!redisCacheEnabled || quantity <= 0) {
            return;
        }
        try {
            stringRedisTemplate.execute(stockReleaseScript,
                    List.of(CacheKeys.inventoryStock(productId)), String.valueOf(quantity));
        } catch (Exception ex) {
            log.warn("库存缓存释放失败 | productId={} | quantity={} | error={}",
                    productId, quantity, ex.getMessage());
        }
    }

    @Override
    public void evict(Long productId) {
        try {
            stringRedisTemplate.delete(CacheKeys.inventoryStock(productId));
        } catch (Exception ex) {
            log.warn("库存缓存删除失败 | productId={} | error={}", productId, ex.getMessage());
        }
    }
}