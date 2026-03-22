package io.github.cokelee777.a2a.agent.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for spring-ai-a2a-agent-common shared components.
 *
 * <p>
 * Registers {@link PingController} for any Spring Boot application that includes the
 * {@code spring-ai-a2a-autoconfigure-agent-common} module on its classpath.
 * </p>
 */
@AutoConfiguration
@Import(PingController.class)
public class AgentCommonAutoConfiguration {

}
