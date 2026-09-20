package com.shopflow.service;

import com.shopflow.dto.category.CategorySaveDTO;
import com.shopflow.dto.category.CategoryUpdateDTO;
import com.shopflow.vo.category.CategoryVO;

import java.util.List;

/**
 * 商品分类服务。
 *
 * @author shopflow
 */
public interface CategoryService {

    /** 查询完整分类树（走缓存） */
    List<CategoryVO> tree();

    /** 查询分类详情 */
    CategoryVO detail(Long id);

    /** 新增分类 */
    Long create(CategorySaveDTO dto);

    /** 修改分类 */
    void update(CategoryUpdateDTO dto);

    /** 删除分类（存在子分类或商品时拒绝） */
    void delete(Long id);
}