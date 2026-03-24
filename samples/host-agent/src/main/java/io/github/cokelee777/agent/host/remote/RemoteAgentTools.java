package io.github.cokelee777.agent.host.remote;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.github.cokelee777.a2a.agent.common.A2ATransport;
import io.github.cokelee777.a2a.agent.common.LazyAgentCard;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Spring {@link Component} that registers the host's downstream A2A agents as Spring AI
 * {@link Tool}-annotated methods, routing orchestration calls over JSON-RPC via
 * {@link A2ATransport}.
 *
 * <p>
 * Each configured URL ({@link RemoteAgentProperties}) gets one {@link LazyAgentCard}.
 * Routing uses the card's {@link AgentCard#name() display name} (from
 * {@code /.well-known/agent-card.json}), not the YAML map key.
 * </p>
 *
 * <p>
 * {@link #getAgentDescriptions()} feeds the orchestrator system prompt; it prefers
 * {@link LazyAgentCard#peek()} when any card is cached, and falls back to
 * {@link LazyAgentCard#get()} for all entries when the cache is still cold so the first
 * user turn is not misled into thinking no agents exist.
 * </p>
 *
 * <p>
 * Tool methods are synchronous from the model's perspective: {@link #sendMessage} blocks
 * until the downstream task completes; {@link #sendMessagesParallel} blocks until every
 * delegated call finishes (each runs on a virtual thread).
 * </p>
 *
 * <p>
 * Instances are safe for concurrent use; {@link #lazyCards} is a
 * {@link ConcurrentHashMap} and {@link LazyAgentCard} coordinates resolution per URL.
 * </p>
 */
@Slf4j
@Component
public class RemoteAgentTools {

	/**
	 * Runs one downstream delegation per virtual thread for {@link #sendMessagesParallel}
	 * without tying up platform thread pools.
	 */
	private static final Executor VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

	/**
	 * Config key (e.g. {@code "order-agent"}) to lazy card; iteration order follows the
	 * map implementation and must not be relied on for deterministic routing.
	 */
	private final Map<String, LazyAgentCard> lazyCards = new ConcurrentHashMap<>();

	/**
	 * Builds one {@link LazyAgentCard} per entry in
	 * {@link RemoteAgentProperties#agents()}; each card attempts an immediate fetch (see
	 * {@link LazyAgentCard} constructor).
	 * @param properties bound {@code remote.agents} map; must not be {@code null}
	 */
	public RemoteAgentTools(RemoteAgentProperties properties) {
		properties.agents().forEach((key, value) -> lazyCards.put(key, new LazyAgentCard(value.url())));
	}

	/**
	 * Sends one user message derived from {@link RemoteAgentDelegationRequest#task()} to
	 * the agent whose {@link AgentCard#name()} equals
	 * {@link RemoteAgentDelegationRequest#agentName()}.
	 *
	 * <p>
	 * Resolves the card via {@link #findByName(String)} (peek when possible, otherwise
	 * {@link LazyAgentCard#get()} for entries that are not yet cached). Blocks until
	 * {@link A2ATransport#send(AgentCard, Message)} completes or fails.
	 * </p>
	 * @param request non-null delegation target and task text
	 * @return downstream text, or a short English error line from
	 * {@link #unknownAgentMessage(String)} if no card matches {@code agentName}
	 */
	@Tool(description = "한 원격 에이전트에 한 건의 작업만 위임합니다. 특정 전문 에이전트에게 맡길 때 사용하세요.")
	public String sendMessage(@ToolParam(
			description = "단일 위임 요청. 에이전트 이름(agentName)과 작업 설명(task)을 포함합니다.") RemoteAgentDelegationRequest request) {
		Assert.notNull(request, "request must not be null");

		String agentName = request.agentName();
		String task = request.task();

		AgentCard agentCard = findByName(agentName);
		if (agentCard == null) {
			return unknownAgentMessage(agentName);
		}

		Message message = A2A.toUserMessage(task);
		return A2ATransport.send(agentCard, message);
	}

	/**
	 * Finds the first {@link AgentCard} whose {@link AgentCard#name()} equals
	 * {@code agentName} (case-sensitive, exact match).
	 *
	 * <p>
	 * For each configured {@link LazyAgentCard}, uses {@link LazyAgentCard#peek()} when
	 * populated; otherwise {@link LazyAgentCard#get()} for that entry only. Stops at the
	 * first name match; if multiple agents shared a name, which one wins is undefined
	 * because {@link Map#values()} iteration order is not specified.
	 * </p>
	 * @param agentName value from the model; must match the downstream card name
	 * @return the matching card, or {@code null} if no configured agent exposes that name
	 */
	private @Nullable AgentCard findByName(String agentName) {
		for (LazyAgentCard lazy : lazyCards.values()) {
			Optional<AgentCard> card = lazy.peek();
			if (card.isEmpty()) {
				card = lazy.get();
			}
			if (card.isPresent() && agentName.equals(card.get().name())) {
				return card.get();
			}
		}
		return null;
	}

	/**
	 * User-facing error when {@link #findByName(String)} fails; lists names from
	 * {@link #loadCachedCards()} only (peek cache). Agents not yet successfully resolved
	 * are omitted from the list even if configured.
	 * @param agentName the name the model supplied
	 * @return a single English sentence suitable as tool return text
	 */
	private String unknownAgentMessage(String agentName) {
		String availableAgents = loadCachedCards().stream().map(AgentCard::name).collect(Collectors.joining(", "));
		return "Agent '%s' not found. Available agents: %s".formatted(agentName, availableAgents);
	}

	/**
	 * Runs {@link #sendMessage} once per list element on
	 * {@link #VIRTUAL_THREAD_EXECUTOR}, waits for all to finish, then formats results in
	 * the same order as {@code requests}.
	 *
	 * <p>
	 * Do not use when outputs must be chained (one result feeds the next); use separate
	 * model turns with {@link #sendMessage} instead. Empty {@code requests} yields an
	 * empty string (after {@link String#trim()}).
	 * </p>
	 * @param requests ordered, non-null, no null elements
	 * @return numbered blocks {@code [n] agent: ... / response: ...} joined with newlines
	 */
	@Tool(description = """
			한 번의 호출로 서로 무관한 여러 위임을 병렬로 실행합니다. \
			한 에이전트의 응답이 다른 에이전트 호출의 입력이 되거나 순서가 중요하면 사용하지 말고 sendMessage를 여러 번 호출하세요. \
			반환값은 요청 순서대로 번호가 붙은 응답 블록들의 집계 텍스트입니다.""")
	public String sendMessagesParallel(@ToolParam(
			description = "병렬 위임 요청. 에이전트 이름(agentName)과 작업 설명(task)을 포함합니다.") List<RemoteAgentDelegationRequest> requests) {
		Assert.notNull(requests, "requests must not be null");
		Assert.noNullElements(requests, "requests must contain non-null elements");

		List<CompletableFuture<String>> futures = new ArrayList<>(requests.size());
		for (RemoteAgentDelegationRequest request : requests) {
			futures.add(CompletableFuture.supplyAsync(() -> sendMessage(request), VIRTUAL_THREAD_EXECUTOR));
		}
		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < requests.size(); i++) {
			RemoteAgentDelegationRequest request = requests.get(i);
			String agentName = request.agentName();
			String body = futures.get(i).join();
			out.append("[%d] agent: %s%nresponse:%n%s%n%n".formatted(i + 1, agentName, body));
		}
		return out.toString().trim();
	}

	/**
	 * Returns a JSON-formatted description of all available agents for the system prompt.
	 *
	 * <p>
	 * Uses {@link LazyAgentCard#peek()} only when at least one card is already cached, so
	 * steady-state invocations avoid redundant fetches. If nothing is cached yet (e.g.
	 * downstream agents were down at host startup), attempts {@link LazyAgentCard#get()}
	 * for each configured agent so the first user turn sees up-to-date routable agents.
	 * </p>
	 * @return newline-separated JSON objects {@code {"name": "...", "description":
	 * "..."}} for each included card; empty string when no agent could be resolved
	 */
	public String getAgentDescriptions() {
		return cardsForSystemPrompt().stream()
			.map(card -> String.format("{\"name\": \"%s\", \"description\": \"%s\"}", card.name(),
					Objects.requireNonNullElse(card.description(), "No description")))
			.collect(Collectors.joining("\n"));
	}

	/**
	 * Resolves which {@link AgentCard} instances to describe in the system prompt.
	 * @return {@link #loadCachedCards()} when non-empty; otherwise the result of calling
	 * {@link LazyAgentCard#get()} on every configured lazy card (may be a subset if some
	 * agents remain unreachable)
	 */
	private List<AgentCard> cardsForSystemPrompt() {
		List<AgentCard> cached = loadCachedCards();
		if (!cached.isEmpty()) {
			return cached;
		}
		return lazyCards.values().stream().map(LazyAgentCard::get).flatMap(Optional::stream).toList();
	}

	/**
	 * Snapshot of cards already in memory without triggering network I/O.
	 * @return all non-empty {@link LazyAgentCard#peek()} results (order follows
	 * {@link Map#values()})
	 */
	private List<AgentCard> loadCachedCards() {
		return lazyCards.values().stream().map(LazyAgentCard::peek).flatMap(java.util.Optional::stream).toList();
	}

}
