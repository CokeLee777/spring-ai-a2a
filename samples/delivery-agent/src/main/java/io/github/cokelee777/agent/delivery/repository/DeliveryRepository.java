package io.github.cokelee777.agent.delivery.repository;

import io.github.cokelee777.agent.delivery.domain.Delivery;

import java.util.Optional;

/**
 * Read-only access to seeded shipments for the sample delivery agent.
 */
public interface DeliveryRepository {

	/**
	 * Resolves a shipment by tracking reference; accepts bare ids or free text containing
	 * a known id.
	 * @param trackingNumber user- or LLM-provided value, possibly null or blank
	 * @return the shipment, if any
	 */
	Optional<Delivery> findByTrackingNumber(String trackingNumber);

}
