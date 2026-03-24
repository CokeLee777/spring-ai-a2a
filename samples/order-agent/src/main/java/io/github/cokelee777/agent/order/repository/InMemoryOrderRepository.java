package io.github.cokelee777.agent.order.repository;

import io.github.cokelee777.agent.order.domain.Order;
import io.github.cokelee777.agent.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed seed data backing {@link OrderRepository}, mimicking a small in-memory store.
 *
 * <p>
 * Includes at least one row per {@link OrderStatus} plus extra rows for duplicate phases
 * used to demo distinct payment outcomes.
 * </p>
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

	private static final Map<String, Order> orderStore = new LinkedHashMap<>();

	/**
	 * Initializes the repository with deterministic demo orders.
	 */
	public InMemoryOrderRepository() {
		put(new Order("ORD-1001", "노트북", 1_500_000L, LocalDate.of(2026, 3, 1), "TRACK-1001", OrderStatus.DELIVERED));
		put(new Order("ORD-1002", "마우스", 45_000L, LocalDate.of(2026, 3, 10), "TRACK-1002", OrderStatus.SHIPPED));
		put(new Order("ORD-1003", "키보드", 120_000L, LocalDate.of(2026, 3, 12), "TRACK-1003",
				OrderStatus.ORDER_COMPLETED));
		put(new Order("ORD-1004", "스마트워치", 329_000L, LocalDate.of(2026, 3, 13), "", OrderStatus.ORDER_WAITING));
		put(new Order("ORD-1010", "블루투스 이어폰", 59_000L, LocalDate.of(2026, 3, 13), "", OrderStatus.ORDER_WAITING));
		put(new Order("ORD-1005", "예약상품 세트", 210_000L, LocalDate.of(2026, 3, 14), "", OrderStatus.ORDER_RECEIVED));
		put(new Order("ORD-1006", "커피머신", 890_000L, LocalDate.of(2026, 3, 14), "", OrderStatus.ORDER_RECEIVED));
		put(new Order("ORD-1007", "모니터", 450_000L, LocalDate.of(2026, 3, 15), "TRACK-2007",
				OrderStatus.SHIPMENT_INSTRUCTED));
		put(new Order("ORD-1008", "사무용 의자", 198_000L, LocalDate.of(2026, 3, 16), "TRACK-2008",
				OrderStatus.PRODUCT_PREPARING));
		put(new Order("ORD-1009", "탁상시계", 35_000L, LocalDate.of(2026, 3, 17), "", OrderStatus.CANCEL_COMPLETED));
	}

	private void put(Order order) {
		orderStore.put(order.orderNumber(), order);
	}

	@Override
	public List<Order> findAll() {
		return List.copyOf(orderStore.values());
	}

	@Override
	public Optional<Order> findByOrderNumber(String orderNumber) {
		if (orderNumber == null || orderNumber.isBlank()) {
			return Optional.empty();
		}
		String query = orderNumber.strip();
		Order direct = orderStore.get(query);
		if (direct != null) {
			return Optional.of(direct);
		}
		return orderStore.values().stream().filter(o -> query.contains(o.orderNumber())).findFirst();
	}

}
