package io.github.cokelee777.agent.payment;

import io.github.cokelee777.agent.payment.domain.Payment;
import io.github.cokelee777.agent.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for the Payment Agent.
 *
 * <p>
 * Exposes {@link #getPaymentStatus} backed by {@link PaymentRepository} (in-memory demo
 * data aligned with order-agent ORD-* identifiers).
 * </p>
 */
@Component
@RequiredArgsConstructor
public class PaymentTools {

	private final PaymentRepository paymentRepository;

	/**
	 * Returns payment status for the given order number.
	 * @param orderNumber the order number (e.g. {@code ORD-1001})
	 * @return payment status as plain text
	 */
	@Tool(description = "주문번호로 결제/환불 상태 조회")
	public String getPaymentStatus(@ToolParam(description = "결제/환불 상태를 조회할 주문번호 (예: ORD-1001)") String orderNumber) {
		return paymentRepository.findByOrderNumber(orderNumber)
			.map(Payment::toStatusLine)
			.orElse("해당 주문번호의 결제 정보를 찾을 수 없습니다.");
	}

}
