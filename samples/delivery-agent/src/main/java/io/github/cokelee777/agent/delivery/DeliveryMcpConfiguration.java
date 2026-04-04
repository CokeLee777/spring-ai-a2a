package io.github.cokelee777.agent.delivery;

import io.github.cokelee777.agent.delivery.domain.Delivery;
import io.github.cokelee777.agent.delivery.repository.DeliveryRepository;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP server capabilities for the delivery agent: Tools, Resources, and Completions.
 *
 * <p>
 * Tools are exposed via {@link ToolCallbackProvider} so that
 * {@code ToolCallbackConverterAutoConfiguration} converts them to MCP
 * {@code SyncToolSpecification} automatically.
 *
 * <p>
 * Resources expose the full delivery list at {@code deliveries://list}. Completions
 * suggest tracking numbers based on a prefix typed by the client.
 */
@Configuration
public class DeliveryMcpConfiguration {

	/**
	 * Exposes {@link DeliveryTools} methods as MCP tools via Spring AI's
	 * {@link ToolCallbackProvider}.
	 * @param tools the delivery tools bean
	 * @return provider wrapping all {@code @Tool}-annotated methods
	 */
	@Bean
	public ToolCallbackProvider deliveryMcpTools(DeliveryTools tools) {
		return MethodToolCallbackProvider.builder().toolObjects(tools).build();
	}

	/**
	 * Exposes the full delivery list as an MCP resource at {@code deliveries://list}.
	 * Delegates to {@link DeliveryTools#getDeliveryList()} to keep formatting logic in
	 * the tools layer.
	 * @param tools the delivery tools bean
	 * @return singleton list containing the resource specification
	 */
	@Bean
	public List<McpServerFeatures.SyncResourceSpecification> deliveryMcpResources(DeliveryTools tools) {
		McpSchema.Resource resource = McpSchema.Resource.builder()
			.uri("deliveries://list")
			.name("전체 배송 목록")
			.description("운송장번호별 현재 배송 상태 전체 목록")
			.mimeType("text/plain")
			.build();
		return List.of(new McpServerFeatures.SyncResourceSpecification(resource,
				(exchange, req) -> new McpSchema.ReadResourceResult(List
					.of(new McpSchema.TextResourceContents(req.uri(), "text/plain", tools.getDeliveryList())))));
	}

	/**
	 * Provides auto-complete suggestions for tracking numbers at
	 * {@code deliveries://list}.
	 * @param repo delivery repository for reading seeded rows
	 * @return singleton list containing the completion specification
	 */
	@Bean
	public List<McpServerFeatures.SyncCompletionSpecification> deliveryMcpCompletions(DeliveryRepository repo) {
		return List.of(new McpServerFeatures.SyncCompletionSpecification(
				new McpSchema.ResourceReference("deliveries://list"), (exchange, req) -> {
					String prefix = (req.argument() != null) ? req.argument().value() : "";
					List<String> candidates = repo.findAll()
						.stream()
						.map(Delivery::trackingNumber)
						.filter(n -> n.startsWith(prefix))
						.toList();
					return new McpSchema.CompleteResult(
							new McpSchema.CompleteResult.CompleteCompletion(candidates, candidates.size(), false));
				}));
	}

}
