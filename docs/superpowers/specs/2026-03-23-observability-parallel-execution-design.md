# Observability + Parallel Tool Execution 설계

**날짜**: 2026-03-23
**브랜치**: feat/observability-parallel-execution
**상태**: 승인됨

---

## 목표

1. OpenTelemetry 기반 분산 추적 — 멀티에이전트 호출 체인 전체를 하나의 trace-id로 묶어 로그에서 가시화
2. 병렬 Tool Execution — host-agent가 다운스트림 에이전트를 동시에 호출해 레이턴시 감소

---

## 아키텍처 개요

### 현재

```
AgentCore → host-agent → order-agent    (순차)
                       → delivery-agent (순차)
                       → payment-agent  (순차)
→ 추적 없음, 병목 지점 불투명
```

### 목표

```
AgentCore → host-agent ──┬→ order-agent    (동시)
      [trace: abc123]     ├→ delivery-agent (동시)
                          └→ payment-agent  (동시)
→ 전체 체인이 하나의 trace-id로 로그에 출력
→ 각 에이전트 span 시간 개별 측정
```

---

## 선결 과제: A2ATransport 빈 전환 (Step 0)

현재 `A2ATransport`는 static 유틸리티 클래스다. `ObservationRegistry` 주입을 위해
Spring 관리 빈으로 전환이 선행되어야 한다.

### 변경 범위

| 대상 | 변경 내용 |
|------|----------|
| `A2ATransport` | `@Component` 빈 전환, `ObservationRegistry` 생성자 주입 |
| `RemoteAgentConnections` (host-agent) | `A2ATransport` 빈 생성자 주입으로 교체 |
| `DeliveryAgentClient` (order-agent) | `A2ATransport` 빈 생성자 주입으로 교체 |
| `PaymentAgentClient` (order-agent) | `A2ATransport` 빈 생성자 주입으로 교체 |
| `A2ATransportTest` | static 호출 방식 → 빈 기반 테스트로 리팩터링 |

---

## 모듈 구조

### 신규 모듈

```
auto-configurations/observability/
└── spring-ai-a2a-autoconfigure-observability/
    └── A2AObservabilityAutoConfiguration
        ├── @ConditionalOnClass(ObservationRegistry.class)
        ├── @ConditionalOnProperty(prefix="spring.ai.a2a.observability", name="enabled", matchIfMissing=true)
        ├── @AutoConfiguration(after = {MicrometerAutoConfiguration.class, ObservationAutoConfiguration.class})
        ├── ObservationRegistry 빈 (@ConditionalOnMissingBean) — ObservationAutoConfiguration 없는 환경 fallback
        └── OtlpMeterRegistry 빈 (@ConditionalOnProperty("OTEL_EXPORTER_OTLP_ENDPOINT")) — 사용자가 opentelemetry-exporter-otlp를 직접 추가해야 활성화됨
```

`spring/autoconfigure/auto-configuration.imports`에 등록 필요.

### 기존 모듈 변경

| 모듈 | 변경 내용 |
|------|----------|
| `spring-ai-a2a-agent-common` | `A2ATransport` 빈 전환 + `send()` Observation 수동 wrap |
| `spring-ai-a2a-server` | `DefaultAgentExecutor`에 `ObservationRegistry` 생성자 주입, `micrometer-observation` 의존성 추가 |
| 각 sample 모듈 | `micrometer-tracing-bridge-otel` 의존성 추가, `A2ATransport` 빈 주입 방식 변경 |

> `DefaultAgentExecutor`에 `ObservationRegistry` 주입을 위해 `spring-ai-a2a-server`의
> 생성자 시그니처가 변경된다. 라이브러리 사용자 영향 범위 검토 필요.

---

## 계측 지점

| 위치 | 계측 방법 | Span 이름 |
|------|----------|-----------|
| `InvocationsController.invoke()` | Spring MVC 자동 계측 | `POST /invocations` |
| `A2ATransport.send()` | `Observation` 수동 wrap | `a2a.agent.send` (agentName 태그 포함) |
| `DefaultAgentExecutor.execute()` | `Observation` 수동 wrap | `a2a.agent.execute` |

> `@Observed`는 사용하지 않는다. `DefaultAgentExecutor.execute()`는 A2A SDK가 직접 호출하므로
> Spring AOP 프록시를 통하지 않아 `@Observed`가 동작하지 않는다.

---

## Trace 전파 흐름

```
host-agent (span: POST /invocations)
  └─ A2ATransport.send("order-agent")
       → HTTP 요청에 traceparent 헤더 수동 주입
       → order-agent (span: a2a.agent.send, a2a.agent.execute)
            └─ A2ATransport.send("delivery-agent")
                 → delivery-agent (span: ...)
```

**[TBD — 구현 전 확인 필요]** `A2ATransport`는 매 호출마다 A2A SDK `Client.builder().build()`로
새 클라이언트를 생성하고, 내부 transport는 `JSONRPCTransportConfig`를 사용한다.
`JSONRPCTransportConfig`에 커스텀 HTTP 헤더 주입 API가 존재하는지 A2A SDK 0.3.3.Final
소스에서 확인해야 한다. API가 없을 경우 다음 대안 중 선택:
- `JSONRPCTransportConfig` 서브클래싱
- SDK가 제공하는 인터셉터/데코레이터 패턴 사용

Spring `RestClient`가 아닌 `JdkA2AHttpClient`(`java.net.http.HttpClient`)를 사용하므로
Micrometer RestClient 자동 계측은 적용되지 않는다.

