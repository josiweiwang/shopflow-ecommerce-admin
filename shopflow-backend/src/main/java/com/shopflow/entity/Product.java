package com.shopflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表（product）。
 *
 * @author shopflow
 */
@Data
@TableName("product")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    /** 商品编码，全局唯一 */
    private String sku;

    private String subtitle;

    private String mainImage;

    /** 商品详情（大字段，列表查询不返回） */
    private String detail;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    /** 累计销量（冗余字段，支付成功后累加） */
    private Integer sales;

    private Integer sort;

    private Long deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}