package io.github.cokelee777.agent.payment.repository;

import io.github.cokelee777.agent.payment.domain.Payment;
import io.github.cokelee777.agent.payment.domain.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed ORD-* rows aligned with {@code order-agent} seeds; exercises every
 * {@link PaymentStatus}.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

	private static final Map<String, Payment> paymentStore = new LinkedHashMap<>();

	/**
	 * Seeds demo payments for each order row (including duplicate lifecycle demos).
	 */
	public InMemoryPaymentRepository() {
		put(new Payment("ORD-1001", 1_500_000L, "카드결제", LocalDate.of(2026, 3, 1), PaymentStatus.CAPTURED, null, null));
		put(new Payment("ORD-1002", 45_000L, "카드결제", LocalDate.of(2026, 3, 10), PaymentStatus.CAPTURED, null, null));
		put(new Payment("ORD-1003", 120_000L, "간편결제", LocalDate.of(2026, 3, 12), PaymentStatus.CAPTURED, null, null));
		put(new Payment("ORD-1004", 329_000L, "카드·간편결제 시도", null, PaymentStatus.AWAITING_PAYMENT, null, null));
		put(new Payment("ORD-1010", 59_000L, "카드결제", null, PaymentStatus.VOIDED, null, null));
		put(new Payment("ORD-1005", 210_000L, "카드결제", LocalDate.of(2026, 3, 14), PaymentStatus.AUTHORIZATION_HOLD, null,
				null));
		put(new Payment("ORD-1006", 890_000L, "카드결제", LocalDate.of(2026, 3, 14), PaymentStatus.PAYMENT_FAILED, null,
				null));
		put(new Payment("ORD-1007", 450_000L, "카드결제", LocalDate.of(2026, 3, 15), PaymentStatus.CAPTURED, null, null));
		put(new Payment("ORD-1008", 198_000L, "카드결제", LocalDate.of(2026, 3, 16), PaymentStatus.PARTIALLY_REFUNDED,
				55_000L, LocalDate.of(2026, 3, 17)));
		put(new Payment("ORD-1009", 35_000L, "간편결제", LocalDate.of(2026, 3, 17), PaymentStatus.FULLY_REFUNDED, 35_000L,
				LocalDate.of(2026, 3, 18)));
	}

	private void put(Payment payment) {
		paymentStore.put(payment.orderNumber(), payment);
	}

	@Override
	public Optional<Payment> findByOrderNumber(String orderNumber) {
		if (orderNumber == null || orderNumber.isBlank()) {
			return Optional.empty();
		}
		String query = orderNumber.strip();
		Payment direct = paymentStore.get(query);
		if (direct != null) {
			return Optional.of(direct);
		}
		return paymentStore.values().stream().filter(p -> query.contains(p.orderNumber())).findFirst();
	}

}
