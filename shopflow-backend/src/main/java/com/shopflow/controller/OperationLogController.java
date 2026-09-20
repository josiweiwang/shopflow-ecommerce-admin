package com.shopflow.controller;

import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.R;
import com.shopflow.dto.log.OperationLogQueryDTO;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.OperationLogService;
import com.shopflow.vo.log.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口。
 *
 * @author shopflow
 */
@Tag(name = "操作日志", description = "后台管理员操作审计")
@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "操作日志分页查询")
    @RequirePermission("log:read")
    @GetMapping
    public R<PageResult<OperationLogVO>> page(OperationLogQueryDTO query) {
        return R.ok(operationLogService.page(query));
    }
}