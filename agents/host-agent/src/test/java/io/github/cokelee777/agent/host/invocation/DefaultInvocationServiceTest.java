package io.github.cokelee777.agent.host.invocation;

import io.github.cokelee777.agent.host.RemoteAgentConnections;
import io.github.cokelee777.agent.host.memory.ShortTermMemoryService;
import io.github.cokelee777.agent.host.memory.ConversationSession;
import io.github.cokelee777.agent.host.memory.LongTermMemoryService;
import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInvocationServiceTest {

	@Mock
	private ShortTermMemoryService shortTermMemoryService;

	@Mock
	private LongTermMemoryService longTermMemoryService;

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private ChatClient.CallResponseSpec callSpec;

	@Mock
	private RemoteAgentConnections connections;

	@Test
	void modeNone_nullSessionInResponse() {
		DefaultInvocationService service = serviceWith(MemoryMode.NONE);
		setupChatClientChain("reply");
		when(connections.getAgentDescriptions()).thenReturn("");

		InvocationResponse response = service.invoke(new InvocationRequest("hello", null, null));

		assertThat(response.content()).isEqualTo("reply");
		assertThat(response.sessionId()).isNull();
		assertThat(response.actorId()).isNull();
	}

	@Test
	void modeShortTerm_loadsHistoryAndSavesTurnsAfterChatClient() {
		DefaultInvocationService service = serviceWith(MemoryMode.SHORT_TERM);
		when(shortTermMemoryService.loadHistory(any(ConversationSession.class)))
			.thenReturn(List.of(new UserMessage("prev")));
		setupChatClientChain("ok");
		when(connections.getAgentDescriptions()).thenReturn("");

		service.invoke(new InvocationRequest("hi", "actor-1", "session-1"));

		InOrder order = inOrder(chatClient, shortTermMemoryService);
		order.verify(chatClient).prompt();
		order.verify(shortTermMemoryService).appendUserTurn(any(ConversationSession.class), eq("hi"));
		order.verify(shortTermMemoryService).appendAssistantTurn(any(ConversationSession.class), eq("ok"));
		verifyNoInteractions(longTermMemoryService);
	}

	@Test
	void modeLongTerm_retrievesRelevantAndDoesNotPersistShortTermTurns() {
		DefaultInvocationService service = serviceWith(MemoryMode.LONG_TERM);
		when(longTermMemoryService.retrieveRelevant(anyString(), anyString())).thenReturn(List.of("past info"));
		setupChatClientChain("response");
		when(connections.getAgentDescriptions()).thenReturn("");

		service.invoke(new InvocationRequest("query", "actor-1", "session-1"));

		verify(longTermMemoryService).retrieveRelevant("actor-1", "query");
		verify(shortTermMemoryService, never()).loadHistory(any(ConversationSession.class));
		verify(shortTermMemoryService, never()).appendUserTurn(any(ConversationSession.class), anyString());
		verify(shortTermMemoryService, never()).appendAssistantTurn(any(ConversationSession.class), anyString());
	}

	@Test
	void chatClientFailure_noMemorySaved() {
		DefaultInvocationService service = serviceWith(MemoryMode.BOTH);
		when(shortTermMemoryService.loadHistory(any(ConversationSession.class))).thenReturn(List.of());
		when(longTermMemoryService.retrieveRelevant(anyString(), anyString())).thenReturn(List.of());
		when(connections.getAgentDescriptions()).thenReturn("");
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.system(anyString())).thenReturn(requestSpec);
		when(requestSpec.messages(anyList())).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.call()).thenThrow(new RuntimeException("LLM error"));

		assertThatThrownBy(() -> service.invoke(new InvocationRequest("hi", "actor-1", "session-1")))
			.isInstanceOf(RuntimeException.class);

		verify(shortTermMemoryService, never()).appendUserTurn(any(ConversationSession.class), anyString());
		verify(shortTermMemoryService, never()).appendAssistantTurn(any(ConversationSession.class), anyString());
	}

	@Test
	void noSessionId_generatesNewSessionIdInResponse() {
		DefaultInvocationService service = serviceWith(MemoryMode.SHORT_TERM);
		when(shortTermMemoryService.loadHistory(any(ConversationSession.class))).thenReturn(List.of());
		setupChatClientChain("hi");
		when(connections.getAgentDescriptions()).thenReturn("");

		InvocationResponse response = service.invoke(new InvocationRequest("hello", null, null));

		assertThat(response.sessionId()).isNotBlank();
		assertThat(response.actorId()).isNotBlank();
	}

	@Test
	void providedSessionId_returnsSameSessionIdInResponse() {
		DefaultInvocationService service = serviceWith(MemoryMode.SHORT_TERM);
		when(shortTermMemoryService.loadHistory(any(ConversationSession.class))).thenReturn(List.of());
		setupChatClientChain("reply");
		when(connections.getAgentDescriptions()).thenReturn("");

		InvocationResponse response = service.invoke(new InvocationRequest("hi", "actor-1", "sess-42"));

		assertThat(response.sessionId()).isEqualTo("sess-42");
		assertThat(response.actorId()).isEqualTo("actor-1");
	}

	private DefaultInvocationService serviceWith(MemoryMode mode) {
		return new DefaultInvocationService(chatClient, connections, mode, shortTermMemoryService,
				longTermMemoryService);
	}

	private void setupChatClientChain(String content) {
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.system(anyString())).thenReturn(requestSpec);
		when(requestSpec.messages(anyList())).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callSpec);
		when(callSpec.content()).thenReturn(content);
	}

}
