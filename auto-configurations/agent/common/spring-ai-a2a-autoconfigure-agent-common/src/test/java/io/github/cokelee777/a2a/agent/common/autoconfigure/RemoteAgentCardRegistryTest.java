package io.github.cokelee777.a2a.agent.common.autoconfigure;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class RemoteAgentCardRegistryTest {

	private static final String URL_A = "http://127.0.0.1:9001";

	private static final String URL_B = "http://127.0.0.1:9002";

	private static final String EXPECTED_CARD_PATH = ".well-known/agent-card.json";

	private static RemoteAgentProperties props(String key, String url) {
		return new RemoteAgentProperties(Map.of(key, new RemoteAgentProperties.Agent(url)));
	}

	private static RemoteAgentProperties propsTwoAgents() {
		return new RemoteAgentProperties(
				Map.of("order", new RemoteAgentProperties.Agent(URL_A), "pay", new RemoteAgentProperties.Agent(URL_B)));
	}

	private static AgentCard mockCard(String name, String description) {
		AgentCard card = mock(AgentCard.class);
		when(card.name()).thenReturn(name);
		when(card.description()).thenReturn(description);
		return card;
	}

	@Test
	void constructor_nullProperties_throwsIllegalArgument() {
		assertThatThrownBy(() -> new RemoteAgentCardRegistry(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("properties");
	}

	@Test
	void getAgentDescriptions_emptyRegistry_returnsEmptyString() {
		assertThat(new RemoteAgentCardRegistry(new RemoteAgentProperties(Map.of())).getAgentDescriptions()).isEmpty();
	}

	@Test
	void findLazyCardByAgentName_unknownName_throwsIllegalArgument() {
		RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(props("a", "http://127.0.0.1:9"));

		assertThatThrownBy(() -> registry.findLazyCardByAgentName("missing"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("missing");
	}

	@Test
	void findLazyCardByAgentName_blank_throwsIllegalArgument() {
		RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(props("a", "http://127.0.0.1:9"));

		assertThatThrownBy(() -> registry.findLazyCardByAgentName("   ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("agentName");
	}

	@Test
	void findLazyCardByAgentName_configuredName_returnsLazyCard() {
		RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(props("a", "http://127.0.0.1:9"));

		assertThat(registry.findLazyCardByAgentName("a")).isNotNull();
	}

	@Test
	void findCardByAgentName_blank_throwsIllegalArgument() {
		RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(props("a", URL_A));

		assertThatThrownBy(() -> registry.findCardByAgentName("")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("agentName");
	}

	@Test
	void findCardByAgentName_returnsFirstCardWhoseNameMatches() {
		AgentCard orderCard = mockCard("order-agent", "Orders");
		AgentCard payCard = mockCard("payment-agent", "Payments");

		try (MockedStatic<A2A> a2a = mockStatic(A2A.class)) {
			a2a.when(() -> A2A.getAgentCard(URL_A, EXPECTED_CARD_PATH, null)).thenReturn(orderCard);
			a2a.when(() -> A2A.getAgentCard(URL_B, EXPECTED_CARD_PATH, null)).thenReturn(payCard);

			RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(propsTwoAgents());

			assertThat(registry.findCardByAgentName("payment-agent")).isSameAs(payCard);
			assertThat(registry.findCardByAgentName("missing")).isNull();
		}
	}

	@Test
	void findCardByAgentName_usesGetWhenPeekEmpty() {
		AgentCard card = mockCard("late-agent", "Late");

		try (MockedStatic<A2A> a2a = mockStatic(A2A.class)) {
			a2a.when(() -> A2A.getAgentCard(URL_A, EXPECTED_CARD_PATH, null))
				.thenThrow(new RuntimeException("down"))
				.thenReturn(card);

			RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(props("a", URL_A));

			assertThat(registry.findCardByAgentName("late-agent")).isSameAs(card);
			a2a.verify(() -> A2A.getAgentCard(any(), any(), any()), times(2));
		}
	}

	@Test
	void peekCachedAgentCards_returnsOnlySuccessfullyLoadedCards() {
		AgentCard only = mockCard("one", "d");

		try (MockedStatic<A2A> a2a = mockStatic(A2A.class)) {
			a2a.when(() -> A2A.getAgentCard(URL_A, EXPECTED_CARD_PATH, null)).thenReturn(only);
			a2a.when(() -> A2A.getAgentCard(URL_B, EXPECTED_CARD_PATH, null))
				.thenThrow(new RuntimeException("unavailable"));

			RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(propsTwoAgents());

			assertThat(registry.peekCachedAgentCards()).containsExactly(only);
		}
	}

	@Test
	void getAgentDescriptions_whenPeekCached_skipsAdditionalFetch() {
		AgentCard card = mockCard("order-agent", "Handles orders");

		try (MockedStatic<A2A> a2a = mockStatic(A2A.class)) {
			a2a.when(() -> A2A.getAgentCard(URL_A, EXPECTED_CARD_PATH, null)).thenReturn(card);

			RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(props("order", URL_A));
			String descriptions = registry.getAgentDescriptions();

			assertThat(descriptions).contains("order-agent").contains("Handles orders");
			a2a.verify(() -> A2A.getAgentCard(any(), any(), any()), times(1));
		}
	}

	@Test
	void getAgentDescriptions_whenNothingCached_retriesGetPerAgent() {
		try (MockedStatic<A2A> a2a = mockStatic(A2A.class)) {
			a2a.when(() -> A2A.getAgentCard(any(), any(), any())).thenThrow(new RuntimeException("offline"));

			RemoteAgentCardRegistry registry = new RemoteAgentCardRegistry(propsTwoAgents());

			assertThat(registry.getAgentDescriptions()).isEmpty();
			a2a.verify(() -> A2A.getAgentCard(any(), any(), any()), times(4));
		}
	}

}
