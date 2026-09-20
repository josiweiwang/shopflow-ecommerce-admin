package com.shopflow.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 创建订单请求参数。
 *
 * <p>{@code requestNo} 是客户端生成的幂等号：用户重复点击提交或网络重试时，
 * 服务端凭它保证同一笔请求只生成一个订单。
 *
 * @author shopflow
 */
@Data
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户端幂等号，同一次下单必须保持一致", example = "req-20260920-0001")
    @NotBlank(message = "幂等号不能为空")
    @Size(max = 64, message = "幂等号最长 64 位")
    private String requestNo;

    @Schema(description = "收货人姓名", example = "张三")
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 32, message = "收货人姓名最长 32 位")
    private String receiverName;

    @Schema(description = "收货人电话", example = "13800000000")
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货人电话格式不正确")
    private String receiverPhone;

    @Schema(description = "收货地址", example = "广东省深圳市南山区科技园")
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "收货地址最长 255 位")
    private String receiverAddress;

    @Schema(description = "订单备注")
    @Size(max = 255, message = "备注最长 255 位")
    private String remark;

    @Schema(description = "商品明细")
    @NotEmpty(message = "订单商品不能为空")
    @Size(max = 50, message = "单笔订单最多 50 种商品")
    @Valid
    private List<OrderItemCreateDTO> items;
}