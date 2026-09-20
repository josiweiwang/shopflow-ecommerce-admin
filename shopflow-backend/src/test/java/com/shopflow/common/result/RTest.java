package com.shopflow.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一响应体与分页结果单元测试。
 *
 * <p>这类「地基代码」一旦出错会影响所有接口，因此必须有测试兜底。
 *
 * @author shopflow
 */
class RTest {

    @Test
    @DisplayName("成功响应：业务码为 200，数据原样返回")
    void okShouldReturnSuccessCodeAndData() {
        R<String> result = R.ok("shopflow");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("shopflow");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTimestamp()).isPositive();
        assertThat(result.getMessage()).isEqualTo("success");
    }

    @Test
    @DisplayName("成功响应：无数据时 data 为 null")
    void okShouldAllowNullData() {
        R<Void> result = R.ok();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("失败响应：携带业务码与默认提示，data 为空")
    void failShouldReturnBusinessCode() {
        R<Void> result = R.fail(ResultCode.STOCK_INSUFFICIENT);

        assertThat(result.getCode()).isEqualTo(60001);
        assertThat(result.getMessage()).isEqualTo("商品库存不足");
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("失败响应：支持自定义提示覆盖默认文案")
    void failShouldSupportCustomMessage() {
        R<Void> result = R.fail(ResultCode.STOCK_INSUFFICIENT, "商品「机械键盘」库存不足");

        assertThat(result.getCode()).isEqualTo(60001);
        assertThat(result.getMessage()).isEqualTo("商品「机械键盘」库存不足");
    }

    @Test
    @DisplayName("分页结果：总页数按向上取整计算")
    void pageResultShouldComputePagesByCeiling() {
        PageResult<String> page = PageResult.of(List.of("a", "b"), 95L, 1L, 10L);

        assertThat(page.getTotal()).isEqualTo(95L);
        assertThat(page.getPages()).isEqualTo(10L);
        assertThat(page.getRecords()).hasSize(2);
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("分页结果：空数据时总页数为 0")
    void pageResultShouldHandleEmptyData() {
        PageResult<String> page = PageResult.empty(1L, 10L);

        assertThat(page.getTotal()).isZero();
        assertThat(page.getPages()).isZero();
        assertThat(page.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("分页结果：每页条数为 0 时使用默认值，避免除零")
    void pageResultShouldFallbackWhenPageSizeInvalid() {
        PageResult<String> page = PageResult.of(List.of("a"), 1L, 1L, 0L);

        assertThat(page.getPageSize()).isEqualTo(10L);
        assertThat(page.getPages()).isEqualTo(1L);
    }
}