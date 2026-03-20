package io.github.cokelee777.agent.host.memory.bedrock;

import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for Amazon Bedrock AgentCore Memory.
 *
 * <p>
 * Bound from the {@code aws.bedrock.agentcore.memory} YAML prefix. Override with
 * environment variables (e.g., {@code BEDROCK_MEMORY_ID}).
 * </p>
 *
 * @param memoryId the Memory resource ID or ARN
 * @param mode the memory usage mode; defaults to {@link MemoryMode#BOTH}
 * @param shortTermMaxTurns max conversation turns to load from short-term; defaults to 10
 * @param strategyId the Memory strategy ID for long-term retrieval
 * @param longTermMaxResults max records to return from long-term search; defaults to 4
 */
@ConfigurationProperties(prefix = "aws.bedrock.agentcore.memory")
public record BedrockMemoryProperties(@DefaultValue("placeholder-memory-id") String memoryId,
		@DefaultValue("BOTH") MemoryMode mode, @DefaultValue("10") int shortTermMaxTurns,
		@DefaultValue("placeholder-strategy-id") String strategyId, @DefaultValue("4") int longTermMaxResults) {

}
