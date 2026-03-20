package io.github.cokelee777.agent.host.memory.bedrock;

import io.github.cokelee777.agent.host.memory.MemoryMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.model.MemoryContent;
import software.amazon.awssdk.services.bedrockagentcore.model.MemoryRecordSummary;
import software.amazon.awssdk.services.bedrockagentcore.model.RetrieveMemoryRecordsRequest;
import software.amazon.awssdk.services.bedrockagentcore.model.RetrieveMemoryRecordsResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BedrockLongTermMemoryServiceTest {

	@Mock
	private BedrockAgentCoreClient client;

	@Test
	void emptyStrategyId_returnsEmptyWithoutApiCall() {
		BedrockMemoryProperties props = new BedrockMemoryProperties("mem-1", MemoryMode.BOTH, 10, "", 4);
		BedrockLongTermMemoryService service = new BedrockLongTermMemoryService(client, props);

		List<String> result = service.retrieveRelevant("actor-1", "query");

		assertThat(result).isEmpty();
		verifyNoInteractions(client);
	}

	@Test
	void retrieveRelevant_callsApiWithCorrectNamespaceAndCriteria() {
		BedrockMemoryProperties props = new BedrockMemoryProperties("mem-1", MemoryMode.BOTH, 10, "strat-1", 4);
		BedrockLongTermMemoryService service = new BedrockLongTermMemoryService(client, props);

		MemoryRecordSummary summary = MemoryRecordSummary.builder()
			.content(MemoryContent.fromText("past order info"))
			.build();
		RetrieveMemoryRecordsResponse response = RetrieveMemoryRecordsResponse.builder()
			.memoryRecordSummaries(List.of(summary))
			.build();
		when(client.retrieveMemoryRecords(any(RetrieveMemoryRecordsRequest.class))).thenReturn(response);

		List<String> result = service.retrieveRelevant("actor-1", "my order");

		ArgumentCaptor<RetrieveMemoryRecordsRequest> captor = ArgumentCaptor
			.forClass(RetrieveMemoryRecordsRequest.class);
		verify(client).retrieveMemoryRecords(captor.capture());
		RetrieveMemoryRecordsRequest req = captor.getValue();
		assertThat(req.memoryId()).isEqualTo("mem-1");
		assertThat(req.namespace()).isEqualTo("/strategies/strat-1/actors/actor-1");
		assertThat(req.searchCriteria().memoryStrategyId()).isEqualTo("strat-1");
		assertThat(req.searchCriteria().searchQuery()).isEqualTo("my order");
		assertThat(req.maxResults()).isEqualTo(4);
		assertThat(result).containsExactly("past order info");
	}

	@Test
	void placeholderStrategyId_returnsEmptyWithoutApiCall() {
		BedrockMemoryProperties props = new BedrockMemoryProperties("mem-1", MemoryMode.BOTH, 10,
				"placeholder-strategy-id", 4);
		BedrockLongTermMemoryService service = new BedrockLongTermMemoryService(client, props);

		List<String> result = service.retrieveRelevant("actor-1", "query");

		assertThat(result).isEmpty();
		verifyNoInteractions(client);
	}

}
