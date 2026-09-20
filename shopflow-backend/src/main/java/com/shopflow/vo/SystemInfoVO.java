package com.shopflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 系统信息视图对象。
 *
 * <p>用于连通性探测：部署完成后先调这个接口，能同时确认「服务起来了」与「当前是什么环境」。
 *
 * @param application 应用名
 * @param version     应用版本
 * @param profile     当前激活的环境
 * @param javaVersion Java 运行版本
 * @param serverTime  服务端时间
 * @param traceId     本次请求的链路追踪 ID
 * @author shopflow
 */
@Schema(description = "系统信息")
public record SystemInfoVO(
        @Schema(description = "应用名", example = "shopflow-backend") String application,
        @Schema(description = "应用版本", example = "1.0.0") String version,
        @Schema(description = "运行环境", example = "dev") String profile,
        @Schema(description = "Java 版本", example = "17.0.20") String javaVersion,
        @Schema(description = "服务端时间") LocalDateTime serverTime,
        @Schema(description = "链路追踪 ID") String traceId) {
}