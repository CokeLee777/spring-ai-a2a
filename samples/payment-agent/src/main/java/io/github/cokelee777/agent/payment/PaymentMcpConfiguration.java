package io.github.cokelee777.agent.payment;

import io.github.cokelee777.agent.payment.domain.Payment;
import io.github.cokelee777.agent.payment.repository.PaymentRepository;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP server capabilities for the payment agent: Tools, Resources, and Completions.
 *
 * <p>
 * Tools are exposed via {@link ToolCallbackProvider} so that
 * {@code ToolCallbackConverterAutoConfiguration} converts them to MCP
 * {@code SyncToolSpecification} automatically.
 *
 * <p>
 * Resources expose the full payment list at {@code payments://list}. Completions suggest
 * order numbers based on a prefix typed by the client.
 */
@Configuration
public class PaymentMcpConfiguration {

	/**
	 * Exposes {@link PaymentTools} methods as MCP tools via Spring AI's
	 * {@link ToolCallbackProvider}.
	 * @param tools the payment tools bean
	 * @return provider wrapping all {@code @Tool}-annotated methods
	 */
	@Bean
	public ToolCallbackProvider paymentMcpTools(PaymentTools tools) {
		return MethodToolCallbackProvider.builder().toolObjects(tools).build();
	}

	/**
	 * Exposes the full payment list as an MCP resource at {@code payments://list}.
	 * Delegates to {@link PaymentTools#getPaymentList()} to keep formatting logic in the
	 * tools layer.
	 * @param tools the payment tools bean
	 * @return singleton list containing the resource specification
	 */
	@Bean
	public List<McpServerFeatures.SyncResourceSpecification> paymentMcpResources(PaymentTools tools) {
		McpSchema.Resource resource = McpSchema.Resource.builder()
			.uri("payments://list")
			.name("전체 결제 목록")
			.description("주문번호별 현재 결제 상태 전체 목록")
			.mimeType("text/plain")
			.build();
		return List.of(new McpServerFeatures.SyncResourceSpecification(resource,
				(exchange, req) -> new McpSchema.ReadResourceResult(
						List.of(new McpSchema.TextResourceContents(req.uri(), "text/plain", tools.getPaymentList())))));
	}

	/**
	 * Provides auto-complete suggestions for order numbers at {@code payments://list}.
	 * @param repo payment repository for reading seeded rows
	 * @return singleton list containing the completion specification
	 */
	@Bean
	public List<McpServerFeatures.SyncCompletionSpecification> paymentMcpCompletions(PaymentRepository repo) {
		return List.of(new McpServerFeatures.SyncCompletionSpecification(
				new McpSchema.ResourceReference("payments://list"), (exchange, req) -> {
					String prefix = (req.argument() != null) ? req.argument().value() : "";
					List<String> candidates = repo.findAll()
						.stream()
						.map(Payment::orderNumber)
						.filter(n -> n.startsWith(prefix))
						.toList();
					return new McpSchema.CompleteResult(
							new McpSchema.CompleteResult.CompleteCompletion(candidates, candidates.size(), false));
				}));
	}

}
