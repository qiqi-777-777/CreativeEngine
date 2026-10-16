# ---------- 构建阶段：用 Maven + JDK 21 编译打包 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 走阿里云镜像，避免在国内从中央仓库拉依赖超时。
# 注意 settings.xml 不能放进 /root/.m2 —— 那个目录被下面的缓存挂载覆盖，放进去会被遮住
COPY docker/maven-settings.xml /build/maven-settings.xml

COPY pom.xml .
COPY src ./src

# 用 BuildKit 缓存挂载持久化 ~/.m2：
# 依赖仓库留在缓存里，改代码后重建不用重新下载；网络中断导致某个包只下了一半时，
# 下一次构建会从断点继续补齐，而不是整层重做。
# 之前用「先 COPY pom 跑 dependency:go-offline」来分层缓存，问题有两个：
# 一是它会把测试依赖、报表插件也全拉一遍（构建时间最长的一段），
# 二是其中任何一个包下载失败都会直接判定构建失败，而这一步本来就是预热、不是正确性检查。
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -s /build/maven-settings.xml clean package -DskipTests

# ---------- 运行阶段：只保留 JRE 与可执行 jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"

COPY --from=build /build/target/CreativeEngine-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
