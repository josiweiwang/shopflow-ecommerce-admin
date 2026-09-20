package com.shopflow.controller;

import com.shopflow.aspect.OperationLog;
import com.shopflow.common.result.PageResult;
import com.shopflow.common.result.R;
import com.shopflow.dto.user.AssignRolesDTO;
import com.shopflow.dto.user.UserQueryDTO;
import com.shopflow.dto.user.UserStatusDTO;
import com.shopflow.security.RequirePermission;
import com.shopflow.service.UserService;
import com.shopflow.vo.user.RoleVO;
import com.shopflow.vo.user.UserPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台用户管理接口。
 *
 * @author shopflow
 */
@Tag(name = "用户管理", description = "用户列表、启用禁用与角色分配")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页查询")
    @RequirePermission("user:read")
    @GetMapping
    public R<PageResult<UserPageVO>> page(UserQueryDTO query) {
        return R.ok(userService.page(query));
    }

    @Operation(summary = "启用/禁用用户", description = "禁用后该账号的令牌立即失效")
    @RequirePermission("user:update")
    @OperationLog(module = "用户管理", operation = "启用或禁用用户")
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody @Valid UserStatusDTO dto) {
        userService.updateStatus(id, dto);
        return R.ok();
    }

    @Operation(summary = "分配角色", description = "变更后权限缓存立即失效，新权限下一个请求生效")
    @RequirePermission("user:assign-role")
    @OperationLog(module = "用户管理", operation = "分配角色")
    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody @Valid AssignRolesDTO dto) {
        userService.assignRoles(id, dto);
        return R.ok();
    }

    @Operation(summary = "可分配角色列表")
    @RequirePermission("user:read")
    @GetMapping("/roles")
    public R<List<RoleVO>> listRoles() {
        return R.ok(userService.listRoles());
    }
}