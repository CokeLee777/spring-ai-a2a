package io.github.cokelee777.agent.order.repository;

import io.github.cokelee777.agent.order.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to persisted orders for the sample agent.
 */
public interface OrderRepository {

	/**
	 * Returns all orders for the current (implicit) member, in stable insertion order.
	 * @return every seeded order
	 */
	List<Order> findAll();

	/**
	 * Resolves an order by number. Accepts a bare id or free text that contains a known
	 * id (e.g. {@code "주문 ORD-1001 확인"}) to mirror lenient tool input.
	 * @param orderNumber user- or LLM-provided reference, possibly null or blank
	 * @return the matching order, if any
	 */
	Optional<Order> findByOrderNumber(String orderNumber);

}
