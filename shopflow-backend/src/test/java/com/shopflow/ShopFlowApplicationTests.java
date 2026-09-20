package com.shopflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 应用冒烟测试。
 *
 * <p>覆盖「启动即失败」这类高成本问题：
 * <ol>
 *     <li>Spring 上下文（含 MyBatis-Plus、Spring Security、Redis 等全部配置）能否正常加载；</li>
 *     <li>白名单接口是否可匿名访问；</li>
 *     <li>受保护接口在未携带 Token 时是否返回统一 JSON 的 401；</li>
 *     <li>请求方式错误时是否返回统一 JSON 的 405；</li>
 *     <li>链路追踪 ID 是否写入响应头与响应体。</li>
 * </ol>
 *
 * @author shopflow
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopFlowApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Spring 上下文加载成功")
    void contextLoads() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("系统探测接口可匿名访问，并返回统一响应结构与链路 ID")
    void pingShouldBeAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/system/ping"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.application").value("shopflow-backend"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    @DisplayName("未携带 Token 访问受保护接口返回 401，且响应体结构与业务接口一致")
    void protectedApiShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("请求方式错误返回 405 与统一错误结构")
    void wrongHttpMethodShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/system/ping"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(40003));
    }

    @Test
    @DisplayName("链路 ID 可由上游透传，保证跨服务日志可串联")
    void traceIdShouldBePropagatedFromUpstream() throws Exception {
        String upstreamTraceId = "trace-id-from-gateway-0001";

        mockMvc.perform(get("/api/v1/system/ping").header("X-Trace-Id", upstreamTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", upstreamTraceId))
                .andExpect(jsonPath("$.traceId").value(upstreamTraceId));
    }
}