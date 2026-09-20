package com.shopflow.controller;

import com.shopflow.aspect.OperationLog;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.R;
import com.shopflow.dto.order.OrderCancelDTO;
import com.shopflow.dto.order.OrderCreateDTO;
import com.shopflow.dto.order.OrderQueryDTO;
import com.shopflow.dto.order.OrderStatusUpdateDTO;
import com.shopflow.security.LoginRequired;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.OrderService;
import com.shopflow.vo.order.OrderCreateVO;
import com.shopflow.vo.order.OrderDetailVO;
import com.shopflow.vo.order.OrderPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 *
 * <p>权限设计：下单、支付、取消属于「登录即可」的前台能力；
 * 只有后台的状态流转（发货、完成）需要 order:update 权限。
 * 数据归属由服务层统一校验，防止水平越权。
 *
 * @author shopflow
 */
@Tag(name = "订单管理", description = "下单、支付、取消、查询与状态流转")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单", description = "幂等 + Redis 预扣库存 + 数据库条件更新双保险")
    @LoginRequired
    @PostMapping
    public R<OrderCreateVO> create(@RequestBody @Valid OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @Operation(summary = "订单分页查询", description = "前台只能查询自己的订单，管理员可查询全部")
    @LoginRequired
    @GetMapping
    public R<PageResult<OrderPageVO>> page(OrderQueryDTO query) {
        return R.ok(orderService.page(query));
    }

    @Operation(summary = "订单详情", description = "含商品快照明细与状态流转时间线")
    @LoginRequired
    @GetMapping("/{orderNo}")
    public R<OrderDetailVO> detail(@PathVariable String orderNo) {
        return R.ok(orderService.detail(orderNo));
    }

    @Operation(summary = "支付订单", description = "模拟支付：锁定库存转实际扣减并累加销量")
    @LoginRequired
    @OperationLog(module = "订单管理", operation = "支付订单")
    @PostMapping("/{orderNo}/pay")
    public R<Void> pay(@PathVariable String orderNo) {
        orderService.pay(orderNo);
        return R.ok();
    }

    @Operation(summary = "取消订单", description = "仅待支付订单可取消，取消后释放锁定库存")
    @LoginRequired
    @OperationLog(module = "订单管理", operation = "取消订单")
    @PostMapping("/{orderNo}/cancel")
    public R<Void> cancel(@PathVariable String orderNo, @RequestBody(required = false) OrderCancelDTO dto) {
        orderService.cancel(orderNo, dto);
        return R.ok();
    }

    @Operation(summary = "修改订单状态", description = "后台发货、确认收货；状态机校验非法流转")
    @RequirePermission("order:update")
    @OperationLog(module = "订单管理", operation = "修改订单状态")
    @PatchMapping("/{orderNo}/status")
    public R<Void> updateStatus(@PathVariable String orderNo, @RequestBody @Valid OrderStatusUpdateDTO dto) {
        orderService.updateStatus(orderNo, dto);
        return R.ok();
    }
}