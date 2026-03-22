package io.github.cokelee777.agent.host.config;

import io.github.cokelee777.agent.host.memory.ShortTermMemoryService;
import io.github.cokelee777.agent.host.memory.LongTermMemoryService;
import io.github.cokelee777.agent.host.memory.MemoryMode;
import io.github.cokelee777.agent.host.memory.NoOpShortTermMemoryService;
import io.github.cokelee777.agent.host.memory.NoOpLongTermMemoryService;
import io.github.cokelee777.agent.host.memory.bedrock.BedrockMemoryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Registers no-operation memory beans when memory is disabled (mode is {@code none} or
 * the property is absent).
 *
 * <p>
 * This is the logical counterpart of {@link BedrockMemoryConfiguration}, which activates
 * via {@link MemoryEnabledCondition}. Exactly one of the two configurations is always
 * active, guaranteeing that
 * {@link io.github.cokelee777.agent.host.memory.ShortTermMemoryService},
 * {@link io.github.cokelee777.agent.host.memory.LongTermMemoryService}, and
 * {@link io.github.cokelee777.agent.host.memory.MemoryMode} beans are always present.
 * </p>
 *
 * <p>
 * Allows the application to start without AWS credentials (e.g., local development). Also
 * registers {@link BedrockMemoryProperties} so that {@code DefaultInvocationService} can
 * read the mode even in NONE mode without requiring AWS infrastructure beans.
 * </p>
 */
@Configuration
@Conditional(MemoryDisabledCondition.class)
@EnableConfigurationProperties(BedrockMemoryProperties.class)
public class NoOpMemoryConfiguration {

	/**
	 * Exposes {@link MemoryMode#NONE} as a bean so that service-layer components do not
	 * need to depend on the Bedrock-specific properties class.
	 * @return {@link MemoryMode#NONE}
	 */
	@Bean
	public MemoryMode memoryMode() {
		return MemoryMode.NONE;
	}

	/**
	 * No-op short-term memory service bean.
	 * @return a {@link NoOpShortTermMemoryService} instance
	 */
	@Bean
	public ShortTermMemoryService shortTermMemoryService() {
		return new NoOpShortTermMemoryService();
	}

	/**
	 * No-op long-term memory service bean.
	 * @return a {@link NoOpLongTermMemoryService} instance
	 */
	@Bean
	public LongTermMemoryService longTermMemoryService() {
		return new NoOpLongTermMemoryService();
	}

}
