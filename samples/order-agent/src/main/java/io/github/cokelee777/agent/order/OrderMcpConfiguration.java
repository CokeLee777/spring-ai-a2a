package io.github.cokelee777.agent.order;

import io.github.cokelee777.agent.order.domain.Order;
import io.github.cokelee777.agent.order.repository.OrderRepository;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP server capabilities for the order agent: Tools, Resources, and Completions.
 *
 * <p>
 * Tools are exposed via {@link ToolCallbackProvider} so that
 * {@code ToolCallbackConverterAutoConfiguration} converts them to MCP
 * {@code SyncToolSpecification} automatically.
 *
 * <p>
 * Resources expose the full order list at {@code orders://list}. Completions suggest
 * order numbers based on a prefix typed by the client.
 */
@Configuration
public class OrderMcpConfiguration {

	/**
	 * Exposes {@link OrderTools} methods as MCP tools via Spring AI's
	 * {@link ToolCallbackProvider}.
	 * @param tools the order tools bean
	 * @return provider wrapping all {@code @Tool}-annotated methods
	 */
	@Bean
	public ToolCallbackProvider orderMcpTools(OrderTools tools) {
		return MethodToolCallbackProvider.builder().toolObjects(tools).build();
	}

	/**
	 * Exposes the full order list as an MCP resource at {@code orders://list}. Delegates
	 * to {@link OrderTools#getOrderList()} to keep formatting logic in the tools layer.
	 * @param tools the order tools bean
	 * @return singleton list containing the resource specification
	 */
	@Bean
	public List<McpServerFeatures.SyncResourceSpecification> orderMcpResources(OrderTools tools) {
		McpSchema.Resource resource = McpSchema.Resource.builder()
			.uri("orders://list")
			.name("전체 주문 목록")
			.description("주문번호별 상품명, 금액, 주문일, 운송장번호 전체 목록")
			.mimeType("text/plain")
			.build();
		return List.of(new McpServerFeatures.SyncResourceSpecification(resource,
				(exchange, req) -> new McpSchema.ReadResourceResult(
						List.of(new McpSchema.TextResourceContents(req.uri(), "text/plain", tools.getOrderList())))));
	}

	/**
	 * Provides auto-complete suggestions for order numbers at {@code orders://list}.
	 * @param repo order repository for reading seeded rows
	 * @return singleton list containing the completion specification
	 */
	@Bean
	public List<McpServerFeatures.SyncCompletionSpecification> orderMcpCompletions(OrderRepository repo) {
		return List.of(new McpServerFeatures.SyncCompletionSpecification(
				new McpSchema.ResourceReference("orders://list"), (exchange, req) -> {
					String prefix = (req.argument() != null) ? req.argument().value() : "";
					List<String> candidates = repo.findAll()
						.stream()
						.map(Order::orderNumber)
						.filter(n -> n.startsWith(prefix))
						.toList();
					return new McpSchema.CompleteResult(
							new McpSchema.CompleteResult.CompleteCompletion(candidates, candidates.size(), false));
				}));
	}

}
