package com.shopflow.common.enums;

/**
 * 内置角色编码。
 *
 * <p>角色数据存在数据库中，这里只定义「代码里需要判断」的三个内置角色，
 * 避免在业务代码中出现魔法字符串。
 *
 * @author shopflow
 */
public final class RoleCode {

    private RoleCode() {
    }

    /** 超级管理员：拥有全部权限 */
    public static final String ADMIN = "ADMIN";

    /** 运营人员：商品、库存、订单、看板 */
    public static final String OPERATOR = "OPERATOR";

    /** 普通用户：前台注册用户 */
    public static final String USER = "USER";
}