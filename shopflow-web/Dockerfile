# =====================================================================
# 前端镜像：Node 构建静态资源 -> Nginx 托管
# 最终镜像不包含 node_modules 与源码，只保留构建产物与 Nginx 配置。
# =====================================================================

# ---------- 阶段一：构建 ----------
FROM node:20-alpine AS builder
WORKDIR /build

# 先装依赖（package.json 不变则复用缓存层）
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# 构建前会执行 vue-tsc 类型检查，类型不过就不出镜像
RUN npm run build

# ---------- 阶段二：运行 ----------
FROM nginx:1.27-alpine

LABEL org.opencontainers.image.title="shopflow-web" \
      org.opencontainers.image.description="ShopFlow 电商后台管理系统 - 前端" \
      org.opencontainers.image.version="1.0.0"

ENV TZ=Asia/Shanghai

COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]