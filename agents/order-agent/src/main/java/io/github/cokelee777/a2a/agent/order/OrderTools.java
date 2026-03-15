package io.github.cokelee777.a2a.agent.order;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for the Order Agent.
 *
 * <p>
 * Exposes {@link #getOrderList} and {@link #checkOrderCancellability} as LLM-callable
 * tools. Mock data mirrors the original {@code OrderListSkillExecutor} and
 * {@code OrderCancellabilitySkillExecutor}.
 * </p>
 */
@Component
public class OrderTools {

	/**
	 * Returns mock order history for the current member.
	 * <p>
	 * Note: memberId is fixed (assumed from session context).
	 * </p>
	 * @return formatted order list as plain text
	 */
	@Tool(description = "현재 회원의 주문 내역 목록 조회")
	public String getOrderList() {
		return """
				[주문 내역]
				- ORD-1001 | 상품: 노트북 | 금액: 1,500,000원 | 상태: 배송완료 | 주문일: 2026-03-01
				- ORD-1002 | 상품: 마우스 | 금액: 45,000원 | 상태: 배송중 | 주문일: 2026-03-10
				- ORD-1003 | 상품: 키보드 | 금액: 120,000원 | 상태: 결제완료 | 주문일: 2026-03-12
				""";
	}

	/**
	 * Returns mock cancellability verdict for the given order number.
	 * @param orderNumber the order number to check (e.g., {@code ORD-1001})
	 * @return cancellability verdict as plain text
	 */
	@Tool(description = "주문 취소 가능 여부 확인. 주문번호(ORD-xxxx) 필요")
	public String checkOrderCancellability(
			@ToolParam(description = "취소 가능 여부를 확인할 주문번호 (예: ORD-1001)") String orderNumber) {
		if (orderNumber.contains("ORD-1001")) {
			return "ORD-1001 취소 불가 — 이미 배송이 완료된 주문입니다.";
		}
		if (orderNumber.contains("ORD-1002")) {
			return "ORD-1002 취소 가능 — 배송 준비 단계이므로 취소할 수 있습니다.";
		}
		if (orderNumber.contains("ORD-1003")) {
			return "ORD-1003 취소 가능 — 아직 결제 완료 단계이므로 취소할 수 있습니다.";
		}
		return "해당 주문번호의 취소 가능 여부를 확인할 수 없습니다.";
	}

}
