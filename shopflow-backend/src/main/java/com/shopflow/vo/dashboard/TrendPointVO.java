package com.shopflow.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势图上的一个数据点。
 *
 * <p>数据库只返回有订单的日期，缺失的日期由业务层补零，
 * 这样前端折线图不会出现断点。
 *
 * @author shopflow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "日期", example = "2026-09-20")
    private String date;

    @Schema(description = "订单数")
    private Long orderCount;

    @Schema(description = "成交额")
    private BigDecimal amount;
}