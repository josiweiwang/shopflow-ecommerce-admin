package com.shopflow.controller;

import com.shopflow.common.constant.CommonConstants;
import com.shopflow.common.result.R;
import com.shopflow.vo.SystemInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 系统信息接口（无需认证）。
 *
 * @author shopflow
 */
@Tag(name = "系统信息", description = "服务探测与版本信息")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final Environment environment;

    @Operation(summary = "连通性探测")
    @GetMapping("/ping")
    public R<SystemInfoVO> ping() {
        return R.ok(buildInfo());
    }

    @Operation(summary = "服务版本信息")
    @GetMapping("/version")
    public R<SystemInfoVO> version() {
        return R.ok(buildInfo());
    }

    private SystemInfoVO buildInfo() {
        return new SystemInfoVO(
                environment.getProperty("spring.application.name", "shopflow-backend"),
                environment.getProperty("shopflow.version", "1.0.0"),
                String.join(",", environment.getActiveProfiles()),
                System.getProperty("java.version"),
                LocalDateTime.now(),
                MDC.get(CommonConstants.TRACE_ID));
    }
}