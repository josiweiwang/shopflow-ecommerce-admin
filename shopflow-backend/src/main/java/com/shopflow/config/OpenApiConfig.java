package com.shopflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（OpenAPI 3 / Swagger UI）。
 *
 * <p>声明全局 JWT 认证方式后，可以直接在 Swagger UI 上填 Token 调接口，
 * 便于自测与演示；生产环境通过配置关闭。
 *
 * @author shopflow
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI shopFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopFlow 电商后台管理系统 API")
                        .description("""
                                电商后台管理系统后端接口文档。
                                统一响应结构：{code, message, data, traceId, timestamp}。
                                除白名单接口外，其余接口均需在请求头携带 Authorization: Bearer <accessToken>。
                                """)
                        .version("v1.0.0")
                        .contact(new Contact().name("ShopFlow").email("dev@shopflow.local"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 accessToken")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}