package io.github.cokelee777.agent.host.invocation;

/**
 * Response payload for {@code POST /invocations}.
 *
 * <p>
 * {@code sessionId} and {@code actorId} are always non-null. Clients must persist these
 * values and include them in every subsequent request to continue the conversation.
 * </p>
 *
 * @param content the assistant response text
 * @param sessionId the session identifier used for this invocation
 * @param actorId the actor identifier used for this invocation
 */
public record InvocationResponse(String content, String sessionId, String actorId) {

}
