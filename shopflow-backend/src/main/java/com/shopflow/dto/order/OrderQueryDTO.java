package com.shopflow.dto.order;

import com.shopflow.dto.common.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 订单分页查询条件。
 *
 * <p>{@code userId} 由服务端根据当前登录人强制设置，前台用户无法通过参数越权查看他人订单。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQueryDTO extends PageQuery {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "下单用户ID，仅管理员可用")
    private Long userId;

    @Schema(description = "订单状态：0-待支付 1-已支付 2-配送中 3-已完成 4-已取消")
    private Integer status;

    @Schema(description = "创建时间起", example = "2026-09-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "创建时间止", example = "2026-09-30 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}