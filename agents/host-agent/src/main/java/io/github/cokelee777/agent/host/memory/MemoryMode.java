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
	BOTH;

	/**
	 * Returns {@code true} if conversation history should be loaded and persisted.
	 * @return {@code true} for {@link #SHORT_TERM} and {@link #BOTH}
	 */
	public boolean supportsShortTerm() {
		return this == SHORT_TERM || this == BOTH;
	}

	/**
	 * Returns {@code true} if long-term memory retrieval should be performed.
	 * @return {@code true} for {@link #LONG_TERM} and {@link #BOTH}
	 */
	public boolean supportsLongTerm() {
		return this == LONG_TERM || this == BOTH;
	}

	/**
	 * Returns {@code true} if memory is disabled and no session context is maintained.
	 * @return {@code true} for {@link #NONE}
	 */
	public boolean isDisabled() {
		return this == NONE;
	}

}
