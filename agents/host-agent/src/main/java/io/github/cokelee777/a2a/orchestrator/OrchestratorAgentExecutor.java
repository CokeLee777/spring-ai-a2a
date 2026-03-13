package io.github.cokelee777.a2a.orchestrator;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import io.github.cokelee777.a2a.common.TextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * A2A AgentExecutor implementation for the orchestrator.
 *
 * <p>
 * Delegates to {@link ChatOrchestrator} which invokes an LLM with tool-calling to
 * coordinate downstream A2A agents. The session ID is resolved from the
 * {@code X-Amzn-Bedrock-AgentCore-Runtime-Session-Id} header injected by AgentCore
 * Runtime; falls back to a new UUID when running locally.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestratorAgentExecutor implements AgentExecutor {

	static final String SESSION_HEADER = "X-Amzn-Bedrock-AgentCore-Runtime-Session-Id";

	private final ChatOrchestrator chatOrchestrator;

	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		TaskUpdater updater = createTaskUpdater(context, eventQueue);
		updater.startWork();

		Message message = Objects.requireNonNull(context.getMessage(), "message must not be null");
		String text = TextExtractor.extractFromMessage(message);
		log.debug("Orchestrator execute: text={}", text);

		try {
			ChatResponse response = chatOrchestrator.handle(new ChatRequest(text));
			updater.addArtifact(List.of(new TextPart(response.content())));
			updater.complete();
		}
		catch (Exception e) {
			log.error("Orchestrator execution error: {}", e.getMessage(), e);
			updater.fail();
		}
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		createTaskUpdater(context, eventQueue).cancel();
	}

	/**
	 * Creates a {@link TaskUpdater} for the given context and event queue.
	 *
	 * <p>
	 * Protected to allow spy-based overriding in unit tests.
	 * </p>
	 * @param context the request context
	 * @param eventQueue the event queue for this task
	 * @return a new {@link TaskUpdater}
	 */
	protected TaskUpdater createTaskUpdater(RequestContext context, EventQueue eventQueue) {
		return new TaskUpdater(context, eventQueue);
	}

}
