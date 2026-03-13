package io.github.cokelee777.a2a.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AgentCore Runtime invocations.
 *
 * <p>
 * Amazon Bedrock AgentCore Runtime forwards user messages to {@code POST /invocations}.
 * This controller delegates each request to the {@link ChatClient}, which routes to
 * downstream A2A agents via {@link RemoteAgentConnections}.
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InvocationsController {

	private final ChatClient chatClient;

	/**
	 * Handles invocation requests from Amazon Bedrock AgentCore Runtime.
	 *
	 * <p>
	 * This endpoint receives a prompt from the runtime, forwards it to the configured LLM
	 * through {@code ChatClient}, and returns the generated response text back to the
	 * runtime.
	 * </p>
	 * @param request the invocation request containing the user prompt
	 * @return the generated response text from the LLM
	 */
	@PostMapping("/invocations")
	public String invoke(@RequestBody InvocationRequest request) {
		log.info("Received: {}", request.prompt());
		String response = this.chatClient.prompt().user(request.prompt()).call().content();
		log.info("Response: {}", response);
		return response;
	}

	/**
	 * Request payload used by AgentCore Runtime when invoking the agent.
	 *
	 * @param prompt the user prompt that should be processed by the agent
	 */
	public record InvocationRequest(String prompt) {
	}

}
