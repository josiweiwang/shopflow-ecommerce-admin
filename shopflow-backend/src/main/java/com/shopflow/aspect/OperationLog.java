package com.shopflow.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 后台操作日志注解。
 *
 * <p>标在需要审计的写接口上，例如：
 * <pre>{@code
 * @OperationLog(module = "商品管理", operation = "新增商品")
 * }</pre>
 *
 * @author shopflow
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 业务模块名 */
    String module();

    /** 操作描述 */
    String operation();

    /** 是否记录请求参数（默认记录，敏感字段会自动脱敏） */
    boolean saveParam() default true;
}