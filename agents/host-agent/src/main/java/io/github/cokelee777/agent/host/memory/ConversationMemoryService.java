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
	 * Returns the conversation history for the given session. Returns an empty list when
	 * there is no history (first message).
	 * @param session the conversation session identifier
	 * @return ordered list of {@link Message} objects (oldest first)
	 */
	List<Message> loadHistory(ConversationSession session);

	/**
	 * Appends a user turn to the short-term memory.
	 * @param session the conversation session identifier
	 * @param userText the user message text
	 */
	void appendUserTurn(ConversationSession session, String userText);

	/**
	 * Appends an assistant turn to the short-term memory.
	 * @param session the conversation session identifier
	 * @param assistantText the assistant response text
	 */
	void appendAssistantTurn(ConversationSession session, String assistantText);

}
