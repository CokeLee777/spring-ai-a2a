package io.github.cokelee777.agent.host.config;

import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link org.springframework.context.annotation.Condition} that matches when the
 * configured memory mode is enabled but does not support long-term memory retrieval.
 *
 * <p>
 * Reads {@code aws.bedrock.agent-core.memory.mode} from the environment and matches when
 * {@link MemoryMode#isDisabled()} is {@code false} and
 * {@link MemoryMode#supportsLongTerm()} is {@code false}. This ensures a
 * {@link io.github.cokelee777.agent.host.memory.NoOpLongTermMemoryService} is registered
 * for any future mode that uses only short-term memory, not just {@code short_term}.
 * </p>
 */
public class LongTermNotSupportedCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		String raw = context.getEnvironment().getProperty("aws.bedrock.agent-core.memory.mode", "none");
		MemoryMode mode = MemoryMode.valueOf(raw.toUpperCase());
		if (!mode.isDisabled() && !mode.supportsLongTerm()) {
			return ConditionOutcome.match("memory mode '" + raw + "' is enabled but does not support long-term memory");
		}
		return ConditionOutcome.noMatch("memory mode '" + raw + "' is either disabled or supports long-term memory");
	}

}
