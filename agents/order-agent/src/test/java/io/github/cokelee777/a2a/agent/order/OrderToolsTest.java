package io.github.cokelee777.a2a.agent.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderToolsTest {

	private final OrderTools tools = new OrderTools();

	@Test
	void getOrderList_returnsAllOrders() {
		String result = tools.getOrderList();
		assertThat(result).contains("ORD-1001").contains("ORD-1002").contains("ORD-1003");
	}

	@Test
	void checkOrderCancellability_ord1001_notCancellable() {
		assertThat(tools.checkOrderCancellability("ORD-1001")).contains("취소 불가");
	}

	@Test
	void checkOrderCancellability_ord1002_cancellable() {
		assertThat(tools.checkOrderCancellability("ORD-1002")).contains("취소 가능");
	}

	@Test
	void checkOrderCancellability_ord1003_cancellable() {
		assertThat(tools.checkOrderCancellability("ORD-1003")).contains("취소 가능");
	}

	@Test
	void checkOrderCancellability_unknown_returnsNotFound() {
		assertThat(tools.checkOrderCancellability("ORD-9999")).contains("확인할 수 없습니다");
	}

}
