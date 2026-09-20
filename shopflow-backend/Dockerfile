# =====================================================================
# 后端镜像：多阶段构建
#   构建阶段用 Maven + JDK 编译并跑测试，运行阶段只保留 JRE 与一个 jar，
#   最终镜像比"把 JDK 和源码一起塞进去"小得多，攻击面也更小。
# =====================================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先只拷贝 pom 并拉取依赖：只要 pom 没变，后续改代码不会重新下载依赖（利用 Docker 层缓存）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再拷贝源码构建。这里执行完整测试（Testcontainers 用例在无 Docker 的构建环境会自动跳过）
COPY src ./src
RUN mvn -B clean package

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre

LABEL org.opencontainers.image.title="shopflow-api" \
      org.opencontainers.image.description="ShopFlow 电商后台管理系统 - 后端服务" \
      org.opencontainers.image.version="1.0.0"

# curl 用于容器健康检查；tzdata 保证容器内时区正确
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai" \
    LOG_PATH=/app/logs

WORKDIR /app

# 以非 root 用户运行：即使应用被攻破，攻击者拿到的也只是普通用户权限
RUN useradd -r -u 1001 -m -s /usr/sbin/nologin shopflow \
    && mkdir -p /app/logs \
    && chown -R shopflow:shopflow /app

COPY --from=builder --chown=shopflow:shopflow /build/target/shopflow-backend.jar /app/app.jar

USER shopflow

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]