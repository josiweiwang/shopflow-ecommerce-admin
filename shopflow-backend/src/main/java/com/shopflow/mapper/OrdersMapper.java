package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopflow.dto.order.OrderQueryDTO;
import com.shopflow.entity.Orders;
import com.shopflow.vo.order.OrderPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单 Mapper。
 *
 * <p>状态流转不使用 UPDATE ... SET status 裸更新，而是由业务层构造
 * {@code WHERE id = ? AND status = 原状态} 的条件更新，
 * 保证并发下不会出现「两个请求同时把订单从待支付改成不同状态」。
 *
 * @author shopflow
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {

    /**
     * 多条件分页查询订单（连表带出下单用户名）。
     */
    IPage<OrderPageVO> selectPageByCondition(IPage<OrderPageVO> page, @Param("query") OrderQueryDTO query);
}