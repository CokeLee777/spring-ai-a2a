package io.github.cokelee777.agent.payment;

import io.github.cokelee777.agent.payment.repository.InMemoryPaymentRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentToolsTest {

	private final PaymentTools tools = new PaymentTools(new InMemoryPaymentRepository());

	@Test
	void getPaymentStatus_ord1001_returnsPaymentInfo() {
		assertThat(tools.getPaymentStatus("ORD-1001")).contains("1,500,000원").contains("결제완료");
	}

	@Test
	void getPaymentStatus_ord1002_returnsPaymentInfo() {
		assertThat(tools.getPaymentStatus("ORD-1002")).contains("45,000원").contains("결제완료");
	}

	@Test
	void getPaymentStatus_ord1003_returnsPaymentInfo() {
		assertThat(tools.getPaymentStatus("ORD-1003")).contains("120,000원").contains("결제완료");
	}

	@Test
	void getPaymentStatus_ord1004_returnsAwaitingPayment() {
		assertThat(tools.getPaymentStatus("ORD-1004")).contains("결제대기").contains("329,000원");
	}

	@Test
	void getPaymentStatus_ord1005_returnsAuthorizationHold() {
		assertThat(tools.getPaymentStatus("ORD-1005")).contains("승인보류");
	}

	@Test
	void getPaymentStatus_ord1006_returnsPaymentFailed() {
		assertThat(tools.getPaymentStatus("ORD-1006")).contains("결제실패");
	}

	@Test
	void getPaymentStatus_ord1008_returnsPartialRefund() {
		assertThat(tools.getPaymentStatus("ORD-1008")).contains("부분환불").contains("55,000");
	}

	@Test
	void getPaymentStatus_ord1009_returnsFullRefund() {
		assertThat(tools.getPaymentStatus("ORD-1009")).contains("환불완료").contains("35,000원");
	}

	@Test
	void getPaymentStatus_ord1010_returnsVoided() {
		assertThat(tools.getPaymentStatus("ORD-1010")).contains("결제취소");
	}

	@Test
	void getPaymentStatus_unknown_returnsNotFound() {
		assertThat(tools.getPaymentStatus("ORD-9999")).contains("찾을 수 없습니다");
	}

}
