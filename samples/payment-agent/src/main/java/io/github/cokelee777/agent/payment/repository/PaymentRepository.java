package io.github.cokelee777.agent.payment.repository;

import io.github.cokelee777.agent.payment.domain.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to seeded payments for the sample payment agent.
 */
public interface PaymentRepository {

	/**
	 * Returns all seeded payments in stable insertion order.
	 * @return every payment row
	 */
	List<Payment> findAll();

	/**
	 * Resolves a payment by order reference; accepts bare ids or free text containing a
	 * known id.
	 * @param orderNumber user- or LLM-provided value, possibly null or blank
	 * @return the payment row, if any
	 */
	Optional<Payment> findByOrderNumber(String orderNumber);

}
