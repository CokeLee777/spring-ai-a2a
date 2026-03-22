package io.github.cokelee777.agent.host.invocation;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

/**
 * Request payload for {@code POST /invocations}.
 *
 * <p>
 * {@code actorId} and {@code sessionId} are optional — omit them on the first message and
 * the service generates them, returning the values in {@link InvocationResponse}.
 * </p>
 *
 * @param prompt the user message; must not be blank
 * @param actorId the actor identifier; {@code null} on first message
 * @param sessionId the session identifier; {@code null} on first message
 */
public record InvocationRequest(@NotBlank(message = "prompt must not be blank") String prompt, @Nullable String actorId,
		@Nullable String sessionId) {

}
