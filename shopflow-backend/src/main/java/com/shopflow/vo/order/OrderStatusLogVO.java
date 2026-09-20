package com.shopflow.vo.order;

import com.shopflow.common.enums.OrderOperatorType;
import com.shopflow.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单状态流转记录（用于订单详情时间线）。
 *
 * @author shopflow
 */
@Data
public class OrderStatusLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer fromStatus;

    private Integer toStatus;

    @Schema(description = "操作人类型：1-用户 2-管理员 3-系统")
    private Integer operatorType;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;

    public String getFromStatusDesc() {
        OrderStatus status = OrderStatus.fromCode(this.fromStatus);
        return status == null ? "-" : status.getDescription();
    }

    public String getToStatusDesc() {
        OrderStatus status = OrderStatus.fromCode(this.toStatus);
        return status == null ? "" : status.getDescription();
    }

    public String getOperatorTypeDesc() {
        if (operatorType == null) {
            return "";
        }
        for (OrderOperatorType type : OrderOperatorType.values()) {
            if (type.getCode() == operatorType) {
                return type.getDescription();
            }
        }
        return "";
    }
}