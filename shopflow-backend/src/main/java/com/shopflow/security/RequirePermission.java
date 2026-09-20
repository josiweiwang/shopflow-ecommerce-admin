package com.shopflow.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解。
 *
 * <p>用法：
 * <pre>{@code
 * @RequirePermission("product:create")
 * @PostMapping
 * public R<Long> create(@RequestBody @Valid ProductSaveDTO dto) { ... }
 *
 * @RequirePermission(value = {"product:update", "product:delete"}, logical = Logical.OR)
 * }</pre>
 *
 * <p>权限码来自数据库 sys_permission 表，通过「用户-角色-权限」关系查询并缓存到 Redis。
 * 新增角色不需要改代码，只改数据即可生效。
 *
 * @author shopflow
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 需要的权限码，如 product:create */
    String[] value();

    /** 多个权限码之间的逻辑关系，默认全部满足 */
    Logical logical() default Logical.AND;

    /**
     * 权限组合逻辑。
     */
    enum Logical {
        /** 必须拥有全部权限 */
        AND,
        /** 拥有任意一个即可 */
        OR
    }
}