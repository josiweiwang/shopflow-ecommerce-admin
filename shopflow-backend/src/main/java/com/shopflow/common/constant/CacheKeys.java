package com.shopflow.common.constant;

/**
 * Redis 缓存 Key 规范。
 *
 * <p>命名规则：{@code shopflow:{业务模块}:{数据类型}:{标识}}。
 * 统一前缀便于运维按模块批量清理，也避免与其他应用共用 Redis 时冲突。
 *
 * @author shopflow
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** 全局前缀 */
    public static final String PREFIX = "shopflow:";

    // ==================== 认证 ====================
    /** 登录用户上下文（含角色与权限）：shopflow:auth:login:{userId} */
    public static final String AUTH_LOGIN_USER = PREFIX + "auth:login:";
    /** 刷新令牌：shopflow:auth:refresh:{userId} */
    public static final String AUTH_REFRESH = PREFIX + "auth:refresh:";
    /** 令牌黑名单：shopflow:auth:blacklist:{jti} */
    public static final String AUTH_BLACKLIST = PREFIX + "auth:blacklist:";

    // ==================== 商品 ====================
    /** 商品详情：shopflow:product:detail:{productId} */
    public static final String PRODUCT_DETAIL = PREFIX + "product:detail:";
    /** 商品详情空值占位（防穿透） */
    public static final String PRODUCT_DETAIL_NULL = PREFIX + "product:detail:null:";
    /** 商品缓存回源互斥锁：shopflow:product:lock:{productId} */
    public static final String PRODUCT_REBUILD_LOCK = PREFIX + "product:lock:";
    /** 分类树：shopflow:category:tree */
    public static final String CATEGORY_TREE = PREFIX + "category:tree";

    // ==================== 库存 ====================
    /** 商品可用库存：shopflow:inventory:stock:{productId} */
    public static final String INVENTORY_STOCK = PREFIX + "inventory:stock:";
    /** 库存缓存重建锁：shopflow:inventory:lock:{productId} */
    public static final String INVENTORY_LOCK = PREFIX + "inventory:lock:";

    // ==================== 订单 ====================
    /** 订单超时关单延时队列（ZSet，score 为到期时间戳） */
    public static final String ORDER_TIMEOUT_ZSET = PREFIX + "order:timeout";
    /** 下单幂等：shopflow:order:idempotent:{userId}:{requestNo} */
    public static final String ORDER_IDEMPOTENT = PREFIX + "order:idempotent:";
    /** 订单号序列：shopflow:order:seq:{yyyyMMddHHmmss} */
    public static final String ORDER_NO_SEQ = PREFIX + "order:seq:";

    // ==================== 统计 ====================
    /** Dashboard 概览缓存：shopflow:dashboard:overview */
    public static final String DASHBOARD_OVERVIEW = PREFIX + "dashboard:overview";

    // ==================== Key 拼接方法 ====================

    public static String authLoginUser(Long userId) {
        return AUTH_LOGIN_USER + userId;
    }

    public static String authRefresh(Long userId) {
        return AUTH_REFRESH + userId;
    }

    public static String authBlacklist(String jti) {
        return AUTH_BLACKLIST + jti;
    }

    public static String productDetail(Long productId) {
        return PRODUCT_DETAIL + productId;
    }

    public static String productDetailNull(Long productId) {
        return PRODUCT_DETAIL_NULL + productId;
    }

    public static String productRebuildLock(Long productId) {
        return PRODUCT_REBUILD_LOCK + productId;
    }

    public static String inventoryStock(Long productId) {
        return INVENTORY_STOCK + productId;
    }

    public static String inventoryLock(Long productId) {
        return INVENTORY_LOCK + productId;
    }

    public static String orderIdempotent(Long userId, String requestNo) {
        return ORDER_IDEMPOTENT + userId + ":" + requestNo;
    }

    public static String orderNoSeq(String timeKey) {
        return ORDER_NO_SEQ + timeKey;
    }
}