package io.github.cokelee777.agent.host.memory;

/**
 * Memory usage mode for {@code POST /invocations}.
 *
 * <p>
 * Controls which memory stores are read from and written to on each invocation.
 * </p>
 */
public enum MemoryMode {

	/**
	 * Memory disabled. No AgentCore Memory API calls are made. Suitable for local
	 * development without AWS credentials.
	 */
	NONE,

	/**
	 * Short-term only. Conversation history is loaded and saved; long-term search is
	 * skipped.
	 */
	SHORT_TERM,

	/**
	 * Long-term only. Relevant records are retrieved via semantic search; history is not
	 * loaded. Trade-off: the LLM has no current-session context. Use only when
	 * cross-session knowledge recall is the primary goal.
	 */
	LONG_TERM,

	/**
	 * Both short-term and long-term. Recommended for production use.
	 */
	BOTH

}
