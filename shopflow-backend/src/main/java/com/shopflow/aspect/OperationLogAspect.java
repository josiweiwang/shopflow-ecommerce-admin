package com.shopflow.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.common.util.IpUtils;
import com.shopflow.entity.SysOperationLog;
import com.shopflow.security.LoginUser;
import com.shopflow.security.SecurityUtils;
import com.shopflow.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 操作日志切面。
 *
 * <p>把「谁、什么时候、对哪个模块做了什么、结果如何、耗时多久」记录下来。
 * 日志写入是异步的，且失败不影响主流程——审计不能成为业务的单点故障。
 *
 * @author shopflow
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 敏感字段脱敏：把 password / oldPassword / newPassword 的值替换为 *** */
    private static final Pattern SENSITIVE_PATTERN =
            Pattern.compile("(\"[a-zA-Z]*[pP]assword\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE);

    /** 请求参数最大长度，避免大字段把日志表撑爆 */
    private static final int MAX_PARAM_LENGTH = 2000;

    private final OperationLogService operationLogService;

    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                saveLog(joinPoint, operationLog, System.currentTimeMillis() - startTime, error);
            } catch (Exception ex) {
                log.warn("操作日志组装失败 | {} | error={}", operationLog.operation(), ex.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperationLog operationLog,
                         long durationMs, Throwable error) {
        HttpServletRequest request = currentRequest();
        LoginUser loginUser = SecurityUtils.getLoginUser();

        SysOperationLog entity = new SysOperationLog();
        entity.setUserId(loginUser == null ? 0L : loginUser.getUserId());
        entity.setUsername(loginUser == null ? "" : loginUser.getUsername());
        entity.setModule(operationLog.module());
        entity.setOperation(operationLog.operation());
        entity.setDurationMs(durationMs);
        entity.setSuccess(error == null ? 1 : 0);
        entity.setErrorMsg(error == null ? "" : truncate(error.getMessage(), 512));

        if (request != null) {
            entity.setRequestUri(request.getRequestURI());
            entity.setRequestMethod(request.getMethod());
            entity.setIp(IpUtils.getClientIp(request));
        }
        entity.setRequestParam(operationLog.saveParam() ? buildRequestParam(joinPoint) : "");

        operationLogService.saveAsync(entity);
    }

    private String buildRequestParam(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "";
        }
        // 过滤掉无法序列化的参数（如 HttpServletRequest、MultipartFile）
        Object[] serializableArgs = Arrays.stream(args)
                .filter(arg -> arg != null
                        && !(arg instanceof HttpServletRequest)
                        && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
                .toArray();
        if (serializableArgs.length == 0) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(serializableArgs);
            return truncate(maskSensitive(json), MAX_PARAM_LENGTH);
        } catch (Exception ex) {
            return "[参数序列化失败]";
        }
    }

    private String maskSensitive(String json) {
        return SENSITIVE_PATTERN.matcher(json).replaceAll("$1\"***\"");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private HttpServletRequest currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        return (attributes instanceof ServletRequestAttributes servletAttributes)
                ? servletAttributes.getRequest()
                : null;
    }
}