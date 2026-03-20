package io.github.cokelee777.agent.host.invocation;

/**
 * Orchestrates a single {@code POST /invocations} request.
 *
 * <p>
 * Resolves or generates {@code actorId} and {@code sessionId}, loads memory context,
 * invokes the LLM, and persists the turn.
 * </p>
 */
public interface InvocationService {

	/**
	 * Processes one invocation. Generates {@code actorId}/{@code sessionId} if absent.
	 * @param request the invocation request
	 * @return the response including LLM output and resolved identifiers
	 */
	InvocationResponse invoke(InvocationRequest request);

}
