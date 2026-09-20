package com.shopflow.config;

import com.shopflow.interceptor.TraceIdFilter;
import com.shopflow.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置。
 *
 * <p>显式注册链路追踪过滤器（而不是依赖 @Component 自动注册），
 * 目的是明确执行顺序：traceId 必须最先写入 MDC，后续所有日志才能带上它。
 *
 * @author shopflow
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * 关闭 JwtAuthenticationFilter 的自动注册。
     *
     * <p>它需要挂在 Spring Security 的过滤器链上（见 SecurityConfig），
     * 如果同时又作为普通 Servlet 过滤器被自动注册，就会执行两次：
     * 一次在安全链之后（此时已经晚了，鉴权早已失败），白白浪费一次令牌解析。
     *
     * @param jwtAuthenticationFilter JWT 认证过滤器
     * @return 已禁用的注册器
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}