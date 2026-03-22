package io.github.cokelee777.agent.common;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LazyAgentCard}.
 */
class LazyAgentCardTest {

	private static final String AGENT_URL = "http://localhost:9001";

	// URI("http://localhost:9001").getPath() == "" → path + ".well-known/agent-card.json"
	private static final String EXPECTED_CARD_PATH = ".well-known/agent-card.json";

	@Test
	void get_constructorFetchSucceeds_returnsCard() {
		AgentCard mockCard = mockCard("order-agent");

		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(AGENT_URL, EXPECTED_CARD_PATH, null)).thenReturn(mockCard);

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);

			assertThat(lazyCard.get()).contains(mockCard);
		}
	}

	@Test
	void peek_constructorFetchSucceeds_returnsCard() {
		AgentCard mockCard = mockCard("order-agent");

		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(AGENT_URL, EXPECTED_CARD_PATH, null)).thenReturn(mockCard);

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);

			assertThat(lazyCard.peek()).contains(mockCard);
		}
	}

	@Test
	void get_constructorFetchFails_returnsEmpty() {
		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(any(), any(), any()))
				.thenThrow(new RuntimeException("Connection refused"));

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);

			assertThat(lazyCard.get()).isEmpty();
		}
	}

	@Test
	void peek_constructorFetchFails_returnsEmpty() {
		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(any(), any(), any()))
				.thenThrow(new RuntimeException("Connection refused"));

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);

			assertThat(lazyCard.peek()).isEmpty();
		}
	}

	@Test
	void peek_cardNotLoaded_doesNotRetry() {
		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(any(), any(), any()))
				.thenThrow(new RuntimeException("Connection refused"));

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);
			lazyCard.peek();
			lazyCard.peek();

			// Only the constructor call; peek() never triggers a network request
			a2aMock.verify(() -> A2A.getAgentCard(any(), any(), any()), times(1));
		}
	}

	@Test
	void get_cardNotLoaded_retriesOnEachCall() {
		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(any(), any(), any()))
				.thenThrow(new RuntimeException("Connection refused"));

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);
			lazyCard.get();
			lazyCard.get();

			// 1 (constructor) + 2 (get calls) = 3 total attempts
			a2aMock.verify(() -> A2A.getAgentCard(any(), any(), any()), times(3));
		}
	}

	@Test
	void get_retrySucceedsAfterInitialFailure_returnsCard() {
		AgentCard mockCard = mockCard("order-agent");

		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(any(), any(), any()))
				.thenThrow(new RuntimeException("Connection refused"))
				.thenReturn(mockCard);

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);
			Optional<AgentCard> result = lazyCard.get();

			assertThat(result).contains(mockCard);
		}
	}

	@Test
	void peek_afterRetrySucceeds_returnsCardWithoutNetworkCall() {
		AgentCard mockCard = mockCard("order-agent");

		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(any(), any(), any()))
				.thenThrow(new RuntimeException("Connection refused"))
				.thenReturn(mockCard);

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);
			lazyCard.get(); // triggers retry, succeeds and caches
			lazyCard.peek();
			lazyCard.peek();

			// 1 (constructor) + 1 (first get) = 2 total; peek() adds nothing
			a2aMock.verify(() -> A2A.getAgentCard(any(), any(), any()), times(2));
		}
	}

	@Test
	void get_afterCardCached_doesNotRetry() {
		AgentCard mockCard = mockCard("order-agent");

		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(AGENT_URL, EXPECTED_CARD_PATH, null)).thenReturn(mockCard);

			LazyAgentCard lazyCard = new LazyAgentCard(AGENT_URL);
			lazyCard.get();
			lazyCard.get();

			// Card is already cached after constructor; no additional fetches
			a2aMock.verify(() -> A2A.getAgentCard(any(), any(), any()), times(1));
		}
	}

	@Test
	void constructor_usesCorrectCardPath() {
		AgentCard mockCard = mockCard("order-agent");

		try (MockedStatic<A2A> a2aMock = mockStatic(A2A.class)) {
			a2aMock.when(() -> A2A.getAgentCard(AGENT_URL, EXPECTED_CARD_PATH, null)).thenReturn(mockCard);

			new LazyAgentCard(AGENT_URL);

			a2aMock.verify(() -> A2A.getAgentCard(AGENT_URL, EXPECTED_CARD_PATH, null));
		}
	}

	private static AgentCard mockCard(String name) {
		AgentCard card = mock(AgentCard.class);
		when(card.name()).thenReturn(name);
		return card;
	}

}
