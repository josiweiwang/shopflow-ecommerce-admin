package com.shopflow.common.enums;

/**
 * Redis 库存预扣结果。
 *
 * <p>把「缓存未命中」与「库存不足」区分开，是因为两者的处理方式完全不同：
 * 前者需要回源数据库预热缓存，后者应当直接拒绝下单。
 *
 * @author shopflow
 */
public enum StockDeductResult {

    /** 预扣成功 */
    SUCCESS,

    /** 库存不足 */
    INSUFFICIENT,

    /** 缓存未命中，需要回源预热 */
    CACHE_MISS,

    /** 未启用 Redis 库存缓存，直接走数据库条件更新 */
    DISABLED
}