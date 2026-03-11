package io.github.cokelee777.agentcore.orchestrator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates user requests by invoking the configured {@link ChatClient} with
 * session-scoped conversation memory.
 *
 * <p>
 * The {@link ChatClient} is pre-configured with a system prompt, A2A tool functions, and
 * a {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}. This
 * service simply binds the correct session ID to each call so the LLM maintains
 * conversation context across turns.
 * </p>
 */
@Service
public class ChatOrchestrator {

	private final ChatClient chatClient;

	/**
	 * Create a new {@link ChatOrchestrator}.
	 * @param chatClient the pre-configured chat client
	 */
	public ChatOrchestrator(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Sends {@code userMessage} to the LLM and returns the generated response.
	 * @param userMessage the raw text from the caller
	 * @param sessionId unique identifier used to key conversation memory; typically the
	 * AgentCore Runtime session ID or a generated UUID
	 * @return the LLM response text, or a fallback message if the call fails
	 */
	public String handle(String userMessage, String sessionId) {
		try {
			String content = chatClient.prompt()
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
				.user(userMessage)
				.call()
				.content();
			return content != null ? content : "응답을 생성하지 못했습니다.";
		}
		catch (Exception e) {
			return "처리 중 오류가 발생했습니다: " + e.getMessage();
		}
	}

}
