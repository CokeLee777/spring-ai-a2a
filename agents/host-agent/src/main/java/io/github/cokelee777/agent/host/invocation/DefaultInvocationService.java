package io.github.cokelee777.agent.host.invocation;

import io.github.cokelee777.agent.host.RemoteAgentConnections;
import io.github.cokelee777.agent.host.memory.ShortTermMemoryService;
import io.github.cokelee777.agent.host.memory.ConversationSession;
import io.github.cokelee777.agent.host.memory.LongTermMemoryService;
import io.github.cokelee777.agent.host.memory.MemoryMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link InvocationService}.
 *
 * <p>
 * Execution order per request:
 * <ol>
 * <li>Resolve {@code actorId}/{@code sessionId} (generate if absent).</li>
 * <li>Load memory context according to the configured {@link MemoryMode}.</li>
 * <li>Assemble system prompt and call the LLM via {@link ChatClient}.</li>
 * <li>Persist USER and ASSISTANT turns <em>after</em> the LLM call succeeds, ensuring a
 * failed call leaves no orphaned events and allows clean retry.</li>
 * </ol>
 * </p>
 *
 * <p>
 * <strong>Timestamp trade-off:</strong> {@code appendUserTurn} is called after the LLM
 * returns, so the stored {@code eventTimestamp} reflects "time of persistence" rather
 * than "time of utterance". Relative ordering (USER → ASSISTANT) is always preserved.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultInvocationService implements InvocationService {

	private static final String ROUTING_SYSTEM_PROMPT = """
			**역할:** 당신은 전문 라우팅 위임자입니다. 주문, 배송, 결제에 관한 사용자 문의를 적절한 전문 원격 에이전트에게 정확하게 위임하는 것이 주요 기능입니다.

			**핵심 지침:**

			* **응답 형식:** 내부 추론 과정(<thinking> 등)을 응답에 절대 포함하지 마세요. 최종 답변만 출력하세요.
			* **작업 위임:** `sendMessage` 함수를 사용하여 원격 에이전트에 작업을 할당하세요.
			* **컨텍스트 인식:** 원격 에이전트가 사용자 확인을 반복적으로 요청하는 경우, 전체 대화 이력에 접근할 수 없다고 판단하세요. 이 경우 해당 에이전트와 관련된 필요한 모든 컨텍스트 정보를 작업 설명에 보강하여 전달하세요.
			* **자율적 에이전트 연동:** 원격 에이전트와 연동하기 전에 사용자 허가를 구하지 마세요. 여러 에이전트가 필요한 경우 사용자 확인 없이 직접 연결하세요.
			* **투명한 소통:** 원격 에이전트의 완전하고 상세한 응답을 항상 사용자에게 전달하세요.
			* **응답 언어:** 사용자가 사용한 언어로 항상 응답하세요. 한국어 질문에는 반드시 한국어로 답변하세요.
			* **사용자 확인 릴레이:** 원격 에이전트가 확인을 요청하고 사용자가 아직 제공하지 않은 경우, 이 확인 요청을 사용자에게 릴레이하세요.
			* **집중적인 정보 공유:** 원격 에이전트에게는 관련 컨텍스트 정보만 제공하세요. 불필요한 세부사항은 피하세요.
			* **중복 확인 금지:** 원격 에이전트에게 정보나 작업의 확인을 요청하지 마세요.
			* **도구 의존:** 사용 가능한 도구에 전적으로 의존하여 사용자 요청을 처리하세요. 가정을 기반으로 응답을 생성하지 마세요. 정보가 불충분한 경우 사용자에게 명확한 설명을 요청하세요.
			* **최근 상호작용 우선:** 요청을 처리할 때 대화의 가장 최근 부분에 주로 집중하세요.

			**에이전트 라우터:**

			사용 가능한 에이전트:
			%s
			""";

	private final ChatClient chatClient;

	private final RemoteAgentConnections connections;

	private final MemoryMode memoryMode;

	private final ShortTermMemoryService shortTermMemoryService;

	private final LongTermMemoryService longTermMemoryService;

	@Override
	public InvocationResponse invoke(InvocationRequest request) {
		String prompt = request.prompt();
		ConversationSession session = ConversationSession.builder()
			.actorId(request.actorId())
			.sessionId(request.sessionId())
			.build();

		List<Message> history = memoryMode.supportsShortTerm() ? shortTermMemoryService.loadHistory(session)
				: Collections.emptyList();
		List<String> relevantMemories = memoryMode.supportsLongTerm()
				? longTermMemoryService.retrieveRelevant(session.actorId(), prompt) : Collections.emptyList();
		String response = chatClient.prompt()
			.system(buildSystemPrompt(relevantMemories))
			.messages(history)
			.user(prompt)
			.call()
			.content();
		String content = Objects.requireNonNullElse(response, "");

		persistTurns(session, prompt, content);

		log.info("session={} prompt={} response={}", session, prompt, content);
		return toResponse(content, session);
	}

	private String buildSystemPrompt(List<String> relevantMemories) {
		String base = ROUTING_SYSTEM_PROMPT.formatted(connections.getAgentDescriptions());
		if (relevantMemories.isEmpty()) {
			return base;
		}
		return base + "\n\n**관련 기억:**\n- " + String.join("\n- ", relevantMemories);
	}

	private void persistTurns(ConversationSession session, String prompt, String content) {
		if (!memoryMode.supportsShortTerm()) {
			return;
		}
		shortTermMemoryService.appendUserTurn(session, prompt);
		shortTermMemoryService.appendAssistantTurn(session, content);
	}

	private InvocationResponse toResponse(String content, ConversationSession session) {
		return memoryMode.isDisabled() ? new InvocationResponse(content, null, null)
				: new InvocationResponse(content, session.sessionId(), session.actorId());
	}

}
