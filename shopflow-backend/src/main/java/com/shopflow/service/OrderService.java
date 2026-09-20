package com.shopflow.service;

import com.shopflow.common.result.PageResult;
import com.shopflow.dto.order.OrderCancelDTO;
import com.shopflow.dto.order.OrderCreateDTO;
import com.shopflow.dto.order.OrderQueryDTO;
import com.shopflow.dto.order.OrderStatusUpdateDTO;
import com.shopflow.vo.order.OrderCreateVO;
import com.shopflow.vo.order.OrderDetailVO;
import com.shopflow.vo.order.OrderPageVO;

import java.util.List;

/**
 * 订单服务。
 *
 * @author shopflow
 */
public interface OrderService {

    /** 创建订单（幂等 + 预扣库存） */
    OrderCreateVO create(OrderCreateDTO dto);

    /** 订单详情（含明细快照与状态时间线） */
    OrderDetailVO detail(String orderNo);

    /** 订单分页（前台只能看自己的，管理员可看全部） */
    PageResult<OrderPageVO> page(OrderQueryDTO query);

    /** 支付订单 */
    void pay(String orderNo);

    /** 取消订单 */
    void cancel(String orderNo, OrderCancelDTO dto);

    /** 后台修改订单状态（发货、完成、取消） */
    void updateStatus(String orderNo, OrderStatusUpdateDTO dto);

    /**
     * 取出已到期的待支付订单号（只读，不修改状态）。
     *
     * <p>拆成「查询」与「关单」两个方法，是为了让定时任务通过 Spring 代理调用
     * {@link #closeTimeoutOrder(String)}，保证事务注解真正生效。
     */
    List<String> findTimeoutOrderNos(int batchSize);

    /** 关闭单笔超时订单（独立事务） */
    boolean closeTimeoutOrder(String orderNo);
}