# order-agent container
#
# Build (from project root):
#   docker buildx build --platform linux/amd64 \
#     -f agents/order-agent/Dockerfile \
#     -t order-agent:latest --load .
#
# Runtime env:
#   BEDROCK_REGION, BEDROCK_MODEL_ID,
#   AGENT_URL,
#   AGENT_PORT

# ---------- Build stage ----------
FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY spring-ai-a2a-agent-common ./spring-ai-a2a-agent-common
COPY spring-ai-a2a-server ./spring-ai-a2a-server
COPY auto-configurations/server/spring-ai-a2a-autoconfigure-server ./auto-configurations/server/spring-ai-a2a-autoconfigure-server
COPY auto-configurations/agent/common/spring-ai-a2a-autoconfigure-agent-common ./auto-configurations/agent/common/spring-ai-a2a-autoconfigure-agent-common
COPY samples/order-agent ./samples/order-agent

RUN chmod +x gradlew \
	&& ./gradlew :order-agent:bootJar --no-daemon -x test \
	&& cp samples/order-agent/build/libs/order-agent-*-SNAPSHOT.jar /app/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=builder --chown=app:app /app/app.jar /app/app.jar

EXPOSE 9000

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
