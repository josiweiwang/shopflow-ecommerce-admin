package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态流转日志 Mapper（只增不改）。
 *
 * @author shopflow
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}