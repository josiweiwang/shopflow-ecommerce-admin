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
 * <p>Spring Security 默认返回 302 跳转或空 401 页面，对前后端分离不友好。
 * 这里统一返回 JSON 响应体，保证「认证失败」与「业务失败」的响应结构完全一致。
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
        log.warn("未认证访问 | uri={} | message={}", request.getRequestURI(), authException.getMessage());
        writeJson(response, ResultCode.UNAUTHORIZED);
    }

    private void writeJson(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(resultCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(resultCode)));
    }
}