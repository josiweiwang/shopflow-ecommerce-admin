package com.shopflow.convert;

import com.shopflow.entity.Product;
import com.shopflow.vo.product.ProductDetailVO;
import com.shopflow.vo.product.ProductPageVO;
import org.mapstruct.Mapper;

/**
 * 商品对象转换器。
 *
 * @author shopflow
 */
@Mapper(componentModel = "spring")
public interface ProductConverter {

    ProductPageVO toPageVO(Product product);

    ProductDetailVO toDetailVO(Product product);
}