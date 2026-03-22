package io.github.cokelee777.agent.host.config;

import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link org.springframework.context.annotation.Condition} that matches when the
 * configured memory mode is not {@link MemoryMode#NONE}.
 *
 * <p>
 * Reads {@code aws.bedrock.agent-core.memory.mode} from the environment and delegates the
 * check to {@link MemoryMode#isDisabled()}, making {@link MemoryMode} the single source
 * of truth for whether memory is active.
 * </p>
 */
public class MemoryEnabledCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String raw = context.getEnvironment().getProperty("aws.bedrock.agent-core.memory.mode", "none");
		MemoryMode mode = MemoryMode.valueOf(raw.toUpperCase());
		if (mode.isDisabled()) {
			return ConditionOutcome.noMatch("memory mode is 'none' — Bedrock Memory beans will not be registered");
		}
		return ConditionOutcome.match("memory mode is '" + raw + "'");
	}

}
