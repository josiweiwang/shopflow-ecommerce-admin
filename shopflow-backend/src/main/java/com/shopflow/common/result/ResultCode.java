package com.shopflow.common.result;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一业务状态码。
 *
 * <p>编码规范：
 * <ul>
 *     <li>200      成功</li>
 *     <li>400xx    参数校验失败</li>
 *     <li>401xx    未认证</li>
 *     <li>403xx    无权限 / 账号不可用</li>
 *     <li>404xx    资源不存在</li>
 *     <li>409xx    状态冲突（唯一键重复、状态不允许等）</li>
 *     <li>600xx    业务规则不满足（库存不足等）</li>
 *     <li>500xx    系统异常</li>
 * </ul>
 *
 * <p>每个状态码同时声明对应 HTTP 状态码，让 HTTP 语义与业务语义都可读。
 *
 * @author shopflow
 */
@Getter
public enum ResultCode {

    // ==================== 成功 ====================
    SUCCESS(200, "success", HttpStatus.OK),

    // ==================== 参数校验 400xx ====================
    PARAM_MISSING(40001, "请求参数缺失", HttpStatus.BAD_REQUEST),
    PARAM_INVALID(40002, "请求参数不合法", HttpStatus.BAD_REQUEST),
    REQUEST_METHOD_NOT_SUPPORTED(40003, "请求方式不支持", HttpStatus.METHOD_NOT_ALLOWED),
    PARAM_TYPE_MISMATCH(40004, "请求参数类型不匹配", HttpStatus.BAD_REQUEST),

    // ==================== 认证 401xx ====================
    UNAUTHORIZED(40101, "未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(40102, "登录凭证已过期，请重新登录", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(40103, "登录凭证无效", HttpStatus.UNAUTHORIZED),

    // ==================== 权限 403xx ====================
    FORBIDDEN(40301, "没有该操作权限", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(40302, "账号已被禁用，请联系管理员", HttpStatus.FORBIDDEN),

    // ==================== 资源不存在 404xx ====================
    NOT_FOUND(40401, "请求的资源不存在", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(40402, "用户不存在", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(40403, "商品分类不存在", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(40404, "商品不存在", HttpStatus.NOT_FOUND),
    INVENTORY_NOT_FOUND(40405, "库存记录不存在", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(40406, "订单不存在", HttpStatus.NOT_FOUND),

    // ==================== 状态冲突 409xx ====================
    USERNAME_EXISTS(40901, "用户名已被占用", HttpStatus.CONFLICT),
    EMAIL_EXISTS(40902, "邮箱已被占用", HttpStatus.CONFLICT),
    PHONE_EXISTS(40903, "手机号已被占用", HttpStatus.CONFLICT),
    SKU_EXISTS(40904, "商品编码已存在", HttpStatus.CONFLICT),
    CATEGORY_NAME_EXISTS(40905, "同级分类名称已存在", HttpStatus.CONFLICT),
    CATEGORY_HAS_PRODUCT(40906, "该分类下存在商品，无法删除", HttpStatus.CONFLICT),
    ORDER_STATUS_ILLEGAL(40907, "当前订单状态不允许该操作", HttpStatus.CONFLICT),
    PASSWORD_NOT_MATCH(40908, "原密码不正确", HttpStatus.CONFLICT),
    DATA_VERSION_CONFLICT(40909, "数据已被他人修改，请刷新后重试", HttpStatus.CONFLICT),
    DUPLICATE_KEY(40910, "数据已存在，请勿重复提交", HttpStatus.CONFLICT),

    // ==================== 业务规则 600xx ====================
    STOCK_INSUFFICIENT(60001, "商品库存不足", HttpStatus.CONFLICT),
    PRODUCT_OFF_SHELF(60002, "商品已下架", HttpStatus.CONFLICT),
    ORDER_PAY_TIMEOUT(60003, "订单支付已超时", HttpStatus.CONFLICT),
    ORDER_ITEM_EMPTY(60004, "订单商品不能为空", HttpStatus.BAD_REQUEST),
    DUPLICATE_REQUEST(60005, "请求重复提交，请勿重复操作", HttpStatus.CONFLICT),
    REFRESH_TOKEN_INVALID(60006, "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED),

    // ==================== 系统异常 500xx ====================
    SYSTEM_ERROR(50000, "系统繁忙，请稍后再试", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(50001, "数据操作异常", HttpStatus.INTERNAL_SERVER_ERROR);

    /** 业务状态码 */
    private final int code;

    /** 默认提示信息 */
    private final String message;

    /** 对应的 HTTP 状态码 */
    private final HttpStatus httpStatus;

    ResultCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