---

## 병렬 Tool Execution

### 한계: Spring AI 순차 실행

CLAUDE.md에 명시된 대로, Spring AI Tool Calling은 기본적으로 **순차 실행**이다.
`parallelToolCalls(true)`는 LLM에게 병렬 tool call을 허용하는 힌트일 뿐, Java 측 실행을 바꾸지 않는다.
`BedrockConverseOptions`의 지원 여부도 구현 전 확인이 필요하다.

### 전략: CompletableFuture 기반 병렬 디스패치

Spring AI tool call 실행 루프를 병렬 디스패치로 대체:

```
LLM 응답 (여러 tool call 포함)
  └─ ToolCallParallelExecutor (신규)
       ├─ CompletableFuture.supplyAsync(() → order-agent 호출, a2aTaskExecutor)
       ├─ CompletableFuture.supplyAsync(() → delivery-agent 호출, a2aTaskExecutor)
       └─ CompletableFuture.allOf(...).join()
```

- 기존 `a2aTaskExecutor` (virtual thread 기반) 재사용
- `A2ATransport.send()` blocking 코드 변경 없음 (virtual thread에서 안전)

**[TBD — 구현 전 확인 필요]** Spring AI 1.1.3의 `ChatClient` / `DefaultChatClient` 내부 tool call 루프의
공개 확장 지점(`ToolCallingManager`, `ToolCallResultConverter` 등)이 어느 수준으로 제공되는지 확인 필요.
확장 지점이 제한적일 경우 `ChatClient` wrapping 방식으로 우회 전략 수립.

### 구현 범위 조정

Observability와 독립적으로 구현 가능하므로 **별도 단계**로 진행.
구현 플랜 단계에서 Spring AI 확장 지점 조사 후 범위 확정.

---

## 에러 처리

| 상황 | 처리 방식 |
|------|----------|
| OTLP 백엔드 연결 불가 | Exporter가 자동 drop — 앱 동작에 영향 없음 |
| 다운스트림 에이전트 타임아웃 | span에 `error=true` + 예외 메시지 기록, `A2ATransport` 기존 에러 문자열 반환 동작 유지 |
| trace 헤더 없는 요청 | 새 root span 자동 생성 — 기존 동작 유지 |

> Observability는 비기능 요소 — 실패해도 비즈니스 로직에 절대 영향 없음

---

## 로그 출력 예시

```
[host-agent]     [traceId=abc123 spanId=def456] POST /invocations
[order-agent]    [traceId=abc123 spanId=ghi789] a2a.agent.execute  ← 동일 trace-id
[delivery-agent] [traceId=abc123 spanId=jkl012] a2a.agent.execute  ← 동일 trace-id
```

OTLP exporter 활성화: `OTEL_EXPORTER_OTLP_ENDPOINT` 환경변수 설정 +
사용자가 `opentelemetry-exporter-otlp` 의존성 직접 추가 필요 (라이브러리에서는 `compileOnly` 처리).

---

## 테스트 전략

| 종류 | 내용 |
|------|------|
| 단위 테스트 | `A2ATransport` — `ObservationRegistry` mock으로 span 생성 및 `traceparent` 헤더 주입 검증 |
| 통합 테스트 | `HostAgentIntegrationTest` 확장 — 다운스트림 요청에 `traceparent` 헤더가 포함되는지 검증 |
| 병렬 실행 검증 | 병렬 실행 구현 완료 후 — 실행 시간이 순차 합산보다 짧은지 확인 |

---

## 의존성 추가

```kotlin
// spring-ai-a2a-autoconfigure-observability/build.gradle.kts
implementation("io.micrometer:micrometer-tracing-bridge-otel")
compileOnly("io.opentelemetry:opentelemetry-exporter-otlp")  // 사용자 직접 추가 필요

// spring-ai-a2a-agent-common/build.gradle.kts
implementation("io.micrometer:micrometer-observation")  // A2ATransport 빈 전환 후 필요

// spring-ai-a2a-server/build.gradle.kts
implementation("io.micrometer:micrometer-observation")

// 각 에이전트 sample build.gradle.kts
implementation("io.micrometer:micrometer-tracing-bridge-otel")
```

---

## 구현 순서

### Phase 1: Observability

1. **[선결]** `A2ATransport` static → Spring 빈 전환 + 호출부 3곳 변경 + 테스트 리팩터링
2. `spring-ai-a2a-autoconfigure-observability` 모듈 생성 (`auto-configurations/observability/` 하위, `settings.gradle.kts` include 추가 필요)
3. **[TBD 확인]** A2A SDK `JSONRPCTransportConfig` 헤더 주입 API 조사 → `traceparent` 주입 방식 확정
4. `A2ATransport.send()` Observation 수동 wrap + `traceparent` 헤더 주입 구현
5. `DefaultAgentExecutor.execute()` Observation 수동 wrap
6. 각 sample 모듈 `micrometer-tracing-bridge-otel` 의존성 추가
7. 단위 테스트 + 통합 테스트 작성

### Phase 2: 병렬 실행 (Phase 1 완료 후)

8. **[TBD 확인]** Spring AI 1.1.3 tool call 확장 지점 조사
9. `ToolCallParallelExecutor` 구현 + `a2aTaskExecutor` 연동
10. `parallelToolCalls` LLM 옵션 설정 (BedrockConverseOptions 지원 여부 확인 후)
11. 병렬 실행 검증 테스트
