package io.github.cokelee777.agentcore.autoconfigure;

import io.a2a.spec.AgentCard;
import io.github.cokelee777.agentcore.autoconfigure.controller.A2AAgentCardController;
import io.github.cokelee777.agentcore.autoconfigure.controller.PingController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration activated when an {@link AgentCard} bean is present.
 *
 * <p>
 * Registers the {@link PingController} and {@link A2AAgentCardController} beans shared by
 * all A2A server and orchestrator modules.
 * </p>
 */
@AutoConfiguration
@ConditionalOnBean(AgentCard.class)
public class A2ACommonAutoConfiguration {

	/**
	 * Create a new {@link A2ACommonAutoConfiguration}.
	 */
	public A2ACommonAutoConfiguration() {
	}

	/**
	 * Provides the health-check controller when none is already registered.
	 * @return a new {@link PingController}
	 */
	@Bean
	@ConditionalOnMissingBean
	public PingController pingController() {
		return new PingController();
	}

	/**
	 * Provides the agent-card discovery controller backed by the given card.
	 * @param agentCard the agent card bean supplied by the application
	 * @return a new {@link A2AAgentCardController}
	 */
	@Bean
	@ConditionalOnMissingBean
	public A2AAgentCardController a2AAgentCardController(AgentCard agentCard) {
		return new A2AAgentCardController(agentCard);
	}

}
