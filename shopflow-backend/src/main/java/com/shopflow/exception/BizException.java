package com.shopflow.exception;

import com.shopflow.common.result.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。
 *
 * <p>用于表达「业务流程不满足」而不是「程序出错」，例如库存不足、订单状态不允许流转。
 * 由 {@link GlobalExceptionHandler} 统一翻译成响应体，业务代码中不需要写 try-catch。
 *
 * <p>使用示例：
 * <pre>{@code
 * throw new BizException(ResultCode.STOCK_INSUFFICIENT);
 * throw BizException.of(ResultCode.STOCK_INSUFFICIENT, "商品「机械键盘」库存不足");
 * }</pre>
 *
 * @author shopflow
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务状态码枚举 */
    private final ResultCode resultCode;

    /** 业务状态码数值，便于日志检索与前端对齐 */
    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
    }

    public static BizException of(ResultCode resultCode) {
        return new BizException(resultCode);
    }

    public static BizException of(ResultCode resultCode, String message) {
        return new BizException(resultCode, message);
    }

    /** 判断是否由指定状态码引起，便于上游做分支处理 */
    public boolean is(ResultCode expected) {
        return this.resultCode == expected;
    }
}