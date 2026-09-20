package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper。
 *
 * @author shopflow
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 按订单号查询明细（订单详情页使用）。
     */
    @Select("""
            SELECT id, order_id, order_no, product_id, sku, product_name, product_image,
                   price, quantity, subtotal, create_time
            FROM order_item
            WHERE order_no = #{orderNo}
            ORDER BY id ASC
            """)
    List<OrderItem> selectByOrderNo(@Param("orderNo") String orderNo);
}