package com.shopflow.common.enums;

import lombok.Getter;

/**
 * 商品上下架状态。
 *
 * @author shopflow
 */
@Getter
public enum ProductStatus {

    /** 下架：不可被下单 */
    OFF_SHELF(0, "下架"),

    /** 上架：可被下单 */
    ON_SHELF(1, "上架");

    private final int code;

    private final String description;

    ProductStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ProductStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProductStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public boolean isOnShelf() {
        return this == ON_SHELF;
    }
}