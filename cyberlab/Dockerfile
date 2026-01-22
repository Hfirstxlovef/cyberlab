# ============================================
# CyberLab 后端服务 Dockerfile
# 多阶段构建 - Maven编译 + JRE运行
# ============================================

# -------------------- 构建阶段 --------------------
# 使用Amazon Corretto（支持arm64和amd64）
FROM amazoncorretto:17-alpine AS builder

LABEL maintainer="CyberLab Team"
LABEL stage=builder

WORKDIR /build

# 复制Maven配置和依赖文件（利用Docker缓存）
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 复制源代码
COPY src ./src

# 编译打包（跳过测试以加快构建）
# 注：直接在编译时下载依赖，避免dependency:go-offline与旧仓库的兼容问题
RUN ./mvnw clean package -DskipTests -B

# 验证构建产物
RUN ls -lh /build/target/ && \
    test -f /build/target/cyberlab-0.0.1-SNAPSHOT.jar

# -------------------- 运行阶段 --------------------
# 使用Amazon Corretto JRE（支持arm64和amd64）
FROM amazoncorretto:17-alpine

LABEL maintainer="CyberLab Team"
LABEL description="CyberLab Backend API Service"
LABEL version="1.0"

# 安装必要的运行时工具
RUN apk add --no-cache \
    curl \
    tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非root用户运行应用（安全最佳实践）
RUN addgroup -g 1000 cyberlab && \
    adduser -D -u 1000 -G cyberlab cyberlab

WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=builder /build/target/cyberlab-0.0.1-SNAPSHOT.jar app.jar

# 创建必要的目录
RUN mkdir -p /app/uploads /app/logs && \
    chown -R cyberlab:cyberlab /app

# 切换到非root用户
USER cyberlab

# 暴露HTTPS端口
EXPOSE 8443

# JVM优化参数
ENV JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=2 -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f -k https://localhost:8443/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
