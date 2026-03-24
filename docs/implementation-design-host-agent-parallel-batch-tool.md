# host-agent 병렬 배치 툴 — 구현 설계

## 0. 관련 문서

| 문서 | 역할 |
|------|------|
| [design-host-agent-parallel-batch-tool.md](./design-host-agent-parallel-batch-tool.md) | 문제 정의, 목표, API·동시성·프롬프트의 개념 설계 |

본 문서는 **파일 단위 변경**, **시그니처**, **알고리즘 단계**, **테스트 케이스**, **리스크**를 구현자 기준으로 고정한다.

---

## 1. 코딩 규칙

### 1.1 Javadoc 언어

- 모든 `/** ... */` 블록은 **영어**로 작성한다.
- 적용 대상: 새로 추가하는 **public 타입**, **public 메서드**, **public record 컴포넌트**에 대한 요약·`@param`·`@return`·`@throws`(해당 시).
- **비적용**: `@Tool(description = "...")`, `@ToolParam(description = "...")` 문자열은 **모델이 읽는 프롬프트 스키마**이므로 기존 `sendMessage`와 같이 **한국어 유지**(제품 일관성). 필요 시 팀 정책으로 영문 전환 가능하나, 본 구현에서는 한국어로 명시한다.

### 1.2 기타

- `io.spring.javaformat` 및 기존 host-agent 스타일(탭 인덴트 등)에 맞춘다.
- 새 public 타입은 **한 파일 한 최상위 타입** 원칙을 따른다(필요 시 `AgentDelegationRequest.java` 분리).

### 1.3 `@ToolParam` 배치 (Spring AI 스키마)

