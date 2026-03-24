package io.github.cokelee777.agent.payment.domain;

/**
 * Settlement / attempt state for demo payments (aligned with order-agent ORD-* seeds).
 */
public enum PaymentStatus {

	/** Checkout started; no successful authorization yet. */
	AWAITING_PAYMENT("결제대기"),

	/** Issuer or PG fraud / limit hold after auth attempt. */
	AUTHORIZATION_HOLD("승인보류"),

	/** Funds captured and available for settlement. */
	CAPTURED("결제완료"),

	/** Authorization or capture rejected. */
	PAYMENT_FAILED("결제실패"),

	/** Some captured funds returned; order may still be open. */
	PARTIALLY_REFUNDED("부분환불"),

	/** All captured funds returned. */
	FULLY_REFUNDED("환불완료"),

	/** Auth voided or checkout abandoned before capture (no 매입). */
	VOIDED("결제취소");

	private final String displayLabel;

	PaymentStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/**
	 * Headline used in tool output.
	 * @return Korean label
	 */
	public String displayLabel() {
		return displayLabel;
	}

}
