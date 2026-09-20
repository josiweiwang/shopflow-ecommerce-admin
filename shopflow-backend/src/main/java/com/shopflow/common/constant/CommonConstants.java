package com.shopflow.common.constant;

/**
 * 通用常量。
 *
 * @author shopflow
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** MDC 中链路追踪 ID 的键 */
    public static final String TRACE_ID = "traceId";

    /** 响应头中返回给前端的链路追踪 ID */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 认证请求头 */
    public static final String AUTH_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 系统操作人 ID（定时任务、系统自动流转使用） */
    public static final Long SYSTEM_USER_ID = 0L;

    /** 默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /** 默认每页条数 */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /** 单页最大条数，防止一次拉取过多数据 */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 启用 */
    public static final int STATUS_ENABLED = 1;

    /** 禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 未删除标记 */
    public static final long NOT_DELETED = 0L;
}
