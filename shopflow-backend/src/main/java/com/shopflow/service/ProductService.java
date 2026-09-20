package com.shopflow.service;

import com.shopflow.common.result.PageResult;
import com.shopflow.dto.product.ProductQueryDTO;
import com.shopflow.dto.product.ProductSaveDTO;
import com.shopflow.dto.product.ProductUpdateDTO;
import com.shopflow.vo.product.ProductDetailVO;
import com.shopflow.vo.product.ProductPageVO;

/**
 * 商品服务。
 *
 * @author shopflow
 */
public interface ProductService {

    /** 商品分页查询 */
    PageResult<ProductPageVO> page(ProductQueryDTO query);

    /** 商品详情（Redis 缓存） */
    ProductDetailVO detail(Long id);

    /**
     * 新增商品：同时初始化库存记录，两步在同一事务内完成，
     * 避免出现「有商品没库存」这种一上线就会踩坑的脏数据。
     */
    Long create(ProductSaveDTO dto);

    /** 修改商品 */
    void update(ProductUpdateDTO dto);

    /** 逻辑删除商品 */
    void delete(Long id);

    /** 商品上下架 */
    void updateStatus(Long id, Integer status);
}