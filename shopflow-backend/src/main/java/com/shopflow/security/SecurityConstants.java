package com.shopflow.security;

/**
 * 安全相关常量。
 *
 * <p>白名单只放「必须匿名访问」的接口：注册、登录、刷新令牌、健康检查与接口文档。
 * 其余接口一律需要认证，遵循「默认拒绝」原则，避免新增接口忘记加权限时被意外暴露。
 *
 * @author shopflow
 */
public final class SecurityConstants {

    private SecurityConstants() {
    }

    /** 无需认证即可访问的地址 */
    public static final String[] PUBLIC_URLS = {
            // 认证
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            // 系统探测
            "/api/v1/system/ping",
            "/api/v1/system/version",
            // 健康检查与监控
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            // 接口文档
            "/doc.html",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            // 静态资源与容器错误页
            "/favicon.ico",
            "/error"
    };
}