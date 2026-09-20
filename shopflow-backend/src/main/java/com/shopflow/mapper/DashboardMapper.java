package com.shopflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 后台看板统计 Mapper。
 *
 * <p>统计类查询集中在一个 Mapper 中，而不是分散到各业务 Mapper，
 * 好处是：看板 SQL 是纯读、可独立优化（缓存/预聚合），
 * 与业务写路径解耦，改动不会影响下单等核心链路。
 *
 * <p>所有统计都排除已取消订单（status = 4）与逻辑删除数据，保证口径一致。
 *
 * @author shopflow
 */
@Mapper
public interface DashboardMapper {

    /** 有效订单总数（不含已取消） */
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0 AND status <> 4")
    Long countValidOrders();

    /** 按状态统计订单数 */
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0 AND status = #{status}")
    Long countOrdersByStatus(@Param("status") Integer status);

    /** 指定时间区间内的成交额（只统计已支付及之后的状态） */
    @Select("""
            SELECT COALESCE(SUM(pay_amount), 0)
            FROM orders
            WHERE deleted = 0 AND status IN (1, 2, 3)
              AND create_time >= #{start} AND create_time < #{end}
            """)
    BigDecimal sumPaidAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 指定时间区间内的有效订单数 */
    @Select("""
            SELECT COUNT(*)
            FROM orders
            WHERE deleted = 0 AND status IN (1, 2, 3)
              AND create_time >= #{start} AND create_time < #{end}
            """)
    Long countPaidOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按天统计订单量与成交额趋势。
     *
     * <p>返回 d / orderCount / amount 三个字段；
     * 没有订单的日期不会出现在结果里，由业务层补零，保证前端折线图连续。
     */
    @Select("""
            SELECT DATE(create_time)                       AS d,
                   COUNT(*)                                AS orderCount,
                   COALESCE(SUM(pay_amount), 0)            AS amount
            FROM orders
            WHERE deleted = 0 AND status IN (1, 2, 3)
              AND create_time >= #{start} AND create_time < #{end}
            GROUP BY DATE(create_time)
            ORDER BY d ASC
            """)
    List<Map<String, Object>> selectDailyTrend(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /**
     * 热销商品 Top N（按成交件数排序）。
     */
    @Select("""
            SELECT oi.product_id                AS productId,
                   oi.product_name              AS productName,
                   SUM(oi.quantity)             AS quantity,
                   COALESCE(SUM(oi.subtotal), 0) AS amount
            FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.deleted = 0 AND o.status IN (1, 2, 3)
            GROUP BY oi.product_id, oi.product_name
            ORDER BY quantity DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectTopProducts(@Param("limit") int limit);
}