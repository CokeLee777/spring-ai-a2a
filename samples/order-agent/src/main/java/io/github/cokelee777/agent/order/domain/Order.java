package io.github.cokelee777.agent.order.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * In-memory sample order row: identifiers, amounts, and a simple lifecycle phase.
 *
 * @param orderNumber unique order id (e.g. {@code ORD-1001})
 * @param productName display name of the product line
 * @param amountKrw price in KRW (whole won)
 * @param orderDate calendar date of the order
 * @param trackingNumber carrier tracking id for downstream delivery lookup (blank if not
 * yet issued)
 * @param lifecyclePhase coarse state for cancellability explanations
 */
public record Order(String orderNumber, String productName, long amountKrw, LocalDate orderDate, String trackingNumber,
		OrderStatus lifecyclePhase) {

	private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final String NO_TRACKING_LABEL = "미부여";

	/**
	 * Formats one bullet line for {@code getOrderList} output.
	 * @return list row matching the previous sample text shape
	 */
	public String toListLine() {
		String amount = String.format(Locale.KOREA, "%,d", amountKrw);
		return "- %s | 상품: %s | 금액: %s원 | 주문일: %s | 운송장: %s".formatted(orderNumber, productName, amount,
				orderDate.format(ORDER_DATE), trackingDisplay());
	}

	/**
	 * Formats the fixed order-state line used in {@code checkOrderCancellability}.
	 * @return order number, lifecycle label, and tracking number
	 */
	public String toCancellabilityStateLine() {
		return "%s 주문 상태: %s | 운송장: %s".formatted(orderNumber, lifecyclePhase.displayLabel(), trackingDisplay());
	}

	private String trackingDisplay() {
		return (trackingNumber == null || trackingNumber.isBlank()) ? NO_TRACKING_LABEL : trackingNumber;
	}

}
