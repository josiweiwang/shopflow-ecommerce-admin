package com.shopflow.controller;

import com.shopflow.aspect.OperationLog;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.R;
import com.shopflow.dto.inventory.InventoryAdjustDTO;
import com.shopflow.dto.inventory.InventoryInboundDTO;
import com.shopflow.dto.inventory.InventoryLogQueryDTO;
import com.shopflow.dto.inventory.InventoryQueryDTO;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.InventoryService;
import com.shopflow.vo.inventory.InventoryLogVO;
import com.shopflow.vo.inventory.InventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存接口。
 *
 * @author shopflow
 */
@Tag(name = "库存管理", description = "库存查询、入库、盘点调整与库存流水")
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "库存分页列表")
    @RequirePermission("inventory:read")
    @GetMapping
    public R<PageResult<InventoryVO>> page(InventoryQueryDTO query) {
        return R.ok(inventoryService.page(query));
    }

    @Operation(summary = "查询商品库存", description = "优先返回 Redis 缓存值，fromCache 标记来源")
    @RequirePermission("inventory:read")
    @GetMapping("/{productId}")
    public R<InventoryVO> getByProductId(@PathVariable Long productId) {
        return R.ok(inventoryService.getByProductId(productId));
    }

    @Operation(summary = "商品入库")
    @RequirePermission("inventory:update")
    @OperationLog(module = "库存管理", operation = "商品入库")
    @PostMapping("/{productId}/inbound")
    public R<Void> inbound(@PathVariable Long productId, @RequestBody @Valid InventoryInboundDTO dto) {
        inventoryService.inbound(productId, dto);
        return R.ok();
    }

    @Operation(summary = "库存盘点调整")
    @RequirePermission("inventory:update")
    @OperationLog(module = "库存管理", operation = "库存盘点调整")
    @PostMapping("/{productId}/adjust")
    public R<Void> adjust(@PathVariable Long productId, @RequestBody @Valid InventoryAdjustDTO dto) {
        inventoryService.adjust(productId, dto);
        return R.ok();
    }

    @Operation(summary = "库存流水分页")
    @RequirePermission("inventory:read")
    @GetMapping("/{productId}/logs")
    public R<PageResult<InventoryLogVO>> logs(@PathVariable Long productId, InventoryLogQueryDTO query) {
        query.setProductId(productId);
        return R.ok(inventoryService.pageLogs(query));
    }
}