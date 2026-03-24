package io.github.cokelee777.agent.delivery.domain;

/**
 * High-level parcel state for demo shipments (aligned with order-agent TRACK-* seeds).
 */
public enum DeliveryStatus {

	/** Delivered to recipient. */
	DELIVERED("배송완료"),

	/** In transit between hubs. */
	IN_TRANSIT("배송중"),

	/** Label created / not yet handed to carrier. */
	PREPARING("배송준비중");

	private final String displayLabel;

	DeliveryStatus(String displayLabel) {
		this.displayLabel = displayLabel;
	}

	/**
	 * Short headline used in tool output.
	 * @return Korean status label
	 */
	public String displayLabel() {
		return displayLabel;
	}

}
