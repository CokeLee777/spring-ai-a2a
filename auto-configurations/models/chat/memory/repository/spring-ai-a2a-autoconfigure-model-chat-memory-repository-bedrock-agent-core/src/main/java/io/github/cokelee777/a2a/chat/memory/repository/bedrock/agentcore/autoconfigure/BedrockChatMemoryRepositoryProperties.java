package io.github.cokelee777.a2a.chat.memory.repository.bedrock.agentcore.autoconfigure;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Amazon Bedrock AgentCore Chat Memory Repository.
 *
 * <p>
 * Bound from the {@code spring.ai.chat.memory.repository.bedrock.agent-core} prefix.
 * Override with environment variables (e.g., {@code BEDROCK_MEMORY_ID}).
 * </p>
 *
 * @param memoryId the Memory resource ID or ARN; must not be null (autoconfigure is
 * active only when this property is set)
 * @param maxTurns max conversation turns to load from short-term memory; defaults to 10
 */
@Validated
@ConfigurationProperties(prefix = BedrockChatMemoryRepositoryProperties.CONFIG_PREFIX)
public record BedrockChatMemoryRepositoryProperties(@NotNull String memoryId, @DefaultValue("10") int maxTurns) {

	/** Configuration properties prefix. */
	public static final String CONFIG_PREFIX = "spring.ai.chat.memory.repository.bedrock.agent-core";

}