package io.github.cokelee777.agent.host.invocation;

import io.github.cokelee777.agent.host.remote.RemoteAgentConnections;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the orchestrator {@link ChatClient} used on the invocation
 * path.
 *
 * <p>
 * Registers downstream A2A agents as default tools and attaches advisors. Autowired
 * {@link ChatClient.Builder} comes from Spring AI auto-configuration.
 * </p>
 */
@Configuration
public class InvocationConfiguration {

	/**
	 * Builds the routing {@link ChatClient} with downstream A2A agents registered as
	 * tools.
	 * @param builder the Spring AI autoconfigured builder
	 * @param connections the downstream agent tool component
	 * @return the configured {@link ChatClient}
	 */
	@Bean
	public ChatClient chatClient(ChatClient.Builder builder, RemoteAgentConnections connections) {
		return builder.clone().defaultTools(connections).defaultAdvisors(new SimpleLoggerAdvisor()).build();
	}

}
