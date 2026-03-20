package io.github.cokelee777.agent.host.memory;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Short-term conversation memory service.
 *
 * <p>
 * Loads and persists per-session conversation history so that the LLM can reference
 * previous turns within the same session.
 * </p>
 */
public interface ConversationMemoryService {

	/**
	 * Returns the conversation history for the given actor and session. Returns an empty
	 * list when there is no history (first message).
	 * @param actorId the actor identifier
	 * @param sessionId the session identifier
	 * @return ordered list of {@link Message} objects (oldest first)
	 */
	List<Message> loadHistory(String actorId, String sessionId);

	/**
	 * Appends a user turn to the short-term memory.
	 * @param actorId the actor identifier
	 * @param sessionId the session identifier
	 * @param userText the user message text
	 */
	void appendUserTurn(String actorId, String sessionId, String userText);

	/**
	 * Appends an assistant turn to the short-term memory.
	 * @param actorId the actor identifier
	 * @param sessionId the session identifier
	 * @param assistantText the assistant response text
	 */
	void appendAssistantTurn(String actorId, String sessionId, String assistantText);

}
