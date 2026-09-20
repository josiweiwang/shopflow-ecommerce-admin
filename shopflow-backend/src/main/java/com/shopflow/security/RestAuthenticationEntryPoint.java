package com.shopflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.result.R;
import com.shopflow.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证处理器。
 *
 * <p>Spring Security 默认返回 302 跳转或空白 401 页面，对前后端分离不友好。
 * 这里统一返回 JSON，并根据 JwtAuthenticationFilter 写入的原因区分三种情况：
 * 未携带令牌（40101）、令牌过期（40102）、令牌非法或已登出（40103）。
 * 前端可以据此决定「跳登录页」还是「静默刷新令牌后重试」。
 *
 * @author shopflow
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String jwtError = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_JWT_ERROR);
        ResultCode resultCode = resolveResultCode(jwtError);
        log.warn("未认证访问 | uri={} | reason={} | message={}",
                request.getRequestURI(), jwtError == null ? "no-token" : jwtError, authException.getMessage());
        writeJson(response, resultCode);
    }

    private ResultCode resolveResultCode(String jwtError) {
        if (JwtAuthenticationFilter.ERROR_EXPIRED.equals(jwtError)) {
            return ResultCode.TOKEN_EXPIRED;
        }
        if (JwtAuthenticationFilter.ERROR_INVALID.equals(jwtError)
                || JwtAuthenticationFilter.ERROR_REVOKED.equals(jwtError)) {
            return ResultCode.TOKEN_INVALID;
        }
        return ResultCode.UNAUTHORIZED;
    }

    private void writeJson(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(resultCode)));
    }
}