- **메서드 파라미터** `List<AgentDelegationRequest> items`에 `@ToolParam`을 달아 **목록 전체** 의미를 설명한다(독립 위임 배열, 최대 개수 등).
- **`AgentDelegationRequest` 레코드 컴포넌트**(`agentName`, `task`)에도 각각 `@ToolParam(description = "...")`를 단다. Spring AI는 JSON Schema 생성 시 **중첩 필드 설명**에 `@ToolParam`, Jackson `@JsonPropertyDescription`, Swagger `@Schema` 등을 재귀적으로 반영한다([Tool parameters](https://docs.spring.io/spring-ai/reference/api/tools)).
- 따라서 **목록 파라미터만** 설명하는 것으로는 부족하고, **필드별 설명을 반드시 둔다**.

---

## 2. 변경 범위 요약

| 구분 | 경로 | 변경 |
|------|------|------|
| 도메인/툴 | `samples/host-agent/.../RemoteAgentConnections.java` | record(또는 별도 파일), 상수, executor, `sendMessagesParallel` |
| 프롬프트 | `samples/host-agent/.../invocation/DefaultInvocationService.java` | `ROUTING_SYSTEM_PROMPT` 문단 추가 |
| 테스트 | `samples/host-agent/src/test/java/.../RemoteAgentConnectionsTest.java` | **신규** 클래스 권장 |
| 의존성 | `samples/host-agent/build.gradle.kts` | 변경 없음(기존 JDK 25 + Spring AI) |

다른 모듈(`spring-ai-a2a-agent-common`의 `A2ATransport` 등)은 **변경하지 않는다**.

---

## 3. 타입 설계

### 3.1 `AgentDelegationRequest` (record)

**위치**: `io.github.cokelee777.agent.host` 패키지, 파일 `AgentDelegationRequest.java` 또는 `RemoteAgentConnections.java`와 동일 패키지의 전용 파일.

**필드**:

| 필드 | 타입 | 의미 |
|------|------|------|
| `agentName` | `String` | 다운스트림 에이전트 식별자 (`sendMessage(AgentDelegationRequest)`가 사용하는 것과 동일 규칙) |
| `task` | `String` | 전달할 작업 설명·컨텍스트 |

**Javadoc (영문) + `@ToolParam` (한국어, 필드별 스키마 설명)**:

```java
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Request to delegate work to one downstream agent as part of {@link RemoteAgentConnections#sendMessagesParallel}.
 *
 * @param agentName name of the downstream agent (same semantics as {@link RemoteAgentConnections#sendMessage})
 * @param task task description and context to send to that agent
 */
public record AgentDelegationRequest(
		@ToolParam(description = "작업을 위임할 에이전트의 이름") String agentName,
		@ToolParam(description = "에이전트에게 전달할 포괄적인 작업 설명 및 컨텍스트") String task) {
}
```

`sendMessage`의 `@ToolParam` 문안과 **동일한 한국어**를 재사용해 라우팅 도구 간 일관성을 맞춘다.

### 3.2 입력 null·빈 문자열

- `items == null`: 툴 메서드 첫 줄에서 방어적으로 **오류 메시지 문자열 반환**(예외 대신 문자열로 LLM에 피드백).
- `items.isEmpty()`: “No delegation items” 성격의 **영문** 메시지 반환 권장(모델이 이해하기 쉬움).
- 각 항목의 `agentName` / `task`가 null 또는 blank: 해당 슬롯만 에러 블록으로 집계(다른 항목은 계속). 구체 문구는 구현자가 영문으로 통일.

---

## 4. `RemoteAgentConnections` 구현 상세

### 4.1 배치 크기

- **샘플 코드**에서는 한 호출당 위임 개수 **상한을 두지 않는다**. 운영 배포 시에는 동시 호출·다운스트림 부하에 맞게 상한·레이트 리밋을 별도로 두는 것을 권장한다.

### 4.2 Executor

- **필드**: `private static final Executor VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();`
- **이유**: 툴 호출마다 executor를 만들지 않음. 애플리케이션 수명 동안 닫지 않음(Boot 일반 패턴).
- **Javadoc**: 클래스 레벨에 한 줄로 “used for parallel downstream calls in {@link #sendMessagesParallel}” 등 **영문** 기술.

### 4.3 메서드: `sendMessagesParallel`

**시그니처 (권장)**:

```java
@Tool(description = "...한국어(아래 Tool 문안)...")
public String sendMessagesParallel(
		@ToolParam(description = "...한국어(아래 items 문안)...") List<AgentDelegationRequest> items)
```

`AgentDelegationRequest` 정의는 §3.1처럼 **컴포넌트마다 `@ToolParam`** 을 둔다(메서드의 `items`에만 두지 않음).

**`@Tool` / `@ToolParam` 한국어 문안(구현 시 사용)**:

- **Tool**: 서로 **독립적인** 여러 원격 에이전트에 동시에 위임할 때 사용. 한 에이전트의 결과가 다른 에이전트 입력에 필요하면 사용하지 말고 `sendMessage`를 순서대로 호출할 것.
- **`items` 파라미터**: 독립 위임 항목의 배열. 각 원소는 `agentName`과 `task`를 포함하며, 필드 의미는 해당 레코드 컴포넌트의 `@ToolParam`을 따른다.
- **`AgentDelegationRequest.agentName` / `.task`**: §3.1 예시와 동일( `sendMessage` 파라미터 설명 재사용 권장).

**알고리즘 (순서 고정)**:

1. `items == null` / 원소 `null` 등: 구현에 따라 `Assert` 또는 영문 오류 문자열(샘플은 `Assert` 사용 가능).
2. `items.isEmpty()`: 빈 문자열 등으로 집계(구현 일관성 유지).
3. 인덱스 `i = 0 .. n-1`에 대해 `CompletableFuture.supplyAsync`로 슬롯별 실행(예: `sendMessage` 위임).
4. `CompletableFuture.allOf(...).join()`으로 전체 완료 대기.
5. 인덱스 순으로 결과를 **최종 문자열**로 연결.

**`runOneDelegation` 책임**:

- 리스트 원소가 `null`이면 슬롯 전용 영문 에러(`Invalid delegation entry: null item.`).
- 그 외에는 **`sendMessage(AgentDelegationRequest)`** 만 호출한다. `agentName`/`task` 누락·공백·미등록 에이전트 등은 전부 `sendMessage`에서 처리한다.

**출력 포맷 (최종 집계)** — 개념 설계와 동일, 구현 시 리터럴 일치 권장:

```text
[1] agent: <resolved-or-requested-name>
response:
<body>

[2] agent: ...
```

- 에이전트를 찾지 못한 경우 `<resolved-or-requested-name>`에는 요청한 `agentName`을 넣는다.

**로깅**:

- `INFO`: 배치 크기, 에이전트 이름 목록(중복 허용).
- `task` 전문은 **기본적으로 로그에 넣지 않음**(DEBUG에서만 선택).

**관측**: Micrometer 등 추가하지 않음(1차 범위 밖). 필요 시 후속 PR.

### 4.4 기존 `sendMessage`

- 시그니처·동작 유지.
- `findByName`이 `private`이면 `sendMessagesParallel`에서 재사용; **공통 private 메서드**로 한 번만 두는 리팩터는 선택(동작 동일하면 작은 추출 허용).

---

## 5. `DefaultInvocationService` 변경

### 5.1 `ROUTING_SYSTEM_PROMPT`

다음 **한국어 불릿**을 “작업 위임” 근처에 삽입(기존 `sendMessage` 언급과 함께):

- **독립 병렬 위임:** 서로 의존하지 않는 여러 에이전트 조회·위임이 필요하면 `sendMessagesParallel`로 **한 번에** 제출한다.
- **순차 위임:** 한 에이전트의 응답이 다른 에이전트 호출에 필요하면 `sendMessage`만 사용하고, **여러 라운드**에 나누어 호출한다.

기존 지침과 충돌 시 **병렬 툴은 “독립 작업”에만** 쓴다는 점을 우선한다.

### 5.2 Javadoc

- `DefaultInvocationService` 클래스/메서드에 이미 영문 Javadoc이 있으면 유지.
- 실행 순서 `<ol>`에 “LLM may call `sendMessage` or `sendMessagesParallel`” 한 줄 추가는 **선택**(영문).

---

## 6. Spring AI 스키마 리스크

- `List<AgentDelegationRequest>`이 Bedrock Converse + 현재 스타터에서 도구 스키마로 잘 전달되는지 **구현 직후 수동 검증**이 필요하다.
- 만약 런타임에서 스키마/역직렬화 문제가 있으면 **대안**(개념 설계 5.2절): `String jsonItems` 단일 파라미터 + Gson 배열 파싱. 본 구현 설계의 **1차 시도는 List + record**로 고정하고, 이스케이프 해치는 문서에 “Plan B”로 한 단락 남긴다.

---

## 7. 테스트 설계 (`RemoteAgentConnectionsTest`)

**프레임워크**: JUnit 5 + Mockito(기존 host-agent 테스트와 동일).

**주의**: `A2ATransport.send`는 static이므로, **순수 단위 테스트**는 다음 중 하나로 진행한다.

- **권장**: `sendMessagesParallel` 로직을 테스트하기 위해 `A2ATransport`를 래핑하는 **작은 인터페이스**(예: `DownstreamAgentCaller`)를 도입하고 생산 코드에만 주입 — 범위가 커지므로 **1차 구현에서는 선택**.
- **실용적 1차**: 통합에 가까운 테스트는 보류하고, **검증 가능한 분기만** 테스트:
  - `requests == null` → 구현에 따라 `IllegalArgumentException` 또는 안내 문자열.
  - `requests`가 빈 리스트 → 빈 집계 문자열 등 구현 정의에 따름.
  - Lazy 카드가 없을 때: **테스트용 `RemoteAgentProperties`**로 더미 URL 등록 등.

**필수 최소 케이스 (구현 설계 고정)**:

| ID | 시나리오 | 기대 |
|----|-----------|------|
| T1 | `sendMessagesParallel(null)` | NPE 없음; `Assert` 사용 시 `IllegalArgumentException` 등 |
| T2 | `sendMessagesParallel(List.of())` | 빈 집계(또는 안내 문구, 구현과 일치) |

**T4+ (후속)**: Testcontainers 또는 WireMock으로 실제 JSON-RPC는 생략하고, 리팩터 후 `DownstreamAgentCaller` mock으로 병렬·순서 검증.

---

## 8. 완료 기준 (Definition of Done)

- [ ] `sendMessagesParallel` 구현 및 영문 Javadoc(타입·메서드); `@ToolParam`은 목록 + `AgentDelegationRequest` 각 컴포넌트.
- [ ] `ROUTING_SYSTEM_PROMPT`에 병렬/순차 문구 반영.
- [ ] T1–T2 단위 테스트 통과(구현과 일치).
- [ ] `./gradlew :host-agent:check`(또는 `test`) 성공.
- [ ] 수동: 로컬 프로파일에서 단일 사용자 질의로 병렬 툴 호출 유도(선택).

---

## 9. 롤백 및 플래그

- 기능 플래그는 **도입하지 않음**. 롤백은 Git revert.
- 프롬프트만 되돌리면 모델이 `sendMessage`만 쓰는 이전 행동으로 수렴 가능.

---

## 10. 구현 체크리스트 (파일 단위)

- [ ] `AgentDelegationRequest.java`: 레코드 컴포넌트마다 `@ToolParam`(한국어), 영문 Javadoc
- [ ] `RemoteAgentConnections.java`: executor, `sendMessagesParallel`, private 헬퍼
- [ ] `DefaultInvocationService.java`: 프롬프트만
- [ ] `RemoteAgentConnectionsTest.java`: T1–T2
- [ ] 개념 설계 문서 체크리스트와 본 문서 DoD 동기화
