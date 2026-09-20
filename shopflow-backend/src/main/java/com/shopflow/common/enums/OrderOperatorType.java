package com.shopflow.common.enums;

import lombok.Getter;

/**
 * 订单操作人类型（对应 order_status_log.operator_type）。
 *
 * @author shopflow
 */
@Getter
public enum OrderOperatorType {

    /** 前台用户操作 */
    USER(1, "用户"),

    /** 后台管理员操作 */
    ADMIN(2, "管理员"),

    /** 系统自动处理（超时关单等） */
    SYSTEM(3, "系统");

    private final int code;

    private final String description;

    OrderOperatorType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}