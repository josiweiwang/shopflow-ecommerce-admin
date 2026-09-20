package com.shopflow.controller;

import com.shopflow.aspect.OperationLog;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.R;
import com.shopflow.dto.product.ProductQueryDTO;
import com.shopflow.dto.product.ProductSaveDTO;
import com.shopflow.dto.product.ProductStatusDTO;
import com.shopflow.dto.product.ProductUpdateDTO;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.ProductService;
import com.shopflow.vo.product.ProductDetailVO;
import com.shopflow.vo.product.ProductPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口。
 *
 * @author shopflow
 */
@Tag(name = "商品管理", description = "商品的增删改查、上下架与库存初始化")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页查询", description = "支持关键词、分类、状态、价格区间与白名单排序")
    @RequirePermission("product:read")
    @GetMapping
    public R<PageResult<ProductPageVO>> page(ProductQueryDTO query) {
        return R.ok(productService.page(query));
    }

    @Operation(summary = "商品详情", description = "走 Redis 缓存，带穿透/击穿/雪崩防护")
    @RequirePermission("product:read")
    @GetMapping("/{id}")
    public R<ProductDetailVO> detail(@PathVariable Long id) {
        return R.ok(productService.detail(id));
    }

    @Operation(summary = "新增商品", description = "同时初始化库存记录")
    @RequirePermission("product:create")
    @OperationLog(module = "商品管理", operation = "新增商品")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ProductSaveDTO dto) {
        return R.ok(productService.create(dto));
    }

    @Operation(summary = "修改商品")
    @RequirePermission("product:update")
    @OperationLog(module = "商品管理", operation = "修改商品")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid ProductUpdateDTO dto) {
        dto.setId(id);
        productService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除商品", description = "逻辑删除，历史订单不受影响")
    @RequirePermission("product:delete")
    @OperationLog(module = "商品管理", operation = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }

    @Operation(summary = "商品上架/下架")
    @RequirePermission("product:update")
    @OperationLog(module = "商品管理", operation = "商品上下架")
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody @Valid ProductStatusDTO dto) {
        productService.updateStatus(id, dto.getStatus());
        return R.ok();
    }
}