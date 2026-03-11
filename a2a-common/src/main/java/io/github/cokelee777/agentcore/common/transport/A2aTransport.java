package io.github.cokelee777.agentcore.common.transport;

import io.a2a.client.Client;
import io.a2a.client.TaskEvent;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.github.cokelee777.agentcore.common.util.TextExtractor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reusable A2A client transport that sends a {@link Message} to a downstream agent and
 * collects the full text response.
 *
 * <p>
 * The agent card is resolved lazily on the first call and cached for subsequent
 * invocations. Thread-safe initialization is done via double-checked locking with
 * {@link AtomicReference}.
 * </p>
 */
public class A2aTransport {

	private final String agentUrl;

	private final AtomicReference<AgentCard> agentCardRef = new AtomicReference<>();

	/**
	 * Creates a transport targeting the given agent base URL.
	 * @param agentUrl base URL of the downstream A2A agent (e.g.,
	 * {@code "http://order-agent:8080/"})
	 */
	public A2aTransport(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	/**
	 * Sends {@code message} to the downstream agent and waits up to
	 * {@code timeoutSeconds} for the response.
	 *
	 * <p>
	 * Returns {@link Optional#empty()} when the agent returns no text, the timeout
	 * elapses, or any exception occurs.
	 * </p>
	 * @param message the A2A {@link Message} to send
	 * @param timeoutSeconds maximum seconds to wait for a response
	 * @return the concatenated text from all response parts, or empty if the call fails
	 */
	public Optional<String> send(Message message, int timeoutSeconds) {
		try {
			CompletableFuture<Optional<String>> future = CompletableFuture.supplyAsync(() -> {
				CompletableFuture<String> resultFuture = new CompletableFuture<>();
				try (Client client = Client.builder(resolveAgentCard())
					.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
					.addConsumer((event, card) -> {
						if (event instanceof TaskEvent taskEvent) {
							Task task = taskEvent.getTask();
							if (TaskState.TASK_STATE_FAILED.equals(task.status().state())) {
								resultFuture.complete(null);
								return;
							}
							resultFuture.complete(TextExtractor.extractFromTask(task));
						}
					})
					.streamingErrorHandler(resultFuture::completeExceptionally)
					.build()) {
					client.sendMessage(message);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return Optional.ofNullable(resultFuture.getNow(null));
			});
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Resolves and caches the {@link AgentCard} for this transport's target agent.
	 *
	 * <p>
	 * Uses double-checked locking with {@link AtomicReference} for thread-safe lazy
	 * initialization.
	 * </p>
	 * @return the resolved {@link AgentCard}
	 */
	private AgentCard resolveAgentCard() {
		AgentCard card = agentCardRef.get();
		if (card == null) {
			synchronized (this) {
				card = agentCardRef.get();
				if (card == null) {
					card = new A2ACardResolver(agentUrl).getAgentCard();
					agentCardRef.set(card);
				}
			}
		}
		return card;
	}

}
