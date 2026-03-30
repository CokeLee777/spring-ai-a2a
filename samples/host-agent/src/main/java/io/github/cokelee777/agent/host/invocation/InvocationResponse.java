package io.github.cokelee777.agent.host.invocation;

import org.springframework.util.Assert;

/**
 * Response payload for {@code POST /invocations}.
 *
 * <p>
 * {@code actorId} and {@code conversationId} are always non-null. Clients must persist
 * them and send them back on subsequent requests.
 * </p>
 *
 * @param content the assistant response text
 * @param actorId the effective Bedrock AgentCore actor id for this invocation
 * @param conversationId the conversation identifier (chat memory / session key)
 */
public record InvocationResponse(String content, String actorId, String conversationId) {

	public InvocationResponse {
		Assert.notNull(content, "content must not be null");
		Assert.notNull(actorId, "actorId must not be null");
		Assert.notNull(conversationId, "conversationId must not be null");
	}

}
