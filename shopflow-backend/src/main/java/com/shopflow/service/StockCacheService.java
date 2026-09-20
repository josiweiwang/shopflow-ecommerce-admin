package com.shopflow.service;

import com.shopflow.common.enums.StockDeductResult;

/**
 * Redis 库存缓存服务。
 *
 * <p>职责边界：本接口只负责「缓存层」，不负责数据库。
 * 数据库的条件更新（InventoryMapper）永远是最终事实来源，
 * 缓存只是把并发压力挡在前面。
 *
 * @author shopflow
 */
public interface StockCacheService {

    /** 是否启用 Redis 库存缓存 */
    boolean enabled();

    /**
     * 缓存不存在时写入库存值（SETNX）。
     *
     * <p>刻意使用 SETNX 而不是 SET：下单流程是「先扣 Redis、再改数据库」，
     * 如果这里用 SET 覆盖，一个正在读库的请求就可能把已经扣减过的库存值写回去，
     * 造成「扣了又涨回去」的假象。
     */
    void initIfAbsent(Long productId, int availableStock);

    /** 读取缓存库存，未命中返回 null */
    Integer get(Long productId);

    /**
     * 原子预扣库存。
     *
     * @return SUCCESS 成功；INSUFFICIENT 库存不足；CACHE_MISS 需要回源；DISABLED 未启用缓存
     */
    StockDeductResult deduct(Long productId, int quantity);

    /** 释放库存（取消订单、超时关单、数据库扣减失败时回滚） */
    void release(Long productId, int quantity);

    /** 删除缓存（库存变更后调用，由下次读取重建） */
    void evict(Long productId);
}