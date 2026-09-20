package com.shopflow.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口需要登录（不要求具体权限）。
 *
 * <p>SecurityConfig 已经做到「默认拒绝」，这个注解主要用于给接口补语义说明，
 * 让阅读代码的人一眼看出「这个接口是登录即可访问」。
 *
 * @author shopflow
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired {
}