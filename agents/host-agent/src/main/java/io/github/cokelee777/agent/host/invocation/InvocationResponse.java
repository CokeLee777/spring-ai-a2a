package io.github.cokelee777.agent.host.invocation;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Response payload for {@code POST /invocations}.
 *
 * <p>
 * {@code sessionId} and {@code actorId} are {@code null} when memory mode is
 * {@code NONE}, since no session context is maintained. Otherwise both are non-null and
 * clients must persist these values to continue the conversation.
 * </p>
 *
 * @param content the assistant response text
 * @param sessionId the session identifier used for this invocation, or {@code null} if
 * memory is disabled
 * @param actorId the actor identifier used for this invocation, or {@code null} if memory
 * is disabled
 */
public record InvocationResponse(String content, @Nullable String sessionId, @Nullable String actorId) {

	public InvocationResponse {
		Assert.notNull(content, "content must not be null");
	}
}
