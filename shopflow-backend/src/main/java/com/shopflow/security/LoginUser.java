package com.shopflow.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 当前登录用户上下文对象。
 *
 * <p>由 JWT 解析后构建，放入 Spring Security 的 SecurityContext，供业务代码与
 * 自动填充字段（create_by / update_by）使用。
 *
 * @author shopflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 登录用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 账号状态：0-禁用 1-启用 */
    private Integer status;

    /** 角色编码集合，如 ADMIN、OPERATOR */
    private Set<String> roleCodes;

    /** 权限码集合，如 product:create */
    private Set<String> permissions;

    /** 是否拥有指定权限 */
    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }

    /** 是否拥有指定角色 */
    public boolean hasRole(String roleCode) {
        return roleCodes != null && roleCodes.contains(roleCode);
    }

    /** 是否超级管理员（拥有全部权限） */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public Set<String> getPermissions() {
        return permissions == null ? Collections.emptySet() : permissions;
    }

    public Set<String> getRoleCodes() {
        return roleCodes == null ? Collections.emptySet() : roleCodes;
    }
}