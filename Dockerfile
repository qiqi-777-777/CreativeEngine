# ---------- 构建阶段：用 Maven + JDK 21 编译打包 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 走阿里云镜像，避免在国内从中央仓库拉依赖超时
COPY docker/maven-settings.xml /root/.m2/settings.xml

# 先只拷贝 pom 并预下载依赖，源码变更时可复用这一层缓存
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 运行阶段：只保留 JRE 与可执行 jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"

COPY --from=build /build/target/CreativeEngine-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
