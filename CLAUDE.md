# CLAUDE.md

## 프로젝트 개요

Amazon Bedrock AgentCore Runtime + Spring AI + A2A 프로토콜을 활용한 멀티 에이전트 오케스트레이션 샘플.
AgentCore Runtime이 세션을 관리하며, host-agent(오케스트레이터)가 Spring AI tool-calling으로 다운스트림 A2A 에이전트를 호출한다.

## 빌드 / 실행 명령어

```bash
# 전체 빌드
./gradlew build

# 각 에이전트 실행
./gradlew :agents:host-agent:bootRun
./gradlew :agents:order-agent:bootRun
./gradlew :agents:delivery-agent:bootRun
./gradlew :agents:payment-agent:bootRun

# 특정 모듈 컴파일 확인
./gradlew :agent-common:compileJava
```

Gradle Kotlin DSL 사용: 루트 `build.gradle.kts`, `settings.gradle.kts`, 서브프로젝트 `build.gradle.kts`.

## 모듈 구조

```
amazon-bedrock-agentcore-spring-ai-a2a-samples/
├── agent-common/                              # 공유 유틸리티
│   ├── A2ATransport                         # A2A 클라이언트 (TaskEvent 수신, 동기 블로킹)
│   ├── LazyAgentCard                        # AgentCard lazy 로딩·캐싱 (시작 실패 시 첫 호출에 재시도)
│   └── util/TextExtractor                   # Task/Message에서 텍스트 추출
├── spring-ai-a2a-server/                    # A2A 서버 구현체 (JSON-RPC, AgentCard, Ping 컨트롤러)
├── spring-ai-a2a-server-autoconfigure/      # A2A 서버·공통 자동 구성
│   ├── A2AServerAutoConfiguration           # AgentExecutor 빈 있을 때만 활성화
│   └── A2ACommonAutoConfiguration           # AgentCard 빈 있을 때만 활성화
├── agents/
│   ├── host-agent/   (port: 8080)           # AgentCore Runtime 진입점 · 오케스트레이터
│   │   ├── InvocationsController            # POST /invocations, 요청마다 동적 시스템 프롬프트 생성
│   │   ├── RemoteAgentConnections           # 다운스트림 에이전트 호출 Tool (@Tool), LazyAgentCard 맵 관리
│   │   └── RemoteAgentProperties            # 다운스트림 에이전트 URL 설정
│   ├── order-agent/  (port: 9001)           # 주문 조회 · 취소 가능 여부 확인 A2A 에이전트
│   │   ├── OrderTools                       # getOrderList, checkOrderCancellability
│   │   ├── DeliveryAgentClient              # delivery-agent 호출 클라이언트 (LazyAgentCard)
│   │   ├── PaymentAgentClient               # payment-agent 호출 클라이언트 (LazyAgentCard)
│   │   └── RemoteAgentProperties            # 다운스트림 에이전트 URL 설정
│   ├── delivery-agent/ (port: 9002)         # 배송 추적 A2A 에이전트
│   │   └── DeliveryTools                    # trackDelivery
│   └── payment-agent/ (port: 9003)          # 결제/환불 상태 확인 A2A 에이전트
│       └── PaymentTools                     # getPaymentStatus
└── integration-tests/                       # 전체 에이전트 통합 테스트 (jar 미생성)
    ├── HostAgentIntegrationTest
    ├── OrderAgentIntegrationTest
    ├── DeliveryAgentIntegrationTest
    └── PaymentAgentIntegrationTest
```

## 핵심 설계 결정 사항

### A2ATransport

- 다운스트림 에이전트 서버는 `emitter.addArtifact()` + `emitter.complete()`로 **Task** 를 반환한다.
  따라서 클라이언트는 반드시 `TaskEvent` 를 수신해야 한다. `MessageEvent` 는 절대 발생하지 않는다.
- `sendMessage()` 는 동기 블로킹 호출이다. `CompletableFuture.supplyAsync()` 로 별도 스레드에서 실행하고
  `.get(timeoutSeconds, TimeUnit.SECONDS)` 로 실제 타임아웃을 강제한다.

