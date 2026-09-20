package com.shopflow.vo.log;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台操作日志项。
 *
 * @author shopflow
 */
@Data
public class OperationLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String requestUri;

    private String requestMethod;

    private String ip;

    private Long durationMs;

    @Schema(description = "是否成功：0-失败 1-成功")
    private Integer success;

    private String errorMsg;

    private LocalDateTime createTime;
}