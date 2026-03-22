package io.github.cokelee777.agent.host.config;

import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link org.springframework.context.annotation.Condition} that matches when the
 * configured memory mode is {@link MemoryMode#NONE}, or when the property is absent
 * (defaults to {@code none}).
 *
 * <p>
 * This is the logical inverse of {@link MemoryEnabledCondition}: exactly one of the two
 * conditions matches for any given configuration, ensuring that either the Bedrock or the
 * no-op memory beans are always registered.
 * </p>
 */
public class MemoryDisabledCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String raw = context.getEnvironment().getProperty("aws.bedrock.agent-core.memory.mode", "none");
		MemoryMode mode = MemoryMode.valueOf(raw.toUpperCase());
		if (mode.isDisabled()) {
			return ConditionOutcome.match("memory mode is 'none' — no-op memory beans will be registered");
		}
		return ConditionOutcome.noMatch("memory mode is '" + raw + "' — Bedrock memory beans will be registered");
	}

}
