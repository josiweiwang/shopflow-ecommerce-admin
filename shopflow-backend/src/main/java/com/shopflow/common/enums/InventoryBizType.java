package com.shopflow.common.enums;

import lombok.Getter;

/**
 * 库存变更业务类型（对应 inventory_log.biz_type）。
 *
 * @author shopflow
 */
@Getter
public enum InventoryBizType {

    /** 入库：可用库存增加 */
    INBOUND(1, "入库"),

    /** 锁定：下单预占，可用减少、锁定增加 */
    LOCK(2, "锁定"),

    /** 扣减：支付成功，锁定与总库存同时减少 */
    DEDUCT(3, "扣减"),

    /** 释放：取消订单或超时关单，锁定回滚为可用 */
    RELEASE(4, "释放"),

    /** 盘点调整：管理员人工修正库存 */
    ADJUST(5, "盘点调整");

    private final int code;

    private final String description;

    InventoryBizType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InventoryBizType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (InventoryBizType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}