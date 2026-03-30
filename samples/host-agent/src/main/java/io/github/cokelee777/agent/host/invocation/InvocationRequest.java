package io.github.cokelee777.agent.host.invocation;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Request payload for {@code POST /invocations}.
 *
 * <p>
 * {@code actorId} and {@code conversationId} are optional in JSON — omit either on a
 * first message and a UUID is assigned in the compact constructor; echo
 * {@link InvocationResponse#actorId()} and {@link InvocationResponse#conversationId()} on
 * later requests.
 * </p>
 *
 * @param prompt the user message; must not be blank
 * @param actorId the Bedrock AgentCore actor id; {@code null} or omitted to generate a
 * UUID
 * @param conversationId the chat memory conversation id; {@code null} or omitted to
 * generate a UUID
 */
public record InvocationRequest(@NotBlank(message = "prompt must not be blank") String prompt, @Nullable String actorId,
		@Nullable String conversationId) {

	public InvocationRequest {
		actorId = Objects.requireNonNullElse(actorId, UUID.randomUUID().toString());
		conversationId = Objects.requireNonNullElse(conversationId, UUID.randomUUID().toString());
	}

}
