package io.github.cokelee777.agent.host.remote;

import io.a2a.spec.AgentCard;
import io.github.cokelee777.a2a.agent.common.A2ATransport;
import io.github.cokelee777.a2a.agent.common.autoconfigure.RemoteAgentCardRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteAgentToolsTest {

	@Mock
	private RemoteAgentCardRegistry remoteAgentCardRegistry;

	@Test
	void delegateToRemoteAgent_null_throwsIllegalArgument() {
		RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);

		assertThatThrownBy(() -> tools.delegateToRemoteAgent(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("request");
	}

	@Test
	void delegateToRemoteAgent_unknownAgent_returnsEnglishMessageListingCachedNames() {
		AgentCard cached = mock(AgentCard.class);
		when(cached.name()).thenReturn("order-agent");
		when(remoteAgentCardRegistry.findCardByAgentName("unknown")).thenReturn(null);
		when(remoteAgentCardRegistry.peekCachedAgentCards()).thenReturn(List.of(cached));

		RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);
		String out = tools.delegateToRemoteAgent(new RemoteAgentDelegationRequest("unknown", "do work"));

		assertThat(out).contains("Agent 'unknown' not found").contains("order-agent");
	}

	@Test
	void delegateToRemoteAgent_resolvedAgent_usesA2ATransportSendStream() {
		AgentCard card = mock(AgentCard.class);
		when(remoteAgentCardRegistry.findCardByAgentName("pay")).thenReturn(card);

		try (MockedStatic<A2ATransport> transport = mockStatic(A2ATransport.class)) {
			transport.when(() -> A2ATransport.sendStream(eq(card), any())).thenReturn("downstream-text");

			RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);
			assertThat(tools.delegateToRemoteAgent(new RemoteAgentDelegationRequest("pay", "task body")))
				.isEqualTo("downstream-text");

			transport.verify(() -> A2ATransport.sendStream(eq(card), any()));
		}
	}

	@Test
	void delegateToRemoteAgentsParallel_null_throwsIllegalArgument() {
		RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);

		assertThatThrownBy(() -> tools.delegateToRemoteAgentsParallel(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("requests");
	}

	@Test
	void delegateToRemoteAgentsParallel_empty_returnsEmptyAggregatedOutput() {
		RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);

		assertThat(tools.delegateToRemoteAgentsParallel(List.of())).isEmpty();
	}

	@Test
	void delegateToRemoteAgentsParallel_nullElement_throwsIllegalArgument() {
		RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);
		var request = new RemoteAgentDelegationRequest("pay", "t");

		assertThatThrownBy(() -> tools.delegateToRemoteAgentsParallel(Arrays.asList(request, null)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("non-null");
	}

	@Test
	void delegateToRemoteAgentsParallel_unknownAgents_aggregateToolErrorsInOrder() {
		when(remoteAgentCardRegistry.findCardByAgentName("a")).thenReturn(null);
		when(remoteAgentCardRegistry.findCardByAgentName("b")).thenReturn(null);
		when(remoteAgentCardRegistry.peekCachedAgentCards()).thenReturn(List.of());

		RemoteAgentTools tools = new RemoteAgentTools(remoteAgentCardRegistry);
		String out = tools.delegateToRemoteAgentsParallel(
				List.of(new RemoteAgentDelegationRequest("a", "t1"), new RemoteAgentDelegationRequest("b", "t2")));

		assertThat(out).contains("[1] agent: a")
			.contains("Agent 'a' not found")
			.contains("[2] agent: b")
			.contains("Agent 'b' not found");
	}

}
