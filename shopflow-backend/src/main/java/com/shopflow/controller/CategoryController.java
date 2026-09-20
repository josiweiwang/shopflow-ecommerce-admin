package com.shopflow.controller;

import com.shopflow.aspect.OperationLog;
import com.shopflow.common.result.R;
import com.shopflow.dto.category.CategorySaveDTO;
import com.shopflow.dto.category.CategoryUpdateDTO;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.CategoryService;
import com.shopflow.vo.category.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类接口。
 *
 * @author shopflow
 */
@Tag(name = "商品分类", description = "两级分类树的维护")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "查询分类树")
    @RequirePermission("category:read")
    @GetMapping
    public R<List<CategoryVO>> tree() {
        return R.ok(categoryService.tree());
    }

    @Operation(summary = "查询分类详情")
    @RequirePermission("category:read")
    @GetMapping("/{id}")
    public R<CategoryVO> detail(@PathVariable Long id) {
        return R.ok(categoryService.detail(id));
    }

    @Operation(summary = "新增分类")
    @RequirePermission("category:create")
    @OperationLog(module = "商品分类", operation = "新增分类")
    @PostMapping
    public R<Long> create(@RequestBody @Valid CategorySaveDTO dto) {
        return R.ok(categoryService.create(dto));
    }

    @Operation(summary = "修改分类")
    @RequirePermission("category:update")
    @OperationLog(module = "商品分类", operation = "修改分类")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid CategoryUpdateDTO dto) {
        dto.setId(id);
        categoryService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除分类", description = "分类下存在子分类或商品时拒绝删除")
    @RequirePermission("category:delete")
    @OperationLog(module = "商品分类", operation = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}