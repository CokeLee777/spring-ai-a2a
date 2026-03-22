package io.github.cokelee777.agent.host.config;

import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link org.springframework.context.annotation.Condition} that matches when the
 * configured memory mode supports long-term memory retrieval.
 *
 * <p>
 * Reads {@code aws.bedrock.agent-core.memory.mode} from the environment and delegates the
 * check to {@link MemoryMode#supportsLongTerm()}, making {@link MemoryMode} the single
 * source of truth for which modes activate long-term memory.
 * </p>
 */
public class LongTermMemoryCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String raw = context.getEnvironment().getProperty("aws.bedrock.agent-core.memory.mode", "none");
		MemoryMode mode = MemoryMode.valueOf(raw.toUpperCase());
		if (mode.supportsLongTerm()) {
			return ConditionOutcome.match("memory mode '" + raw + "' supports long-term memory");
		}
		return ConditionOutcome.noMatch("memory mode '" + raw + "' does not support long-term memory");
	}

}
