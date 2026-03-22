# Memory Module Restructure Design

**Date:** 2026-03-22
**Status:** Approved

## Overview

`host-agent` 내부에 있던 Bedrock AgentCore 메모리 관련 코드를 독립 Gradle 모듈로 분리한다.
Spring AI의 `ChatMemoryRepository` 인터페이스를 채택하고, 장기 메모리 패턴을 제거하며,
`memory/` 디렉토리 하위에 Spring AI 네이밍 컨벤션을 따르는 모듈 두 개로 관리한다.

---

## Decisions

| 결정 | 내용 |
|------|------|
| 단기 메모리 인터페이스 | 커스텀 `ShortTermMemoryService` 제거 → Spring AI `ChatMemoryRepository` 채택 |
| 장기 메모리 | `LongTermMemoryService` 및 관련 구현체 제거. 추후 Spring AI `VectorStore` 기반 RAG로 대체 |
| NoOp 대체 | `NoOpShortTermMemoryService` 제거 → `InMemoryChatMemoryRepository` 사용 |
| `MemoryMode` | `LONG_TERM`, `BOTH` 제거로 단순화 여지가 있으나, `@ConditionalOnMissingBean` 패턴 채택으로 완전 제거 |
| 조건 처리 | 커스텀 `*Condition` 클래스 제거 → `@ConditionalOnMissingBean(ChatMemoryRepository.class)` |
| 모듈 위치 | `memory/` 디렉토리 하위, Spring AI 네이밍 컨벤션 통일 |

---

## New Module Structure

```
memory/
├── spring-ai-a2a-model-chat-memory-in-memory/
│   └── InMemoryChatMemoryConfiguration          ← @ConditionalOnMissingBean
└── spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core/
    ├── BedrockChatMemoryRepository               ← implements ChatMemoryRepository
    ├── BedrockChatMemoryProperties               ← @ConfigurationProperties
    └── BedrockChatMemoryConfiguration            ← @AutoConfiguration
```

### spring-ai-a2a-model-chat-memory-in-memory

Spring AI의 `ChatMemoryAutoConfiguration`과 동일한 패턴.
`ChatMemoryRepository` 빈이 없을 때만 `InMemoryChatMemoryRepository`를 등록한다.

```java
@AutoConfiguration
public class InMemoryChatMemoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }
}
```

### spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core

`BedrockChatMemoryRepository`가 `ChatMemoryRepository`를 구현한다.
`@AutoConfiguration`으로 등록되어 classpath에 있으면 자동으로 Bedrock 구현체가 우선된다.

```java
// BedrockChatMemoryRepository
public class BedrockChatMemoryRepository implements ChatMemoryRepository {
    // saveAll(conversationId, messages)
    // findByConversationId(conversationId) → List<Message>
    // deleteByConversationId(conversationId)
    // 내부적으로 BedrockAgentCoreClient.createEvent() / listEvents() 사용
}

// BedrockChatMemoryProperties
@ConfigurationProperties(prefix = "aws.bedrock.agent-core.memory")
public record BedrockChatMemoryProperties(
    @NotNull String memoryId,
    @Min(1) int maxTurns   // 기존 shortTermMaxTurns
) {}

// BedrockChatMemoryConfiguration
@AutoConfiguration
@EnableConfigurationProperties(BedrockChatMemoryProperties.class)
public class BedrockChatMemoryConfiguration {
    @Bean
    public BedrockChatMemoryRepository chatMemoryRepository(...) { ... }
}
```

---

## host-agent Changes

### 제거 대상

