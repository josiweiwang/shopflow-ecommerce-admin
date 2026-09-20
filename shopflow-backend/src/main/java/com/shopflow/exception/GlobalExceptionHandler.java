package com.shopflow.exception;

import com.shopflow.common.result.R;
import com.shopflow.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>设计要点：
 * <ol>
 *     <li>所有异常统一转换为 {@link R} 结构，前端只需要处理一种响应格式；</li>
 *     <li>业务异常记 WARN（属于预期内的业务结果），系统异常记 ERROR 并打印完整堆栈；</li>
 *     <li>绝不把异常堆栈、SQL 语句等内部信息返回给调用方，只返回可读提示 + traceId。</li>
 * </ol>
 *
 * @author shopflow
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBizException(BizException ex, HttpServletRequest request) {
        log.warn("业务异常 | uri={} | code={} | message={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        return build(ex.getResultCode(), ex.getMessage());
    }

    // ==================== 参数校验 ====================

    /** @RequestBody 上的校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 | {}", message);
        return build(ResultCode.PARAM_INVALID, message);
    }

    /** 表单或查询参数绑定失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 | {}", message);
        return build(ResultCode.PARAM_INVALID, message);
    }

    /** 方法参数上的 @Validated 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 | {}", message);
        return build(ResultCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "缺少必填参数: " + ex.getParameterName();
        log.warn("参数缺失 | {}", message);
        return build(ResultCode.PARAM_MISSING, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "参数[" + ex.getName() + "]类型不正确";
        log.warn("参数类型不匹配 | {}", message);
        return build(ResultCode.PARAM_TYPE_MISMATCH, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败 | {}", ex.getMessage());
        return build(ResultCode.PARAM_INVALID, "请求体格式错误或不是合法的 JSON");
    }

    // ==================== 路由与请求方式 ====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("请求方式不支持 | method={} | message={}", ex.getMethod(), ex.getMessage());
        return build(ResultCode.REQUEST_METHOD_NOT_SUPPORTED,
                "该接口不支持 " + ex.getMethod() + " 方式请求");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<R<Void>> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("请求地址不存在 | uri={}", request.getRequestURI());
        return build(ResultCode.NOT_FOUND, "请求地址不存在: " + request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("静态资源不存在 | uri={}", request.getRequestURI());
        return build(ResultCode.NOT_FOUND, "请求地址不存在: " + request.getRequestURI());
    }

    // ==================== 认证与鉴权 ====================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("认证失败 | {}", ex.getMessage());
        return build(ResultCode.UNAUTHORIZED, null);
    }

    /**
     * 方法级鉴权失败（@PreAuthorize 或自定义权限注解）。
     * 过滤器链上的鉴权失败由 RestAccessDeniedHandler 处理，不会走到这里。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("权限不足 | {}", ex.getMessage());
        return build(ResultCode.FORBIDDEN, null);
    }

    // ==================== 数据层异常 ====================

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("唯一键冲突 | {}", ex.getMessage());
        return build(ResultCode.DUPLICATE_KEY, null);
    }

    // ==================== 兜底 ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("系统异常 | uri={} | method={}", request.getRequestURI(), request.getMethod(), ex);
        return build(ResultCode.SYSTEM_ERROR, null);
    }

    // ==================== 内部方法 ====================

    private static ResponseEntity<R<Void>> build(ResultCode resultCode, String message) {
        R<Void> body = (message == null) ? R.fail(resultCode) : R.fail(resultCode, message);
        return ResponseEntity.status(resultCode.getHttpStatus()).body(body);
    }

    private static String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}