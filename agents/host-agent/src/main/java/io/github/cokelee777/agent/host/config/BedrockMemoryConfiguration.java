package io.github.cokelee777.agent.host.config;

import io.github.cokelee777.agent.host.memory.LongTermMemoryService;
import io.github.cokelee777.agent.host.memory.MemoryMode;
import io.github.cokelee777.agent.host.memory.NoOpLongTermMemoryService;
import io.github.cokelee777.agent.host.memory.bedrock.AgentCoreEventToMessageConverter;
import io.github.cokelee777.agent.host.memory.bedrock.BedrockShortTermMemoryService;
import io.github.cokelee777.agent.host.memory.bedrock.BedrockLongTermMemoryService;
import io.github.cokelee777.agent.host.memory.bedrock.BedrockMemoryProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;

/**
 * Registers Amazon Bedrock AgentCore Memory beans when
 * {@code aws.bedrock.agent-core.memory.mode} is not {@code none}.
 *
 * <p>
 * When mode is {@code short_term}, a {@link NoOpLongTermMemoryService} is registered.
 * When mode is {@code long_term} or {@code both}, a {@link BedrockLongTermMemoryService}
 * is registered.
 * </p>
 */
@Configuration
@Conditional(MemoryEnabledCondition.class)
@EnableConfigurationProperties(BedrockMemoryProperties.class)
public class BedrockMemoryConfiguration {

	/**
	 * Exposes the configured {@link MemoryMode} as a bean so that service-layer
	 * components do not need to depend on the Bedrock-specific properties class.
	 * @param properties the memory properties
	 * @return the configured {@link MemoryMode}
	 */
	@Bean
	public MemoryMode memoryMode(BedrockMemoryProperties properties) {
		return properties.mode();
	}

	/**
	 * Creates the {@link BedrockAgentCoreClient} using the configured AWS region.
	 * @param region the AWS region value
	 * @return the Bedrock AgentCore data-plane client
	 */
	@Bean
	public BedrockAgentCoreClient bedrockAgentCoreClient(@Value("${spring.ai.bedrock.aws.region}") String region) {
		return BedrockAgentCoreClient.builder().region(Region.of(region)).build();
	}

	/**
	 * Creates the event-to-message converter.
	 * @return the converter
	 */
	@Bean
	public AgentCoreEventToMessageConverter agentCoreEventToMessageConverter() {
		return new AgentCoreEventToMessageConverter();
	}

	/**
	 * Creates the Bedrock short-term memory service.
	 * @param client the Bedrock client
	 * @param properties the memory properties
	 * @param converter the event-to-message converter
	 * @return the service
	 */
	@Bean
	public BedrockShortTermMemoryService shortTermMemoryService(BedrockAgentCoreClient client,
			BedrockMemoryProperties properties, AgentCoreEventToMessageConverter converter) {
		return new BedrockShortTermMemoryService(client, properties, converter);
	}

	/**
	 * Creates a no-op long-term memory service when the mode does not support long-term
	 * memory retrieval.
	 * @return a no-op implementation
	 */
	@Bean
	@Conditional(LongTermNotSupportedCondition.class)
	public LongTermMemoryService noOpLongTermMemoryService() {
		return new NoOpLongTermMemoryService();
	}

	/**
	 * Creates the Bedrock long-term memory service when mode is {@code long_term} or
	 * {@code both}.
	 * @param client the Bedrock client
	 * @param properties the memory properties
	 * @return the service
	 */
	@Bean
	@Conditional(LongTermMemoryCondition.class)
	public BedrockLongTermMemoryService bedrockLongTermMemoryService(BedrockAgentCoreClient client,
			BedrockMemoryProperties properties) {
		return new BedrockLongTermMemoryService(client, properties);
	}

}
