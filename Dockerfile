#
# Build
#
FROM maven:3.8.4-jdk-11-slim@sha256:04f8e5ba4a6a74fb7f97940bc75ac7340520728d2fb051ecc5c9ecbb9ba28b48 as buildtime
WORKDIR /build
COPY . .
RUN --mount=type=secret,id=GH_TOKEN,dst=/tmp/secret_token export GITHUB_TOKEN_READ_PACKAGES="$(cat /tmp/secret_token)" \
  && mvn clean package -Dmaven.test.skip=true

FROM adoptopenjdk/openjdk11:alpine-jre@sha256:1a6f7f544918619ee443cc7141edd29bf1424da90a20daf493cbb5b921f4941b as builder
COPY --from=buildtime /build/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


FROM ghcr.io/pagopa/docker-base-springboot-openjdk11:v1.0.1@sha256:bbbe948e91efa0a3e66d8f308047ec255f64898e7f9250bdb63985efd3a95dbf
ADD --chown=spring:spring https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.25.1/opentelemetry-javaagent.jar .

COPY --chown=spring:spring  --from=builder dependencies/ ./
COPY --chown=spring:spring  --from=builder snapshot-dependencies/ ./
# https://github.com/moby/moby/issues/37965#issuecomment-426853382
RUN true
COPY --chown=spring:spring  --from=builder spring-boot-loader/ ./
COPY --chown=spring:spring  --from=builder application/ ./

EXPOSE 8080

ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar","--enable-preview","org.springframework.boot.loader.JarLauncher"]
