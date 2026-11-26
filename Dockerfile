# ---------- 构建阶段：用 Maven Wrapper 在容器内编译，无需本机安装 Maven ----------
# 不要加 "# syntax=docker/dockerfile:1"：它会先去 Docker Hub 拉取 BuildKit 前端镜像，国内网络容易超时
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先只拷 pom 与 wrapper，依赖层可以被 Docker 缓存，源码变更时不用重新下载
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
# 国内网络走阿里云镜像加速（不需要可直接删掉这一行）
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---------- 运行阶段：只带 JRE 与 jar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/smart-campus-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