| 파일 | 이유 |
|------|------|
| `memory/ShortTermMemoryService` | `ChatMemoryRepository`로 대체 |
| `memory/LongTermMemoryService` | 장기 메모리 패턴 제거 |
| `memory/NoOpShortTermMemoryService` | `InMemoryChatMemoryRepository`로 대체 |
| `memory/NoOpLongTermMemoryService` | 장기 메모리 패턴 제거 |
| `memory/MemoryMode` | `@ConditionalOnMissingBean` 패턴으로 불필요 |
| `memory/ConversationSession` | `actorId`는 장기 메모리용이었으므로 제거. `sessionId`(String)만 직접 사용 |
| `memory/bedrock/BedrockLongTermMemoryService` | 장기 메모리 패턴 제거 |
| `memory/bedrock/AgentCoreEventToMessageConverter` | 새 모듈로 이동 |
| `memory/bedrock/BedrockMemoryProperties` | `BedrockChatMemoryProperties`로 대체 (새 모듈) |
| `memory/bedrock/BedrockShortTermMemoryService` | `BedrockChatMemoryRepository`로 대체 (새 모듈) |
| `config/BedrockMemoryConfiguration` | `BedrockChatMemoryConfiguration`으로 대체 (새 모듈) |
| `config/NoOpMemoryConfiguration` | `InMemoryChatMemoryConfiguration`으로 대체 (새 모듈) |
| `config/MemoryEnabledCondition` | `@ConditionalOnMissingBean`으로 불필요 |
| `config/MemoryDisabledCondition` | `@ConditionalOnMissingBean`으로 불필요 |
| `config/LongTermMemoryCondition` | 장기 메모리 패턴 제거 |
| `config/LongTermNotSupportedCondition` | 장기 메모리 패턴 제거 |

### DefaultInvocationService 변경

- `ShortTermMemoryService`, `LongTermMemoryService`, `MemoryMode` 의존성 제거
- `ChatMemoryRepository` 주입
- 항상 히스토리 로드 및 저장 (NoOp 분기 없음)
- `ConversationSession` 제거 → `sessionId` (String) 직접 사용

```java
// 변경 전
List<Message> history = memoryMode.supportsShortTerm()
    ? shortTermMemoryService.loadHistory(session) : Collections.emptyList();

// 변경 후
List<Message> history = chatMemoryRepository.findByConversationId(sessionId);
```

```java
// 변경 전
shortTermMemoryService.appendUserTurn(session, prompt);
shortTermMemoryService.appendAssistantTurn(session, content);

// 변경 후
List<Message> updated = new ArrayList<>(history);
updated.add(new UserMessage(prompt));
updated.add(new AssistantMessage(content));
chatMemoryRepository.saveAll(sessionId, updated);
```

### host-agent build.gradle.kts 변경

```kotlin
// 변경 전
implementation("software.amazon.awssdk:bedrockagentcore")
implementation("software.amazon.awssdk:bedrockagentcorecontrol")

// 변경 후 (Bedrock 사용 시)
implementation(project(":spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core"))
// 또는 (로컬 개발 시)
implementation(project(":spring-ai-a2a-model-chat-memory-in-memory"))
```

---

## settings.gradle.kts 변경

```kotlin
include("spring-ai-a2a-model-chat-memory-in-memory")
include("spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core")

project(":spring-ai-a2a-model-chat-memory-in-memory")
    .projectDir = file("memory/spring-ai-a2a-model-chat-memory-in-memory")
project(":spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core")
    .projectDir = file("memory/spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core")
```

---

## Data Flow

```
InvocationsController
    → DefaultInvocationService
        → ChatMemoryRepository.findByConversationId(sessionId)   // history load
        → ChatClient.prompt().messages(history).user(prompt)     // LLM call
        → ChatMemoryRepository.saveAll(sessionId, updated)       // persist
```

로컬 환경: `InMemoryChatMemoryRepository` (메모리 내 임시 저장, 재시작 시 초기화)
운영 환경: `BedrockChatMemoryRepository` (Bedrock AgentCore Memory API)

---

## Testing

- `BedrockChatMemoryRepository`에 대한 단위 테스트 (`BedrockAgentCoreClient` mocking)
- `InMemoryChatMemoryConfiguration` autoconfigure 테스트 (`@ConditionalOnMissingBean` 동작 검증)
- `BedrockChatMemoryConfiguration` autoconfigure 테스트 (Bedrock 빈이 InMemory를 덮어쓰는지 검증)
- `DefaultInvocationService` 단위 테스트 (`ChatMemoryRepository` mocking)
- 기존 `BedrockShortTermMemoryServiceTest`, `BedrockLongTermMemoryServiceTest` 제거
