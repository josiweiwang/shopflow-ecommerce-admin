package com.shopflow.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态。
 *
 * <p>状态流转规则集中定义在 {@link #canTransitionTo(OrderStatus)}，
 * 业务代码不允许再自己写 if 判断，避免「某个接口忘了校验状态」导致脏数据。
 *
 * @author shopflow
 */
@Getter
public enum OrderStatus {

    /** 待支付：下单成功、库存已预扣 */
    PENDING_PAYMENT(0, "待支付"),

    /** 已支付：支付成功，锁定库存转为实际扣减 */
    PAID(1, "已支付"),

    /** 配送中：管理员已发货 */
    DELIVERING(2, "配送中"),

    /** 已完成：确认收货，终态 */
    COMPLETED(3, "已完成"),

    /** 已取消：用户取消或超时未支付，库存已释放，终态 */
    CANCELED(4, "已取消");

    private final int code;

    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 判断当前状态是否允许流转到目标状态。
     *
     * <pre>
     * 待支付 -> 已支付 / 已取消
     * 已支付 -> 配送中
     * 配送中 -> 已完成
     * 已完成、已取消 -> 终态，不可再流转
     * </pre>
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELED;
            case PAID -> target == DELIVERING;
            case DELIVERING -> target == COMPLETED;
            case COMPLETED, CANCELED -> false;
        };
    }

    /** 是否为终态 */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELED;
    }

    /** 是否为待支付状态 */
    public boolean isPendingPayment() {
        return this == PENDING_PAYMENT;
    }

    /** 按状态码解析，未知状态返回 null */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(null);
    }

    /** 按状态码解析，未知状态抛出异常，用于「必须存在」的场景 */
    public static OrderStatus requireFromCode(Integer code) {
        OrderStatus status = fromCode(code);
        if (status == null) {
            throw new IllegalArgumentException("未知的订单状态码: " + code);
        }
        return status;
    }
}