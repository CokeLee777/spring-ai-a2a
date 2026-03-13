package io.github.cokelee777.a2a.orchestrator;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.github.cokelee777.a2a.common.A2ATransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI tool component that routes requests to downstream A2A agents.
 *
 * <p>
 * Replaces the three separate {@code *AgentTool} classes with a single
 * {@link #sendMessage} tool that dispatches by {@code agentName}.
 * </p>
 */
@Slf4j
@Component
public class RemoteAgentConnections {

	private final Map<String, AgentCard> cards = new HashMap<>();

	/**
	 * Initialises one {@link A2ATransport} per downstream agent configured in
	 * {@link RemoteAgentProperties}.
	 * @param properties the remote agent connection properties
	 */
	public RemoteAgentConnections(RemoteAgentProperties properties) {
		properties.agents()
                .forEach((key, value) -> {
                    String agentUrl = value.url();
                    try {
                        log.info("Resolving agent card from: {}", agentUrl);

                        String path = new URI(agentUrl).getPath();

                        AgentCard card = A2A.getAgentCard(agentUrl, path + ".well-known/agent-card.json", null);

                        this.cards.put(card.name(), card);

                        log.info("Discovered agent: {} at {}", card.name(), agentUrl);
                    } catch (Exception e) {
                        log.error("Failed to connect to agent at {}: {}", agentUrl, e.getMessage());
                    }
                });
	}

	/**
	 * Sends {@code task} to the downstream agent named {@code agentName} and returns its
	 * response.
	 * @param agentName the target agent name
	 * @param task the comprehensive task description and context to send to the agent
	 * @return the agent's text response, or an error message if routing fails
	 */
	@Tool(description = "원격 에이전트에 작업을 위임합니다. 전문 에이전트에게 작업을 분배할 때 사용하세요.")
	public String sendMessage(@ToolParam(description = "작업을 위임할 에이전트의 이름") String agentName,
			@ToolParam(description = "에이전트에게 전달할 포괄적인 작업 설명 및 컨텍스트") String task) {
		AgentCard agentCard = cards.get(agentName);
		if (agentCard == null) {
			String availableAgents = String.join(", ", this.cards.keySet());
			return String.format("Agent '%s' not found. Available agents: %s", agentName, availableAgents);
		}

		Message message = A2A.toUserMessage(task);
		return A2ATransport.send(agentCard, message);
	}

	/**
	 * Returns a JSON-formatted description of all available agents for the system prompt.
	 */
	public String getAgentDescriptions() {
		return this.cards.values()
				.stream()
				.map(card -> String.format("{\"name\": \"%s\", \"description\": \"%s\"}", card.name(),
						card.description() != null ? card.description() : "No description"))
				.collect(Collectors.joining("\n"));
	}

	/**
	 * Returns the list of available agent names.
	 */
	public List<String> getAgentNames() {
		return List.copyOf(this.cards.keySet());
	}

}
