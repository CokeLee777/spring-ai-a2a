package io.github.cokelee777.agent.delivery.repository;

import io.github.cokelee777.agent.delivery.domain.DeliveryStatus;
import io.github.cokelee777.agent.delivery.domain.Delivery;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fixed TRACK-* rows aligned with {@code order-agent} seed orders.
 */
@Repository
public class InMemoryDeliveryRepository implements DeliveryRepository {

	private static final Map<String, Delivery> deliveryStore = new LinkedHashMap<>();

	/**
	 * Seeds demo shipments matching order-agent 운송장 번호.
	 */
	public InMemoryDeliveryRepository() {
		put(new Delivery("TRACK-1001", DeliveryStatus.DELIVERED, " — 2026-03-05 14:32 수령인 인도 완료"));
		put(new Delivery("TRACK-1002", DeliveryStatus.IN_TRANSIT, " — 현재 서울 물류센터에서 출발 (2026-03-12 09:15)"));
		put(new Delivery("TRACK-1003", DeliveryStatus.PREPARING, " — 아직 발송되지 않았습니다"));
		put(new Delivery("TRACK-2007", DeliveryStatus.PREPARING, " — 출고 지시 반영, 패킹·집하 대기 (2026-03-15 08:40)"));
		put(new Delivery("TRACK-2008", DeliveryStatus.IN_TRANSIT, " — 부산 허브 경유 중 (2026-03-18 11:20)"));
	}

	private void put(Delivery delivery) {
		deliveryStore.put(delivery.trackingNumber(), delivery);
	}

	@Override
	public List<Delivery> findAll() {
		return List.copyOf(deliveryStore.values());
	}

	@Override
	public Optional<Delivery> findByTrackingNumber(String trackingNumber) {
		if (trackingNumber == null || trackingNumber.isBlank()) {
			return Optional.empty();
		}
		String query = trackingNumber.strip();
		Delivery direct = deliveryStore.get(query);
		if (direct != null) {
			return Optional.of(direct);
		}
		return deliveryStore.values().stream().filter(s -> query.contains(s.trackingNumber())).findFirst();
	}

}
