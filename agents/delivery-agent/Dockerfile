# delivery-agent container
#
# Build (from project root):
#   docker buildx build --platform linux/amd64 \
#     -f agents/delivery-agent/Dockerfile \
#     -t delivery-agent:latest --load .
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
COPY agent-common ./agent-common
COPY spring-ai-a2a-server ./spring-ai-a2a-server
COPY spring-ai-a2a-server-autoconfigure ./spring-ai-a2a-server-autoconfigure
COPY agents/delivery-agent ./agents/delivery-agent

RUN chmod +x gradlew \
	&& ./gradlew :agents:delivery-agent:bootJar --no-daemon -x test \
	&& cp agents/delivery-agent/build/libs/delivery-agent-*-SNAPSHOT.jar /app/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=builder --chown=app:app /app/app.jar /app/app.jar

EXPOSE 9000

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
