/**
 * Spring Boot auto-configuration for the A2A server.
 *
 * <p>
 * {@link io.github.cokelee777.a2a.server.autoconfigure.A2AServerAutoConfiguration}
 * registers server infrastructure when
 * {@link org.springframework.ai.chat.client.ChatClient} is on the classpath and
 * {@code spring.ai.a2a.server.enabled} is true (the default). The application must
 * provide an {@link io.a2a.spec.AgentCard} bean. Ping and remote-agent registry are
 * provided by the {@code spring-ai-a2a-autoconfigure-agent-common} module.
 * </p>
 */
@NullMarked
package io.github.cokelee777.a2a.server.autoconfigure;

import org.jspecify.annotations.NullMarked;
