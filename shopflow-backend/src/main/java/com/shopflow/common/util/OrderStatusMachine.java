package com.shopflow.common.util;

import com.shopflow.common.enums.OrderStatus;
import com.shopflow.common.result.ResultCode;
import com.shopflow.exception.BizException;
import org.springframework.stereotype.Component;

/**
 * 订单状态机。
 *
 * <p>把「哪些状态可以流转到哪些状态」这一条业务规则收敛到一个地方：
 * <ul>
 *     <li>业务代码不再散落 if-else 判断，新同学看一个类就能理解订单生命周期；</li>
 *     <li>非法流转统一抛出 {@code 40907}，错误码与提示稳定；</li>
 *     <li>状态规则变化时只改一处，且已有单元测试兜底。</li>
 * </ul>
 *
 * @author shopflow
 */
@Component
public class OrderStatusMachine {

    /**
     * 校验状态流转是否合法，不合法直接抛业务异常。
     */
    public void assertAllowed(OrderStatus from, OrderStatus to) {
        if (from == null || to == null || !from.canTransitionTo(to)) {
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, String.format(
                    "订单状态不允许从【%s】变更为【%s】", describe(from), describe(to)));
        }
    }

    /** 判断状态流转是否合法（不抛异常，用于分支判断） */
    public boolean isAllowed(OrderStatus from, OrderStatus to) {
        return from != null && to != null && from.canTransitionTo(to);
    }

    private String describe(OrderStatus status) {
        return status == null ? "未知状态" : status.getDescription();
    }
}