package io.github.cokelee777.agent.payment.domain;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * In-memory payment row keyed by order number.
 *
 * @param orderNumber order id (e.g. {@code ORD-1001})
 * @param orderAmountKrw order / authorized amount in whole won
 * @param methodDescription e.g. {@code 카드결제} or {@code 간편결제}
 * @param paidDate capture or relevant event date; may be null before capture
 * @param state settlement state
 * @param refundedAmountKrw portion refunded when {@link PaymentStatus#PARTIALLY_REFUNDED}
 * or {@link PaymentStatus#FULLY_REFUNDED}
 * @param refundCompletedOn date refund completed; may be null for non-refund states
 */
public record Payment(String orderNumber, long orderAmountKrw, String methodDescription, LocalDate paidDate,
		PaymentStatus state, Long refundedAmountKrw, LocalDate refundCompletedOn) {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	/**
	 * Builds the plain-text line returned by the payment tool.
	 * @return order number, state, amounts, and method context
	 */
	public String toStatusLine() {
		String amount = String.format(Locale.KOREA, "%,d", orderAmountKrw);
		return switch (state) {
			case AWAITING_PAYMENT -> "%s 결제 상태: %s — 주문금액 %s원 (%s, 아직 결제 승인·입금 전)".formatted(orderNumber,
					state.displayLabel(), amount, methodDescription);
			case AUTHORIZATION_HOLD -> "%s 결제 상태: %s — %s원 (%s, %s)".formatted(orderNumber, state.displayLabel(),
					amount, methodDescription, paidDate != null ? paidDate.format(DATE) + " 승인 시도 후 보류" : "승인 보류 중");
			case CAPTURED -> "%s 결제 상태: %s — %s원 (%s, %s)".formatted(orderNumber, state.displayLabel(), amount,
					methodDescription, paidDate != null ? paidDate.format(DATE) : "-");
			case PAYMENT_FAILED -> "%s 결제 상태: %s — 시도 금액 %s원 (%s, %s)".formatted(orderNumber, state.displayLabel(),
					amount, methodDescription, paidDate != null ? paidDate.format(DATE) + " 거절" : "거절");
			case PARTIALLY_REFUNDED -> {
				String ref = refundedAmountKrw != null ? String.format(Locale.KOREA, "%,d", refundedAmountKrw) : "-";
				String refDay = refundCompletedOn != null ? refundCompletedOn.format(DATE) : "-";
				yield "%s 결제 상태: %s — 결제 %s원 중 %s원 환불 (%s, 환불일 %s)".formatted(orderNumber, state.displayLabel(), amount,
						ref, methodDescription, refDay);
			}
			case FULLY_REFUNDED -> {
				String refDay = refundCompletedOn != null ? refundCompletedOn.format(DATE) : "-";
				yield "%s 결제 상태: %s — %s원 전액 환불 (%s, 환불 완료일 %s)".formatted(orderNumber, state.displayLabel(), amount,
						methodDescription, refDay);
			}
			case VOIDED -> "%s 결제 상태: %s — %s원 (%s, 매입 전 취소·승인 자동 소멸)".formatted(orderNumber, state.displayLabel(),
					amount, methodDescription);
		};
	}

}
