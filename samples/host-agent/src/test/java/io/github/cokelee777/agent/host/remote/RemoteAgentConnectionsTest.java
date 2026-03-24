package io.github.cokelee777.agent.host.remote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteAgentConnectionsTest {

	private RemoteAgentConnections connections;

	@BeforeEach
	void setUp() {
		connections = new RemoteAgentConnections(
				new RemoteAgentProperties(Map.of("dummy", new RemoteAgentProperties.Agent("http://127.0.0.1:9"))));
	}

	@Test
	void sendMessagesParallel_null_rejectsWithIllegalArgument() {
		assertThatThrownBy(() -> connections.sendMessagesParallel(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("requests");
	}

	@Test
	void sendMessagesParallel_empty_returnsEmptyAggregatedOutput() {
		assertThat(connections.sendMessagesParallel(List.of())).isEmpty();
	}

}
