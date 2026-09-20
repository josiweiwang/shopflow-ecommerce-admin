package com.shopflow.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单详情：订单主信息 + 商品明细快照 + 状态流转时间线。
 *
 * <p>明细直接使用 order_item 中的快照字段，不 JOIN 商品表，
 * 保证商品后续改价、改名、下架都不会影响历史订单展示与金额。
 *
 * @author shopflow
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderPageVO {

    private String receiverAddress;

    private String remark;

    private String cancelReason;

    private LocalDateTime deliverTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    @Schema(description = "商品明细")
    private List<OrderItemVO> items = new ArrayList<>();

    @Schema(description = "状态流转时间线")
    private List<OrderStatusLogVO> statusLogs = new ArrayList<>();
}