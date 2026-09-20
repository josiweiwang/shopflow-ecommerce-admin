package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper —— 防超卖的核心 SQL 都在这里。
 *
 * <p>三个写操作都使用「条件更新」，即把业务规则写进 WHERE 子句：
 * {@code AND available_stock >= #{quantity}}。
 * 数据库在行锁下判断条件，即使并发请求同时到达，也只有一个能更新成功，
 * 更新影响行数为 0 就代表库存不足，业务层据此抛出库存不足异常。
 *
 * <p>这是防超卖的最后一道防线：即使 Redis 缓存被清空、预热错误或服务重启，
 * 数据库也不会出现负库存。
 *
 * @author shopflow
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 下单锁定库存：可用库存转锁定库存。
     *
     * @return 影响行数，0 表示可用库存不足
     */
    @Update("""
            UPDATE inventory
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity},
                update_time = NOW()
            WHERE product_id = #{productId} AND available_stock >= #{quantity}
            """)
    int lockStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 释放锁定库存（取消订单、超时关单）：锁定库存退回可用库存。
     *
     * @return 影响行数，0 表示锁定库存不足（数据异常）
     */
    @Update("""
            UPDATE inventory
            SET available_stock = available_stock + #{quantity},
                locked_stock = locked_stock - #{quantity},
                update_time = NOW()
            WHERE product_id = #{productId} AND locked_stock >= #{quantity}
            """)
    int releaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 支付成功确认扣减：锁定库存与总库存同时减少。
     *
     * @return 影响行数，0 表示锁定库存不足（数据异常）
     */
    @Update("""
            UPDATE inventory
            SET total_stock = total_stock - #{quantity},
                locked_stock = locked_stock - #{quantity},
                update_time = NOW()
            WHERE product_id = #{productId} AND locked_stock >= #{quantity}
            """)
    int confirmDeduct(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 盘点调整：delta 为正表示增加可用库存，为负表示减少。
     *
     * @return 影响行数，0 表示调整后可用库存会为负
     */
    @Update("""
            UPDATE inventory
            SET total_stock = total_stock + #{delta},
                available_stock = available_stock + #{delta},
                version = version + 1,
                update_time = NOW()
            WHERE product_id = #{productId} AND available_stock + #{delta} >= 0
            """)
    int adjustStock(@Param("productId") Long productId, @Param("delta") int delta);
}