package com.shopflow.vo.dashboard;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销商品排行项。
 *
 * @author shopflow
 */
@Data
public class TopProductVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String productName;

    @Schema(description = "成交件数")
    private Long quantity;

    @Schema(description = "成交金额")
    private BigDecimal amount;
}