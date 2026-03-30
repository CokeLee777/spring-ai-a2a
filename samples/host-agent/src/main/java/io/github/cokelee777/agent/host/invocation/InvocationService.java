package io.github.cokelee777.agent.host.invocation;

/**
 * Orchestrates a single user turn against the host
 * {@link org.springframework.ai.chat.client.ChatClient}.
 *
 * <p>
 * Resolves {@code actorId} and {@code conversationId} (generating UUIDs when absent via
 * {@link InvocationRequest}), loads memory context, runs the model, and persists the
 * updated transcript.
 * </p>
 */
public interface InvocationService {

	/**
	 * Processes one invocation.
	 * @param request the prompt and optional ids
	 * @return assistant text and the effective ids
	 */
	InvocationResponse invoke(InvocationRequest request);

}
