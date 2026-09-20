package com.shopflow.service;

import com.shopflow.security.LoginUser;

/**
 * 登录用户与权限加载服务。
 *
 * <p>「用户 -> 角色 -> 权限」是三次 JOIN 查询，而每个请求都要用一次，
 * 因此结果会缓存到 Redis（默认 30 分钟）。
 * 角色或权限变更时主动删除缓存，下次请求自动重建。
 *
 * @author shopflow
 */
public interface UserPermissionService {

    /**
     * 加载登录用户上下文（含角色与权限），用户不存在返回 null。
     */
    LoginUser loadLoginUser(Long userId);

    /**
     * 删除用户权限缓存（角色变更、账号被封禁、修改密码后调用）。
     */
    void evict(Long userId);
}