package io.github.cokelee777.agentcore.orchestrator;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrchestratorAgentExecutor}.
 */
@ExtendWith(MockitoExtension.class)
class OrchestratorAgentExecutorTest {

	@Mock
	private ChatOrchestrator chatOrchestrator;

	@Mock
	private RequestContext requestContext;

	@Mock
	private AgentEmitter emitter;

	@InjectMocks
	private OrchestratorAgentExecutor executor;

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}

	private Message buildMessage(String text) {
		return Message.builder()
			.messageId("m1")
			.role(Message.Role.ROLE_USER)
			.parts(List.of(new TextPart(text)))
			.build();
	}

	@Test
	void execute_withSessionHeader_usesHeaderValue() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(OrchestratorAgentExecutor.SESSION_HEADER, "session-abc");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		when(requestContext.getMessage()).thenReturn(buildMessage("텍스트"));
		when(chatOrchestrator.handle(any(ChatRequest.class))).thenReturn(new ChatResponse("응답"));

		executor.execute(requestContext, emitter);

		verify(chatOrchestrator)
			.handle(argThat(r -> "텍스트".equals(r.userMessage()) && "session-abc".equals(r.sessionId())));
	}

	@Test
	void execute_withBlankHeader_fallsBackToUuid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(OrchestratorAgentExecutor.SESSION_HEADER, "   ");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		when(requestContext.getMessage()).thenReturn(buildMessage("텍스트"));
		when(chatOrchestrator.handle(any(ChatRequest.class))).thenReturn(new ChatResponse("응답"));

		executor.execute(requestContext, emitter);

		verify(chatOrchestrator).handle(any(ChatRequest.class));
	}

	@Test
	void execute_withNoServletContext_fallsBackToUuid() throws Exception {
		RequestContextHolder.resetRequestAttributes();
		when(requestContext.getMessage()).thenReturn(buildMessage("텍스트"));
		when(chatOrchestrator.handle(any(ChatRequest.class))).thenReturn(new ChatResponse("응답"));

		executor.execute(requestContext, emitter);

		verify(chatOrchestrator).handle(any(ChatRequest.class));
	}

	@Test
	void execute_success_callsAddArtifactAndComplete() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		when(requestContext.getMessage()).thenReturn(buildMessage("텍스트"));
		when(chatOrchestrator.handle(any(ChatRequest.class))).thenReturn(new ChatResponse("응답"));

		executor.execute(requestContext, emitter);

		verify(emitter).startWork();
		verify(emitter).addArtifact(any());
		verify(emitter).complete();
		verify(emitter, never()).fail();
	}

	@Test
	void execute_chatOrchestratorThrows_callsFail() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		when(requestContext.getMessage()).thenReturn(buildMessage("텍스트"));
		when(chatOrchestrator.handle(any(ChatRequest.class))).thenThrow(new RuntimeException("LLM error"));

		executor.execute(requestContext, emitter);

		verify(emitter).startWork();
		verify(emitter).fail();
		verify(emitter, never()).complete();
	}

	@Test
	void cancel_callsEmitterCancel() throws Exception {
		executor.cancel(requestContext, emitter);

		verify(emitter).cancel();
	}

}
