package io.github.cokelee777.agent.delivery;

import io.github.cokelee777.agent.delivery.domain.Delivery;
import io.github.cokelee777.agent.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Spring AI tools for the Delivery Agent.
 *
 * <p>
 * Exposes {@link #getDeliveryList} and {@link #trackDelivery} backed by
 * {@link DeliveryRepository} (in-memory demo data aligned with order-agent TRACK-*
 * identifiers).
 * </p>
 */
@Component
@RequiredArgsConstructor
public class DeliveryTools {

	private final DeliveryRepository deliveryRepository;

	/**
	 * Returns all seeded deliveries as a formatted plain-text list.
	 * @return every delivery status line, prefixed with {@code [배송 목록]}
	 */
	@Tool(description = "전체 배송 목록 조회. 모든 운송장의 현재 배송 상태를 반환합니다.")
	public String getDeliveryList() {
		return deliveryRepository.findAll()
			.stream()
			.map(Delivery::toStatusLine)
			.collect(Collectors.joining("\n", "[배송 목록]\n", "\n"));
	}

	/**
	 * Returns delivery status for the given tracking number.
	 * @param trackingNumber the shipment tracking number (e.g. {@code TRACK-1001})
	 * @return delivery status as plain text
	 */
	@Tool(description = "운송장번호로 배송 상태 조회")
	public String trackDelivery(@ToolParam(description = "조회할 운송장번호 (예: TRACK-1001)") String trackingNumber) {
		return deliveryRepository.findByTrackingNumber(trackingNumber)
			.map(Delivery::toStatusLine)
			.orElse("해당 운송장번호의 배송 정보를 찾을 수 없습니다.");
	}

}
