package com.shopflow.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 客户端 IP 解析工具。
 *
 * <p>部署在 Nginx 之后，{@code request.getRemoteAddr()} 拿到的是代理地址，
 * 因此优先读取 X-Forwarded-For / X-Real-IP。
 *
 * @author shopflow
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value)) {
                // X-Forwarded-For 可能是「客户端IP, 代理1, 代理2」，取第一段
                int commaIndex = value.indexOf(',');
                String ip = commaIndex > 0 ? value.substring(0, commaIndex) : value;
                return ip.trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "" : remoteAddr;
    }
}