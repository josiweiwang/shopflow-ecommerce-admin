package com.shopflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopflow.dto.product.ProductQueryDTO;
import com.shopflow.entity.Product;
import com.shopflow.vo.product.ProductPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 商品 Mapper。
 *
 * <p>列表分页查询写在 XML 中（{@code resources/mapper/ProductMapper.xml}），
 * 因为需要 JOIN 分类与库存表，并且 ORDER BY 必须在 SQL 层做字段白名单控制，避免注入。
 *
 * @author shopflow
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 逻辑删除商品 */
    @Update("UPDATE product SET deleted = id, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int softDeleteById(@Param("id") Long id);

    /** 统计分类下的有效商品数，用于删除分类前校验 */
    @Select("SELECT COUNT(*) FROM product WHERE category_id = #{categoryId} AND deleted = 0")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    /** 统计有效商品数（后台看板） */
    @Select("SELECT COUNT(*) FROM product WHERE deleted = 0")
    Long countValidProducts();

    /** 支付成功后累加销量 */
    @Update("UPDATE product SET sales = sales + #{quantity}, update_time = NOW() WHERE id = #{productId} AND deleted = 0")
    int increaseSales(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 多条件分页查询商品（连表返回分类名与库存）。
     */
    IPage<ProductPageVO> selectPageByCondition(IPage<ProductPageVO> page, @Param("query") ProductQueryDTO query);

    /**
     * 按商品 ID 批量查询名称，用于订单明细拼装前的校验。
     */
    @Select("""
            <script>
            SELECT id, name, sku, price, status, main_image, category_id
            FROM product
            WHERE deleted = 0 AND id IN
            <foreach collection="productIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Map<String, Object>> selectBriefByIds(@Param("productIds") List<Long> productIds);
}