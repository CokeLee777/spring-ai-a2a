# Amazon Bedrock AgentCore Spring Boot Samples

Amazon Bedrock AgentCore Runtime과 Spring AI를 활용한 A2A(Agent-to-Agent) 프로토콜 기반 멀티 에이전트 오케스트레이션 샘플 프로젝트입니다.

## 개요

이 프로젝트는 Amazon Bedrock AgentCore Runtime 위에서 동작하는 고객 지원 오케스트레이터를 구현합니다. 사용자의 주문/배송 문의를 LLM이 분석하여 적절한 다운스트림 A2A 에이전트를 호출합니다.

## 아키텍처

```
AgentCore Runtime
      │
      ▼ (A2A message/send)
┌─────────────────────────┐
│     a2a-orchestrator    │  ← Spring AI ChatClient (Amazon Nova Pro)
│       (port: 9000)      │     MessageChatMemoryAdvisor (세션 기억)
└──┬──────────┬──────┬────┘
   │          │      │  (A2A Tools)
   ▼          ▼      ▼
Order       Delivery  Payment
Agent       Agent     Agent
(8081)      (8082)    (8083)
```

> 다운스트림 에이전트(Order, Delivery, Payment)는 별도 리포지토리에서 관리됩니다.
> [a2a-spring-boot-samples](https://github.com/CokeLee777/a2a-spring-boot-samples) 참고

## 모듈 구조

| 모듈 | 설명 |
|---|---|
| `a2a-common` | A2A 클라이언트 Transport, TextExtractor 유틸리티 |
| `a2a-spring-boot-autoconfigure` | A2A 서버 인프라 자동 구성 (`AgentExecutor` 빈 감지 시 활성화) |
| `a2a-orchestrator` | Bedrock AgentCore Runtime과 연동하는 오케스트레이터 에이전트 |

## 전제 조건

- Java 21
- AWS 자격증명 (Bedrock 접근 권한 필요)
- Amazon Nova Pro 모델 접근 활성화 (`ap-northeast-2` 기본값)
- 다운스트림 A2A 에이전트(Order, Delivery, Payment) 별도 실행

## 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BEDROCK_REGION` | `ap-northeast-2` | AWS 리전 |
| `BEDROCK_MODEL_ID` | `amazon.nova-pro-v1:0` | Bedrock 모델 ID |
| `ORDER_AGENT_URL` | `http://localhost:8081` | 주문 에이전트 URL |
| `DELIVERY_AGENT_URL` | `http://localhost:8082` | 배송 에이전트 URL |
| `PAYMENT_AGENT_URL` | `http://localhost:8083` | 결제 에이전트 URL |
| `A2A_CLIENT_TIMEOUT_SECONDS` | `15` | 다운스트림 에이전트 호출 타임아웃(초) |
| `CHAT_MEMORY_MAX_MESSAGES` | `20` | 대화 기록 최대 메시지 수 |

## 실행 방법

```bash
# 전체 빌드
./gradlew build

# 오케스트레이터 실행
./gradlew :a2a-orchestrator:bootRun
```

## 주요 기술 스택

- **Java 21**, **Spring Boot 3.5.0**
- **Spring AI 1.1.2** — ChatClient, Tool Calling, MessageChatMemoryAdvisor
- **Amazon Bedrock** (Amazon Nova Pro) — LLM 추론
- **A2A Java SDK 1.0.0.Alpha3** — Agent-to-Agent 프로토콜
- **Amazon Bedrock AgentCore Runtime** — 세션 관리, 에이전트 엔트리포인트
