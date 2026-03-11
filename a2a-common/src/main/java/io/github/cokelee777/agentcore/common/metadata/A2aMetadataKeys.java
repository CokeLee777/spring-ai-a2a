package io.github.cokelee777.agentcore.common.metadata;

/**
 * Constant keys used in the {@code metadata} map of A2A {@code Message} objects.
 *
 * <p>
 * Placing these constants in a shared module prevents magic strings from being scattered
 * across agent implementations.
 * </p>
 */
public final class A2aMetadataKeys {

	/** Metadata key carrying the skill ID that the caller wants to invoke. */
	public static final String SKILL_ID = "skillId";

	private A2aMetadataKeys() {
	}

}
