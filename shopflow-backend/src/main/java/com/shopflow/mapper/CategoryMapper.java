package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shopflow.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品分类 Mapper。
 *
 * @author shopflow
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /** 逻辑删除分类 */
    @Update("UPDATE category SET deleted = id, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int softDeleteById(@Param("id") Long id);
}