### LazyAgentCard (agent-common)

- 생성자에서 즉시 AgentCard fetch를 시도하고, 실패하면 URL을 보관한다.
- `get()` — card가 null이면 매 호출마다 재시도. 실제 에이전트 통신 직전에 사용한다.
- `peek()` — 네트워크 호출 없이 현재 캐시 상태만 반환. 시스템 프롬프트 생성 등 정보 조회에 사용한다.
  (startup 시 `get()` 대신 `peek()`를 쓰지 않으면 에이전트 수 × 조회 횟수만큼 불필요한 WARN이 발생한다.)

### RemoteAgentConnections (host-agent)

- `@Tool` 메서드 `sendMessage(agentName, task)` 하나로 모든 다운스트림 에이전트 호출을 처리한다.
- 각 파라미터에 `@ToolParam(description = "...")` 을 달아 LLM이 올바른 값을 추론하도록 돕는다.
- 내부적으로 `Map<String, LazyAgentCard>` (config key → LazyAgentCard)를 관리한다.
  `sendMessage` 는 card name으로 조회(`get()` 경유), `getAgentDescriptions`/`getAgentNames` 는 `peek()` 경유.
- 로컬 실행 시 헤더가 없으면 UUID로 폴백하여 개발/테스트 편의를 보장한다.

### InvocationsController (host-agent)

- 시스템 프롬프트를 **매 요청마다** `connections.getAgentDescriptions()`로 동적 생성한다.
  빈 초기화 시 고정(static)하면 시작 후 lazy 로드된 에이전트가 프롬프트에 반영되지 않는다.

### Spring AI Tool Calling

- `ChatClient.call()` 은 동기 블로킹 agentic loop다. 스트리밍은 `.stream()` 을 사용해야 한다.
- 툴 실행은 기본적으로 **순차 실행**이다. LLM이 한 번의 응답에 여러 툴을 요청해도 Java 측에서 순차 처리한다.
- 단일 필드 래퍼 record 대신 `@ToolParam`이 달린 단일 파라미터를 사용한다.

### auto-configure 활성화 조건

- `A2AServerAutoConfiguration` — `AgentExecutor` 빈이 컨텍스트에 있을 때만 활성화 (다운스트림 에이전트 서버용).
- `A2ACommonAutoConfiguration` — `AgentCard` 빈이 컨텍스트에 있을 때만 활성화 (Ping + AgentCard 컨트롤러).

## Docker 빌드

모든 Dockerfile의 빌드 컨텍스트는 **프로젝트 루트**다.

```bash
# host-agent (AgentCore 배포 → ARM64 필수)
docker buildx build --platform linux/arm64 \
  -f agents/host-agent/Dockerfile \
  -t host-agent:arm64 --load .

# 다운스트림 에이전트 (amd64)
docker buildx build --platform linux/amd64 \
  -f agents/order-agent/Dockerfile    -t order-agent:latest    --load .
docker buildx build --platform linux/amd64 \
  -f agents/delivery-agent/Dockerfile -t delivery-agent:latest --load .
docker buildx build --platform linux/amd64 \
  -f agents/payment-agent/Dockerfile  -t payment-agent:latest  --load .
```

## 의존성 버전 (루트 build.gradle.kts extra)

- `springAiVersion` — 1.1.2
- `awsSdkVersion` — 2.42.9
- `a2aVersion` — 0.3.3.Final
- `gsonVersion` — 2.13.2

## 코드 컨벤션

- `io.spring.javaformat` 플러그인으로 Spring 공식 포맷을 적용한다.
- 모든 public 타입과 메서드에 Javadoc(`/** */`)을 작성한다.
- 설정값은 환경변수로 오버라이드 가능하도록 `${VAR:default}` 패턴을 사용한다.
- `@Tool` 메서드 파라미터에는 반드시 `@ToolParam(description = "...")` 을 달아 LLM 추론을 돕는다.
