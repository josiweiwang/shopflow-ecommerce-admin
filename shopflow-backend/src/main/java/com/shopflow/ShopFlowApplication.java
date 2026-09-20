package com.shopflow;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * ShopFlow 电商后台管理系统启动类。
 *
 * @author shopflow
 */
@Slf4j
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.shopflow.mapper")
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ShopFlowApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ShopFlowApplication.class, args).getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String profiles = String.join(",", env.getActiveProfiles());
        log.info("""

                ----------------------------------------------------------------------
                  ShopFlow 启动成功
                  本地地址   : http://localhost:{}{}
                  接口文档   : http://localhost:{}{}/swagger-ui/index.html
                  健康检查   : http://localhost:{}{}/actuator/health
                  运行环境   : {}
                ----------------------------------------------------------------------
                """, port, contextPath, port, contextPath, port, contextPath, profiles);
    }
}
