package io.github.cokelee777.a2a.orchestrator;

import io.a2a.A2A;
import io.github.cokelee777.a2a.common.A2aTransport;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
@Component
public class RemoteAgentConnections {

	private final Map<String, A2aTransport> transports;

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Initialises one {@link A2aTransport} per downstream agent configured in
	 * {@link RemoteAgentProperties}.
	 * @param properties the remote agent connection properties
	 */
	public RemoteAgentConnections(RemoteAgentProperties properties) {
		this.transports = properties.agents()
			.entrySet()
			.stream()
			.collect(
					Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> new A2aTransport(entry.getValue().url())));
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
		A2aTransport transport = this.transports.get(agentName);
		if (transport == null) {
			return "알 수 없는 에이전트: " + agentName;
		}
		return transport.send(A2A.toUserMessage(task), this.timeoutSeconds).orElse("에이전트 호출 중 오류가 발생했습니다.");
	}

	/**
	 * Returns a JSON-formatted description of all available agents for the system prompt.
	 */
	public String getAgentDescriptions() {
		return this.transports.values()
			.stream()
			.map(a2aTransport -> String.format("{\"name\": \"%s\", \"description\": \"%s\"}",
					a2aTransport.getAgentCard().name(),
					a2aTransport.getAgentCard().description() != null ? a2aTransport.getAgentCard().description()
							: "No description"))
			.collect(Collectors.joining("\n"));
	}

	/**
	 * Returns the list of available agent names.
	 */
	public List<String> getAgentNames() {
		return this.transports.values()
			.stream()
			.map(a2aTransport -> a2aTransport.getAgentCard().name())
			.collect(Collectors.toList());
	}

}
