package io.github.cokelee777.agent.delivery.domain;

/**
 * In-memory shipment row keyed by tracking number.
 *
 * @param trackingNumber carrier tracking id (e.g. {@code TRACK-1001})
 * @param status coarse delivery state
 * @param detailSuffix trailing explanation after the status headline (leading space or em
 * dash included)
 */
public record Delivery(String trackingNumber, DeliveryStatus status, String detailSuffix) {

	/**
	 * Builds the plain-text line returned by the delivery tool.
	 * @return tracking number, status, and detail
	 */
	public String toStatusLine() {
		return "%s 배송 상태: %s%s".formatted(trackingNumber, status.displayLabel(), detailSuffix);
	}

}
