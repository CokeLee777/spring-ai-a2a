package io.github.cokelee777.agent.order.domain;

/**
 * E-commerce order lifecycle for the sample agent. Drives which downstream agents are
 * queried during cancellability checks.
 *
 * <p>
 * <strong>결제 에이전트</strong>: {@link #requiresPaymentAgentQuery()} 가 {@code true} 인 단계만
 * 호출합니다. {@link #ORDER_WAITING 주문대기} 는 결제가 아직 확정되지 않은 상태로 보고 생략합니다.
 * {@link #CANCEL_COMPLETED 취소완료} 는 환불·정산 여부 확인을 위해 호출합니다.
 * </p>
 *
 * <p>
 * <strong>배송 에이전트</strong>: {@link #requiresDeliveryAgentQuery()} 가 {@code true} 인 단계만
 * 호출합니다. {@link #SHIPMENT_INSTRUCTED 출고지시} 부터 {@link #DELIVERED 배송완료} 까지는 물류 추적이 의미 있다고
 * 보고, 그 이전 단계와 {@link #CANCEL_COMPLETED 취소완료} 는 생략합니다. 운송장이 없으면 단계와 무관하게 호출하지 않습니다.
 * </p>
 */
public enum OrderStatus {

	/** 결제 전 대기. */
	ORDER_WAITING("주문대기"),

	/** 주문 접수·결제 진행. */
	ORDER_RECEIVED("주문접수"),

	/** 결제 확정, 출고 전. */
	ORDER_COMPLETED("주문완료"),

	/** 창고 출고 지시(배송 연동 시작). */
	SHIPMENT_INSTRUCTED("출고지시"),

	/** 피킹·패킹 등 상품 준비. */
	PRODUCT_PREPARING("상품준비"),

	/** 택배 인계 완료. */
	SHIPPED("발송완료"),

	/** 고객 수령 완료. */
	DELIVERED("배송완료"),

	/** 취소·환불 처리 완료. */
	CANCEL_COMPLETED("취소완료");

	private final String displayLabel;

	OrderStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/**
	 * Human-readable label for tool output and LLM reasoning.
	 * @return Korean description of this phase
	 */
	public String displayLabel() {
		return displayLabel;
	}

	/**
	 * Whether the payment agent should be called for cancellability context.
	 * @return {@code false} only for {@link #ORDER_WAITING}
	 */
	public boolean requiresPaymentAgentQuery() {
		return this != ORDER_WAITING;
	}

	/**
	 * Whether the delivery agent should be called when a tracking number exists.
	 * @return {@code true} from {@link #SHIPMENT_INSTRUCTED} through {@link #DELIVERED}
	 */
	public boolean requiresDeliveryAgentQuery() {
		return switch (this) {
			case SHIPMENT_INSTRUCTED, PRODUCT_PREPARING, SHIPPED, DELIVERED -> true;
			default -> false;
		};
	}

	/**
	 * Explains why the payment agent was not invoked (only {@link #ORDER_WAITING}).
	 * @return Korean reason text without a leading tag
	 */
	public String paymentAgentOmissionDetail() {
		return "주문대기 단계에서는 결제가 확정되지 않아 결제 에이전트 조회를 생략합니다.";
	}

	/**
	 * Explains why the delivery agent was not invoked for this lifecycle phase.
	 * @return Korean reason text without a leading tag
	 */
	public String deliveryAgentOmissionDetail() {
		return switch (this) {
			case ORDER_WAITING -> "주문대기 단계에서는 출하·배송 정보가 없습니다.";
			case ORDER_RECEIVED -> "주문접수 단계로 아직 출고 지시 전입니다.";
			case ORDER_COMPLETED -> "주문완료(결제 확정) 단계이나 출고 지시 전이라 배송 에이전트 조회를 생략합니다.";
			case CANCEL_COMPLETED -> "취소 완료된 주문이므로 배송 추적이 필요하지 않습니다.";
			case SHIPMENT_INSTRUCTED, PRODUCT_PREPARING, SHIPPED, DELIVERED -> throw new IllegalStateException(
					"Delivery omission detail must not be used when delivery query is required: " + this);
		};
	}

}
