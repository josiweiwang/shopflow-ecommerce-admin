package com.shopflow.convert;

import com.shopflow.entity.OrderItem;
import com.shopflow.entity.OrderStatusLog;
import com.shopflow.entity.Orders;
import com.shopflow.vo.order.OrderDetailVO;
import com.shopflow.vo.order.OrderItemVO;
import com.shopflow.vo.order.OrderPageVO;
import com.shopflow.vo.order.OrderStatusLogVO;
import org.mapstruct.Mapper;

/**
 * 订单对象转换器。
 *
 * @author shopflow
 */
@Mapper(componentModel = "spring")
public interface OrderConverter {

    OrderPageVO toPageVO(Orders orders);

    OrderDetailVO toDetailVO(Orders orders);

    OrderItemVO toItemVO(OrderItem orderItem);

    OrderStatusLogVO toStatusLogVO(OrderStatusLog statusLog);
}