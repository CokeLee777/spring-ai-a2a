package io.github.cokelee777.a2a.agent.common;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.UpdateEvent;
import io.github.cokelee777.a2a.agent.common.util.TextExtractor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class for sending a {@link Message} to a downstream A2A agent and collecting
 * the outcome as a single {@link String} (concatenated artifact text, or a formatted
 * error line when the call or task fails).
 *
 * <p>
 * All methods are static; this class is not meant to be instantiated.
 * </p>
 */
@Slf4j
public class A2ATransport {

	private static final int STREAM_COMPLETION_TIMEOUT_SECONDS = 60;

	/**
	 * Formats a final task status that represents an application-level failure.
	 * @param state terminal {@link TaskState} such as {@link TaskState#FAILED}
	 * @param statusMessage optional status {@link Message} from the task
	 * @return {@code A2A task failed: <details>}
	 */
	private static String formatA2ATaskFailed(TaskState state, Message statusMessage) {
		String fromMessage = TextExtractor.extractTextFromMessage(statusMessage);
		String detail = fromMessage.isEmpty() ? state.asString() : fromMessage;
		return "A2A task failed: %s".formatted(detail);
	}

	private static boolean isApplicationTaskFailure(TaskState state) {
		return TaskState.FAILED.equals(state) || TaskState.REJECTED.equals(state) || TaskState.UNKNOWN.equals(state);
	}

	/**
	 * Sends {@code message} to the downstream agent identified by {@code agentCard} using
	 * the non-streaming JSON-RPC transport. The call blocks until the client delivers a
	 * {@link TaskEvent} or throws (SDK-dependent behaviour; this method does not apply an
	 * additional timeout).
	 *
	 * <p>
	 * On a terminal task status of {@link TaskState#FAILED}, {@link TaskState#REJECTED},
	 * or {@link TaskState#UNKNOWN}, returns {@code A2A task failed: …} built from the
	 * status message when present. On transport or client errors, returns
	 * {@code Error communicating with agent '<name>': …}.
	 * </p>
	 * @param agentCard the target agent's {@link AgentCard}
	 * @param message the A2A {@link Message} to send
	 * @return concatenated text from response artifacts, or a formatted error string as
	 * described above
	 */
	public static String send(AgentCard agentCard, Message message) {
		try {
			AtomicReference<String> result = new AtomicReference<>("");
			BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
				if (event instanceof TaskEvent taskEvent) {
					Task task = taskEvent.getTask();

					TaskState state = task.getStatus().state();
					if (isApplicationTaskFailure(state)) {
						result.set(formatA2ATaskFailed(state, task.getStatus().message()));
						return;
					}
					result.set(TextExtractor.extractTextFromTask(task));
				}
			};

			// Create client with consumer via builder
			ClientConfig clientConfig = new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build();
			Client client = Client.builder(agentCard)
				.clientConfig(clientConfig)
				.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
				.addConsumers(List.of(consumer))
				.build();

			client.sendMessage(message);

			return result.get();
		}
		catch (Exception e) {
			log.error("Error sending message to agent '{}': {}", agentCard.name(), e.getMessage());
			return String.format("Error communicating with agent '%s': %s", agentCard.name(), e.getMessage());
		}
	}

	/**
	 * Sends {@code message} to the downstream agent via SSE streaming and blocks until
	 * the outcome is available or 60 seconds elapse waiting on
	 * {@link CompletableFuture#get(long, TimeUnit)}.
	 *
	 * <p>
	 * The A2A JSON-RPC client applies streaming updates asynchronously: {@code
	 * Client#sendMessage} may return before artifact events are delivered. When a
	 * {@link TaskStatusUpdateEvent} is {@linkplain TaskStatusUpdateEvent#isFinal() final}
	 * or its {@link TaskState} {@linkplain TaskState#isFinal() is final}, the internal
	 * future completes normally with either concatenated artifact text collected so far
	 * or, for {@link TaskState#FAILED}, {@link TaskState#REJECTED}, or
	 * {@link TaskState#UNKNOWN}, {@code A2A task failed: …}. Non-final states such as
	 * {@link TaskState#INPUT_REQUIRED} or {@link TaskState#AUTH_REQUIRED} are not resumed
	 * here; if the peer never reaches a final status, {@code get} may time out after
	 * {@value #STREAM_COMPLETION_TIMEOUT_SECONDS} seconds.
	 * </p>
	 *
	 * <p>
	 * Timeouts, {@code get} failures (including wrapped causes), and streaming errors
	 * that {@linkplain CompletableFuture#completeExceptionally(Throwable) complete the
	 * future exceptionally} are caught and returned as
	 * {@code Error communicating with agent '<name>': …}, consistent with {@link #send}.
	 * </p>
	 *
	 * <p>
	 * The A2A JSON-RPC SSE listener may
	 * {@linkplain java.util.concurrent.Future#cancel(boolean) cancel} the HTTP body
	 * future after a final status to close the stream; the JDK client may then report
	 * errors such as {@code "Request cancelled"}. The streaming error handler ignores
	 * further errors once the result future is already completed (typically by the final
	 * {@link TaskStatusUpdateEvent} in the event consumer); otherwise it completes the
	 * future exceptionally, which surfaces through {@code get} and is mapped by the
	 * {@code catch} block to the error string above.
	 * </p>
	 * @param agentCard the target agent's {@link AgentCard}
	 * @param message the A2A {@link Message} to send
	 * @return concatenated artifact text, {@code A2A task failed: …}, or
	 * {@code Error communicating with agent '<name>': …} as appropriate
	 */
	public static String sendStream(AgentCard agentCard, Message message) {
		try {
			CompletableFuture<String> result = new CompletableFuture<>();
			StringBuilder accumulated = new StringBuilder();

			BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
				if (event instanceof TaskUpdateEvent taskUpdateEvent) {
					UpdateEvent updateEvent = taskUpdateEvent.getUpdateEvent();
					if (updateEvent instanceof TaskStatusUpdateEvent statusUpdateEvent) {
						TaskStatus status = statusUpdateEvent.getStatus();
						TaskState state = status.state();
						if (statusUpdateEvent.isFinal() || state.isFinal()) {
							String outcome = isApplicationTaskFailure(state)
									? formatA2ATaskFailed(state, status.message()) : accumulated.toString();
							result.complete(outcome);
						}
					}
					else if (updateEvent instanceof TaskArtifactUpdateEvent artifactUpdateEvent) {
						accumulated.append(TextExtractor.extractTextFromArtifact(artifactUpdateEvent.getArtifact()));
					}
				}
			};

			Consumer<Throwable> streamingErrorHandler = t -> {
				if (result.isDone()) {
					return;
				}
				log.error("Streaming error from agent '{}': {}", agentCard.name(), t.getMessage());
				result.completeExceptionally(t);
			};

			ClientConfig clientConfig = new ClientConfig.Builder().setAcceptedOutputModes(List.of("text"))
				.setStreaming(true)
				.build();
			Client client = Client.builder(agentCard)
				.clientConfig(clientConfig)
				.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
				.addConsumers(List.of(consumer))
				.streamingErrorHandler(streamingErrorHandler)
				.build();

			client.sendMessage(message);

			return result.get(STREAM_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			log.error("Error sending streaming message to agent '{}': {}", agentCard.name(), e.getMessage());
			return String.format("Error communicating with agent '%s': %s", agentCard.name(), e.getMessage());
		}
	}

}
