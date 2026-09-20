package com.shopflow.interceptor;

import com.shopflow.common.constant.CommonConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器。
 *
 * <p>为每个请求生成唯一的 traceId 并写入 MDC，日志模板通过 %X{traceId} 输出。
 * 这样一次请求产生的所有日志（Controller、Service、SQL、异常堆栈）都能串起来，
 * 排查问题时只需要用户提供响应里的 traceId。
 *
 * <p>如果上游（网关）已经透传 traceId，则复用，保证全链路一致。
 *
 * @author shopflow
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        MDC.put(CommonConstants.TRACE_ID, traceId);
        response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程复用场景下必须清理，否则会污染后续请求的日志
            MDC.remove(CommonConstants.TRACE_ID);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}