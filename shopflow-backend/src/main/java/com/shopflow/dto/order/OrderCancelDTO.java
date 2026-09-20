package com.shopflow.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 取消订单请求参数。
 *
 * @author shopflow
 */
@Data
public class OrderCancelDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "取消原因", example = "不想要了")
    @Size(max = 128, message = "取消原因最长 128 位")
    private String reason;
}