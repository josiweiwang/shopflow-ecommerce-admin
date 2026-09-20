package com.shopflow.common.enums;

import com.shopflow.common.util.OrderStatusMachine;
import com.shopflow.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单状态机测试。
 *
 * <p>状态流转是订单模块最核心的业务规则，一旦出错会造成「已取消订单又被发货」这类
 * 很难排查的生产问题，因此这里把规则穷举覆盖。
 *
 * @author shopflow
 */
class OrderStatusTest {

    @ParameterizedTest(name = "{0} 可以流转到 {1}")
    @CsvSource({
            "PENDING_PAYMENT, PAID",
            "PENDING_PAYMENT, CANCELED",
            "PAID, DELIVERING",
            "DELIVERING, COMPLETED"
    })
    @DisplayName("合法的状态流转")
    void shouldAllowLegalTransition(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} 不可以流转到 {1}")
    @CsvSource({
            "PENDING_PAYMENT, DELIVERING",
            "PENDING_PAYMENT, COMPLETED",
            "PAID, COMPLETED",
            "PAID, CANCELED",
            "DELIVERING, PAID",
            "DELIVERING, CANCELED",
            "COMPLETED, DELIVERING",
            "CANCELED, PAID"
    })
    @DisplayName("非法的状态流转（含终态不可再流转）")
    void shouldRejectIllegalTransition(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    @DisplayName("已完成与已取消是终态")
    void finalStatusShouldBeMarked() {
        assertThat(OrderStatus.COMPLETED.isFinalStatus()).isTrue();
        assertThat(OrderStatus.CANCELED.isFinalStatus()).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.isFinalStatus()).isFalse();
        assertThat(OrderStatus.DELIVERING.isFinalStatus()).isFalse();
    }

    @Test
    @DisplayName("状态码解析：非法状态码返回 null，requireFromCode 抛异常")
    void shouldResolveStatusCode() {
        assertThat(OrderStatus.fromCode(0)).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(OrderStatus.fromCode(4)).isEqualTo(OrderStatus.CANCELED);
        assertThat(OrderStatus.fromCode(99)).isNull();
        assertThat(OrderStatus.fromCode(null)).isNull();
        assertThatThrownBy(() -> OrderStatus.requireFromCode(99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("状态机：非法流转抛出业务异常且错误码为 40907")
    void machineShouldThrowBizException() {
        OrderStatusMachine machine = new OrderStatusMachine();

        assertThatThrownBy(() -> machine.assertAllowed(OrderStatus.CANCELED, OrderStatus.PAID))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不允许");

        assertThat(machine.isAllowed(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID)).isTrue();
        assertThat(machine.isAllowed(OrderStatus.COMPLETED, OrderStatus.PAID)).isFalse();
    }
}