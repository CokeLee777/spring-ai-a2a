package io.github.cokelee777.agent.host.memory;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object identifying a conversation session by actor and session.
 *
 * <p>
 * {@code actorId} and {@code sessionId} always travel together in conversation memory
 * operations. Grouping them prevents parameter list pollution across
 * {@link ConversationMemoryService} methods.
 * </p>
 *
 * <p>
 * Use {@link Builder} to construct an instance from nullable request values. Missing
 * identifiers are automatically assigned a random UUID.
 * </p>
 *
 * @param actorId the actor identifier
 * @param sessionId the session identifier
 */
public record ConversationSession(String actorId, String sessionId) {

	public ConversationSession {
		Assert.hasText(actorId, "actorId must not be blank");
		Assert.hasText(sessionId, "sessionId must not be blank");
	}

	/**
	 * Returns a new {@link Builder}.
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link ConversationSession}.
	 *
	 * <p>
	 * Any identifier left {@code null} is replaced with a randomly generated UUID at
	 * {@link #build()} time.
	 * </p>
	 */
	public static final class Builder {

		private @Nullable String actorId;

		private @Nullable String sessionId;

		private Builder() {
		}

		/**
		 * Sets the actor identifier.
		 * @param actorId the actor identifier, or {@code null} to auto-generate
		 * @return this builder
		 */
		public Builder actorId(@Nullable String actorId) {
			this.actorId = actorId;
			return this;
		}

		/**
		 * Sets the session identifier.
		 * @param sessionId the session identifier, or {@code null} to auto-generate
		 * @return this builder
		 */
		public Builder sessionId(@Nullable String sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		/**
		 * Builds the {@link ConversationSession}, generating a UUID for any unset
		 * identifier.
		 * @return a fully resolved {@link ConversationSession}
		 */
		public ConversationSession build() {
			this.actorId = Objects.requireNonNullElse(this.actorId, UUID.randomUUID().toString());
			this.sessionId = Objects.requireNonNullElse(this.sessionId, UUID.randomUUID().toString());
			return new ConversationSession(this.actorId, this.sessionId);
		}

	}

}
