package com.shopflow.convert;

import com.shopflow.entity.Category;
import com.shopflow.vo.category.CategoryVO;
import org.mapstruct.Mapper;

/**
 * 分类对象转换器。
 *
 * @author shopflow
 */
@Mapper(componentModel = "spring")
public interface CategoryConverter {

    CategoryVO toVO(Category category);
}