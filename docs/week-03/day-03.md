# **3일차. LLM 컨텍스트 관리와 멀티모달**

### **이번에 배울 것**

<aside>
❗

이번 학습에서는 휘발성 및 비휘발성 메모리의 구조적 차이를 이해하고, 대화 요약 기법과 데이터베이스 기반의 영속성 관리를 실전 코드로 구현합니다.

또한, Claude Vision API를 활용한 멀티모달 기능을 통해 이미지 분석, 시각적 추론을 서비스에 녹여내는 방법을 배웁니다. 이를 통해 대화 히스토리를 정교하게 제어하고 다양한 형태의 데이터를 처리할 수 있는, 완성도 높은 AI 애플리케이션 구축 역량을 확보하게 됩니다.

</aside>

# **Part 1: LLM 컨텍스트 관리**

## **1. LLM 컨텍스트 관리하기**

LLM은 자체적으로 과거를 기억하지 못하는 Stateless(상태 없음) 특성을 가집니다. 따라서 사용자에게 끊김 없는 경험을 제공하기 위해, 데이터를 계층별로 관리하여 최적의 컨텍스트를 구성하는 전략이 필수적입니다.

효율적인 컨텍스트 구성을 위해 메모리를 속도, 영속성, 용량에 따라 세 가지 레이어로 분리합니다.

#### Layer 1: Volatile Memory (휘발성 메모리)

- **저장소:** In-Memory (Redis, ConcurrentHashMap, Caffeine Cache)
- **역할:** 현재 진행 중인 '활성 세션'의 실시간 대화 보존.
- **특징:**
    - **초고속 접근:** 1ms 미만의 지연 시간으로 즉각적인 프롬프트 구성 가능.
    - **휘발성:** 속도를 위해 영속성을 포기하며, 보통 TTL(Time-To-Live)을 설정해 관리.
- **구현 예시 (Spring Context):**

    ```java
    // 세션별 최근 대화 5~10개를 캐싱하여 DB 부하 감소
    @Cacheable(value = "activeContext", key = "#sessionId")
    public List<Message> getRecentContext(String sessionId) { ... }
    ```

#### Layer 2: Non-volatile Memory (비휘발성 메모리)

- **저장소:** RDBMS (PostgreSQL, MySQL, Oracle)
- **역할:** 서비스 전체 대화 이력의 영구 저장 및 관리.
- **특징:**
    - **비휘발성:** 서버 재시작이나 세션 만료 후에도 사용자 맥락 복원 가능.
    - **구조화 데이터:** 토큰 사용량, 사용자 ID, 응답 시간 등 메타데이터를 함께 저장하여 분석 가능.
- **비교 사례:**
    - **In-Memory 전용:** 서버 점검 후 AI가 "장바구니가 비어있다"고 답함 (맥락 단절).
    - **L2 포함:** 서버 점검 후에도 "아까 말씀하신 청바지 결제할까요?"라고 응답 (맥락 유지).

    ```
    **시나리오: 전자상거래 쇼핑 어시스턴트**
    
    **In-Memory만 사용:**
    사용자: "이 청바지 장바구니에 담아줘"
    AI: "담았습니다" ✅
    
    **[30분 후, 서버 재시작]**
    
    사용자: "아까 담은 상품 주문할게"
    AI: "죄송합니다. 장바구니가 비어있습니다" ❌
    
    **Database 사용:**
    사용자: "이 청바지 장바구니에 담아줘"
    AI: "담았습니다" ✅
    → DB에 영구 저장 💾
    
    **[30분 후, 서버 재시작]**
    
    사용자: "아까 담은 상품 주문할게"
    AI: "청바지 1개가 장바구니에 있습니다" ✅
    → DB에서 복원
    ```

#### Layer 3: Semantic Memory (의미적 검색 메모리)

- **저장소:** Vector Database (Pinecone, Milvus, pgvector)
- **역할:** 수만 개의 과거 대화 중 '현재 질문과 의미적으로 유사한' 내용만 추출.
- **특징:**
    - **RAG(Retrieval-Augmented Generation) 기반:** 모든 이력을 프롬프트에 넣을 수 없을 때, 핵심 요약본이나 관련 지식만 선별해서 제공.
    - **초대용량 대응:** PB 단위의 데이터에서도 유사도 검색 가능.

| **전략**           | **주요 저장소** | **접근 속도**     | **영속성**    | **핵심 활용처**                   |
|------------------|------------|---------------|------------|------------------------------|
| **In-Memory**    | Redis, RAM | **최상 (<1ms)** | X (휘발)     | 실시간 스트리밍 대화, 일회성 Q&A         |
| **Database**     | PostgreSQL | 중 (10~100ms)  | **O (영구)** | 사용자 프로필 기반 맞춤형 추천, 장바구니      |
| **Vector Store** | Milvus     | 하 (100ms+)    | O (영구)     | 매뉴얼 기반 상담, 장기 프로젝트 히스토리      |
| **Hybrid**       | **혼합 사용**  | **최적화됨**      | **O (영구)** | **엔터프라이즈급 AI 비서, 상용 챗봇 서비스** |

<aside>
💡

**하이브리드 컨텍스트 관리 프로세스**

실무에서 가장 권장되는 하이브리드 방식은 다음과 같은 흐름으로 작동합니다.

1. **L1 캐시 확인:** 현재 세션의 최신 메시지가 캐시에 있다면 즉시 프롬프트 구성.
2. **L2 DB 복원:** 캐시가 비어있다면 DB에서 최근 N개의 메시지를 로드하여 컨텍스트 복구.
3. **L3 시맨틱 검색:** 만약 질문이 "예전 프로젝트에서 뭐라고 했지?" 같은 과거 지향적 질문이라면 Vector DB에서 관련 조각을 검색.
4. **컨텍스트 최적화 (Prompt Compaction):**
    - **Sliding Window:** 가장 최근의 유효 토큰 범위 내 메시지만 선택.
    - **Summarization:** 오래된 메시지는 AI가 요약한 '한 줄 요약본'으로 대체하여 토큰 절약.

</aside>

---

## 2. 데이터베이스에서 LLM 컨텍스**트 관리하기**

RDBMS(PostgreSQL 등)는 AI 세션이 종료된 후에도 사용자의 대화 맥락을 영구적으로 보존하는 역할을 합니다. 단순히 메시지를 나열하는 것이 아니라, 대화의 단위(Conversation)와 개별 메시지(Message)를 분리하여 설계함으로써 다음과 같은 컨텍스트 관리 이점을 얻을 수 있습니다.

- **컨텍스트 스위칭(Context Switching):** 사용자가 여러 개의 대화 창을 오갈 때 각 대화의 독립적인 맥락을 즉시 로드할 수 있습니다.
- **프롬프트 최적화:** 전체 이력을 다 읽지 않고도 `summary` 필드만 참조하여 요약된 정보를 프롬프트에 주입할 수 있습니다.
- **멀티테넌시(Multi-tenancy):** 여러 사용자의 데이터가 섞이지 않도록 `userId`를 통해 물리적/논리적 격리를 수행합니다.

### ChatConversation 테이블

대화의 메타데이터와 요약 정보를 저장하는 테이블입니다.

```sql
CREATE TABLE chat_conversations
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

```java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String title;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder
    public ChatConversation(
            UUID id,
            String title
    ) {
        this.id = id;
        this.title = title;
    }
}
```

```java
import com.sparta.msa.lesson.domain.ai.entity.ChatConversation;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

}
```

### ChatMessage 테이블

대화 내역과 각 메시지별 토큰 사용량(Usage)을 저장하는 테이블입니다.

```sql
CREATE TABLE chat_messages
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id   UUID        NOT NULL,
    role              VARCHAR(20) NOT NULL, -- USER, ASSISTANT, SYSTEM
    status            VARCHAR(20) NOT NULL, -- ACTIVE, INACTIVE, DELETED
    message           TEXT        NOT NULL,
    prompt_tokens     INT,
    completion_tokens INT,
    total_tokens      INT,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

```java
public enum ChatMessageType {
    USER, ASSISTANT, SYSTEM, SUMMARY // SUMMARY 추가
}
```

```java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    ChatConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChatMessageType role;  // USER, ASSISTANT, SYSTEM

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    StatusType status;  // ACTIVE, INACTIVE, DELETED

    @Column(nullable = false, columnDefinition = "TEXT")
    String message;

    @Column
    Integer promptTokens;

    @Column
    Integer completionTokens;

    @Column
    Integer totalTokens;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder
    public ChatMessage(
            ChatConversation conversation,
            ChatMessageType role,
            StatusType status,
            String message,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        this.conversation = conversation;
        this.role = role;
        this.status = status;
        this.message = message;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
}
```

```java
import com.sparta.msa.lesson.domain.ai.entity.ChatMessage;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_IdAndStatus(UUID chatConversationId, StatusType status);

}
```

### PersistentChatService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PersistentChatService {

    private final ChatClient chatClient;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    private static final int MAX_HISTORY_MESSAGES = 20;

    @Transactional
    public ContextChatResponse chat(String conversationId, String userMessage) {
        ChatConversation conversation;
        if (!StringUtils.hasText(conversationId)) {
            conversation = saveConversation(userMessage);
        } else {
            conversation = chatConversationRepository.findById(UUID.fromString(conversationId))
                    .orElseThrow(() -> new DomainException(DomainExceptionCode.NOT_FOUND_CONVERSATION));
        }

        saveMessage(conversation, userMessage, ChatMessageType.USER, null);

        List<ChatMessage> persistentMessages =
                chatMessageRepository.findByConversation_IdAndStatus(conversation.getId(),
                        StatusType.ACTIVE);

        List<Message> recentMessages = convertToMessages(persistentMessage);

        try {
            ChatResponse response = chatClient.prompt()
                    .messages(recentMessages)
                    .call()
                    .chatResponse();

            String assistantResponse = response.getResult().getOutput().getText();
            Usage usage = response.getMetadata().getUsage();

            saveMessage(conversation, assistantResponse, ChatMessageType.ASSISTANT, usage);

            ContextChatResponse.TokenUsage tokenUsage = ContextChatResponse.TokenUsage.builder()
                    .promptTokens(usage.getPromptTokens())
                    .completionTokens(usage.getCompletionTokens())
                    .totalTokens(usage.getTotalTokens())
                    .build();

            return ContextChatResponse.builder()
                    .message(assistantResponse)
                    .conversationId(conversationId)
                    .timestamp(LocalDateTime.now())
                    .tokenUsage(tokenUsage)
                    .build();

        } catch (Exception e) {
            log.error("AI 실행 중 오류 발생: {}", e.getMessage());
            throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        }
    }

    private ChatConversation saveConversation(String userMessage) {
        String title = userMessage.length() > 50 ?
                userMessage.substring(0, 50) + "..." : userMessage;

        return chatConversationRepository.save(
                ChatConversation.builder()
                        .id(UUID.randomUUID())
                        .title(title)
                        .build());
    }

    private ChatMessage saveMessage(ChatConversation conversation, String message,
                                    ChatMessageType role, Usage usage) {
        return chatMessageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .message(message)
                .promptTokens(ObjectUtils.isEmpty(usage) ? null : usage.getPromptTokens())
                .completionTokens(ObjectUtils.isEmpty(usage) ? null : usage.getCompletionTokens())
                .totalTokens(ObjectUtils.isEmpty(usage) ? null : usage.getTotalTokens())
                .build());
    }

    private List<Message> convertToMessages(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int start = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        List<ChatMessage> limitedMessages = messages.subList(start, messages.size());

        return limitedMessages.stream()
                .map(this::mapToSpringAiMessage)
                .toList();
    }

    private Message mapToSpringAiMessage(ChatMessage entity) {
        String content = entity.getMessage();

        return switch (entity.getRole()) {
            case USER -> new org.springframework.ai.chat.messages.UserMessage(content);
            case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(content);
            case SYSTEM, SUMMARY -> new org.springframework.ai.chat.messages.SystemMessage(content);
            default -> throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        };
    }
}
```

---

## **3. LLM 컨텍스트 요약 및 압축 전략**

LLM 컨텍스트 관리의 가장 큰 도전 과제는 '유한한 입력창(Context Window)'과 '비용 효율성'입니다. 대화가 길어질수록 과거의 모든 데이터를 모델에 전달하는 것은 기술적으로 불가능하거나 경제적으로 비효율적입니다. 이를 해결하기 위한 핵심 메커니즘이 바로 요약(Summarization)입니다.

#### 1. 왜 대화 요약이 필요한가?

LLM과의 대화는 눈덩이처럼 불어나는 구조입니다. 매 질문마다 이전 대화 이력을 모두 포함해서 보내야 하므로, 턴(Turn)이 거듭될수록 기하급수적으로 토큰 사용량이 증가합니다.

- **문제점:**
    - **비용 폭증:** 같은 질문을 하더라도 대화 후반부에는 전반부보다 수십 배 많은 비용이 발생합니다.
    - **성능 저하 (Lost in the Middle):** 컨텍스트가 너무 길어지면 LLM이 문서 중간에 있는 중요한 정보를 놓치는 현상이 발생합니다.
    - **입력 제한:** 모델이 수용할 수 있는 최대 토큰(예: 128k)을 넘어서면 과거 대화가 잘려나가 맥락을 상실합니다.

        ```
        ┌─────────────────────────────────────────────────────┐
        │  대화 턴이 증가할수록 Context 사용량 폭증             │
        ├─────────────────────────────────────────────────────┤
        │                                                     │
        │  턴 1:   200 토큰                                   │
        │  턴 10:  2,000 토큰                                 │
        │  턴 50:  25,000 토큰                                │
        │  턴 100: 100,000 토큰 (비용 급증!)                  │
        │                                                     │
        └─────────────────────────────────────────────────────┘
        ```

- **해결책: 요약 및 슬라이딩 윈도우 (Sliding Window with Summary)**
    - 오래된 메시지는 핵심만 남기고압축하여 정보 밀도를 높입니다.
    - 최근 메시지는 원본 데이터를 유지하여 디테일한 대화가 가능하게 합니다.
    - 결과: 80% 이상의 토큰을 절감하면서도 대화의 전체 맥락을 유지할 수 있습니다.

        ```
        ┌─────────────────────────────────────────────────────┐
        │  오래된 대화를 요약하여 Context 절약                 │
        ├─────────────────────────────────────────────────────┤
        │                                                     │
        │  턴 1-40: → 요약본 (500 토큰)                       │
        │  턴 41-50: 원본 유지 (5,000 토큰)                   │
        │  총: 5,500 토큰 (78% 절감!)                         │
        │                                                     │
        └─────────────────────────────────────────────────────┘
        ```

#### 2. 대화 요약 서비스 구현

이 서비스는 단순히 텍스트를 줄이는 것이 아니라, 저장된 데이터를 가공하여 LLM이 가장 효율적으로 읽을 수 있는 형태로 변환합니다.

**주요 구현 전략:**

1. **임계치 기반 요약 (Threshold-based):** 모든 대화를 요약하는 것이 아니라, 메시지가 일정 개수(예: 30개) 이상 쌓였을 때만 트리거하여 연산 비용을 아낍니다.
2. **슬라이딩 윈도우(Sliding Window):** 최신 N개의 메시지만 '날것(Raw)'으로 유지하고, 그 이전 데이터는 하나의 '요약 메시지'로 통합하여 시스템 메시지 상단에 배치합니다.
3. **시스템 프롬프트 주입:** 요약된 내용은 단순한 텍스트가 아니라 `MessageRole.SYSTEM`으로 주입하여, AI가 이를 "지켜야 할 배경 지식"으로 인식하게 합니다.

```java
// PersistentChatService.java
private List<Message> convertToMessages(List<ChatMessage> messages,
                                        ChatConversation conversation) {
    if (messages == null || messages.isEmpty()) {
        return List.of();
    }

    int start = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);

    if (start > 0) {
        List<ChatMessage> limitedMessages = messages.subList(start, messages.size());
        String summary = generateSummary(messages.subList(0, start));

        messages.forEach(m -> m.setStatus(StatusType.INACTIVE));
        chatMessageRepository.saveAll(messages);

        ChatMessage summaryMessage = saveSummaryMessage(conversation, summary);

        List<ChatMessage> result = new ArrayList<>();
        result.add(summaryMessage);
        result.addAll(limitedMessages);

        return result.stream()
                .map(this::mapToSpringAiMessage)
                .toList();
    }

    return messages.stream()
            .map(this::mapToSpringAiMessage)
            .toList();
}

private String generateSummary(List<ChatMessage> messages) {
    String conversationText = messages.stream()
            .map(m -> m.getRole().name() + ": " + m.getMessage())
            .collect(Collectors.joining("\n"));

    String prompt = """
            다음 대화 내용을 핵심 정보 위주로 간결하게 요약해줘.
            요약은 향후 대화의 맥락으로 사용될 거야.
            
            대화 내용:
            %s
            """.formatted(conversationText);

    try {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    } catch (Exception e) {
        log.error("대화 요약 생성 실패: {}", e.getMessage());
        throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
}

private ChatMessage saveSummaryMessage(ChatConversation conversation, String summary) {
    return chatMessageRepository.save(ChatMessage.builder()
            .conversation(conversation)
            .role(ChatMessageType.SUMMARY)
            .status(StatusType.ACTIVE)
            .message(summary)
            .build());
}
```

---

# **Part 2: 멀티모달 (Multimodal)**

## 4. 멀티모달이란?

멀티모달(Multimodal)은 텍스트, 이미지, 오디오, 비디오 등 서로 다른 형태의 데이터를 동시에 입력받아 관계를 이해하고 추론하는 AI 기능입니다.

- **전통적인 AI (Single-modal):** 텍스트 입력 → 텍스트 출력 (텍스트 전용 모델)
- **멀티모달 (Native Multimodal):** **입력:** 텍스트 + 이미지 + 오디오 + 비디오 + 코드 등 복합 입력
    - **출력:** 텍스트 응답, 코드 생성, 이미지 생성 등 다양한 결과 도출

<aside>
💡

**지원하는 모달리티**

- **텍스트 (Text):** 방대한 문서 분석 및 대화
- **이미지 (Image):** 고해상도 사진 및 그래픽 분석
- **오디오 (Audio):** 녹음 파일, 음악, 음성 대화 직접 이해
- **비디오 (Video):** 최대 수 시간 분량의 영상 흐름 및 특정 장면 파악

</aside>

### Multimodal 기능

제미나이는 단순한 이미지 인식을 넘어, 업계 최고 수준의 '긴 문맥 이해(Long Context)'와 결합된 멀티모달 기능을 제공합니다.

- ✅ **고성능 이미지 분석:** 복잡한 사진 속 상황을 맥락적으로 설명
- ✅ **Native OCR:** 이미지 내 텍스트뿐만 아니라 수식, 손글씨, 표 구조를 완벽하게 재구성
- ✅ **시각적 추론:** 차트와 그래프의 수치 정보를 추출하여 직접 계산하거나 예측 데이터 제시
- ✅ **오디오/비디오 직접 이해:** 별도의 텍스트 변환(STT) 없이 음성이나 영상 파일 자체를 분석 (예: 영상 속 특정 인물의 행동 시점 찾기)
- ✅ **공간 지능 (Spatial Understanding):** 이미지 내 물체의 위치(Bounding Box)를 좌표로 인식

### 지원 사양 및 제한사항

개발 시 참고해야 할 제미나이(Gemini 1.5 Pro/Flash 기준)의 입력 사양입니다.

- **지원 파일 형식:**
    - **이미지:** PNG, JPEG, WEBP, HEIC, HEIF
    - **비디오:** MP4, MOV, AVI, WMV 등 (오디오 포함 가능)
    - **오디오:** MP3, WAV, AAC, FLAC 등
    - **문서:** PDF (텍스트와 이미지가 혼합된 문서 포함)
- **최대 입력 용량:** API 기준 파일당 최대 **20MB** (단, 전체 프롬프트는 모델의 토큰 제한 내에서 작동)
- **컨텍스트 윈도우 (강점):**
    - **Gemini 1.5 Pro:** 최대 200만 토큰 (이미지 수천 장 또는 수 시간의 영상 처리 가능)
    - **Gemini 1.5 Flash:** 최대 100만 토큰 (빠른 속도와 대량의 멀티모달 데이터 처리 최적화)

| **구분**     | **무료 계정 (Basic)**        | **유료 계정 (Advanced/Pro)**   |
|------------|--------------------------|----------------------------|
| **사용 모델**  | Gemini 3 Flash (빠르고 효율적) | Gemini 3.1 Pro (복잡한 추론 특화) |
| **파일 업로드** | 한 번에 최대 **10개** 파일       | 더 많은 개수 및 대용량 지원           |
| **비디오 분석** | 약 **5분** 내외의 영상          | **수 시간** 분량의 긴 영상 분석 가능    |
| **오디오 분석** | 약 **10분** 내외의 음성         | 매우 긴 회의록이나 오디오 파일 분석       |
| **컨텍스트 창** | 약 32,000 토큰 (약 50페이지)    | 최대 **200만 토큰** (책 수십 권 분량) |

---

## **5. 실습 : Spring AI 멀티모달 구현하기**

멀티모달 학습의 핵심은 "이미지 데이터를 어떻게 AI 모델이 이해할 수 있는 형식(Media 객체)으로 변환하여 전달하는가"입니다.

### 데이터 통신을 위한 DTO 정의

클라이언트(프론트엔드)와 서버, 그리고 서비스 계층 간의 데이터를 안전하고 깔끔하게 전달하기 위해 DTO를 먼저 정의합니다.

- **`ImageAnalysisRequest`**: 클라이언트로부터 `MultipartFile` 형태의 이미지와 질문(`prompt`)을 받는 그릇입니다. 생성자에서 필수 값 유무를 체크합니다.

    ```java
    import lombok.AccessLevel;
    import lombok.Getter;
    import lombok.experimental.FieldDefaults;
    import org.springframework.web.multipart.MultipartFile;
    
    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class ImageAnalysisRequest {
    
      String message;
    
      MultipartFile image;
    
    }
    ```

- **`ImageAnalysisResponse`**: AI의 분석 결과와 더불어 사용된 이미지 정보, 토큰 사용량(TokenUsage)을 포함하여 클라이언트에게 응답합니다.

    ```java
    import lombok.AccessLevel;
    import lombok.Builder;
    import lombok.Getter;
    import lombok.experimental.FieldDefaults;
    
    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class ImageAnalysisResponse {
    
      String analysis;
    
      String imageType;
    
      Long imageSize;
    
      TokenUsage tokenUsage;
    
      @Getter
      @Builder
      @FieldDefaults(level = AccessLevel.PRIVATE)
      public static class TokenUsage {
    
        Integer promptTokens;
    
        Integer completionTokens;
    
        Integer totalTokens;
      }
    }
    ```

### VisionChatService 구현: 멀티모달의 핵심 로직

`VisionChatService`는 Spring AI의 `ChatClient`를 활용하여 실제로 LLM과 통신하는 핵심 계층입니다.

- **Media 객체 생성**: Spring AI는 이미지 데이터를 처리할 때 `Media` 클래스를 사용합니다. 이미지의 MIME 타입(image/jpeg 등)과 바이트 데이터를 결합합니다.
- **UserMessage 구성**: 텍스트 프롬프트와 앞서 만든 `Media` 객체들을 리스트 형태로 묶어 `UserMessage`를 생성합니다. 이것이 모델에 전달되는 최종 입력값입니다.
- **다양한 편의 메서드**:
    - **OCR**: 텍스트 추출을 위한 전용 프롬프트 사용.
    - **차트 분석**: 수치 데이터 및 인사이트 도출을 위한 특화 프롬프트.
    - **비교 분석**: 두 개의 `Media` 객체를 리스트에 담아 한 번에 전송하여 모델이 두 이미지를 대조하게 합니다.

```java
import com.sparta.msa.lesson.domain.ai.dto.response.ImageAnalysisResponse;
import com.sparta.msa.lesson.global.exception.DomainException;
import com.sparta.msa.lesson.global.exception.DomainExceptionCode;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionChatService {

    private final ChatClient chatClient;

    @Transactional
    public ImageAnalysisResponse analyzeImage(String message, MultipartFile image)
            throws IOException {

        String contentType = image.getContentType();
        if (contentType == null) {
            contentType = "image/jpeg";
        }

        try {
            String finalContentType = contentType;
            var response = chatClient.prompt()
                    .user(u -> u
                            .text(message)
                            .media(MimeTypeUtils.parseMimeType(finalContentType), image.getResource())
                    )
                    .call()
                    .chatResponse();

            String analysis = response.getResult().getOutput().getText();

            ImageAnalysisResponse.TokenUsage tokenUsage = null;

            Usage usage = response.getMetadata().getUsage();
            if (usage != null) {
                tokenUsage = ImageAnalysisResponse.TokenUsage.builder()
                        .promptTokens(usage.getPromptTokens())
                        .completionTokens(usage.getCompletionTokens())
                        .totalTokens(usage.getTotalTokens())
                        .build();
            }

            return ImageAnalysisResponse.builder()
                    .analysis(analysis)
                    .imageType(contentType)
                    .imageSize(image.getSize())
                    .tokenUsage(tokenUsage)
                    .build();

        } catch (Exception e) {
            log.error("Gemini 이미지 분석 처리 중 오류 발생: {}", e.getMessage());
            throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        }
    }

    @Transactional
    public ImageAnalysisResponse extractText(MultipartFile imageFile) throws IOException {
        String prompt = "이미지에 있는 모든 텍스트를 정확하게 추출해주세요. 텍스트만 출력하세요.";
        return analyzeImage(prompt, imageFile);
    }

}
```

### Controller 구현: API 엔드포인트

외부에서 멀티모달 기능을 사용할 수 있도록 HTTP 엔드포인트를 노출합니다. 이미지 전송을 위해 모든 요청은 `MULTIPART_FORM_DATA_VALUE` 타입을 소비(consumes)하도록 설정되어 있습니다.

```java
@PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ApiResponse<ImageAnalysisResponse> analyzeImage(
        @RequestParam String message,
        @RequestParam MultipartFile image) throws IOException {
    return ApiResponse.ok(visionChatService.analyzeImage(message, image));
}

```

### Swagger API 테스트

```
curl -X 'POST' \
  'http://localhost:8080/api/chat/gemini/analyze?message=이미지에 있는 모든 텍스트를 정확하게 추출해주세요. 텍스트만 출력하세요.' \
  -H 'accept: */*' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@스크린샷 2026-03-25 133507.png;type=image/png'

```

```bash
{
  "data": {
    "analysis": "3일차. Advisor와 FunctionCall...",
    "imageType": "image/png",
    "imageSize": 53188,
    "tokenUsage": {
      "promptTokens": 310,
      "completionTokens": 140,
      "totalTokens": 450
    }
  }
}

```

### **추가 활용 예시**

이미지 분석 결과를 단순히 텍스트로 받는 것을 넘어, 서비스에서 즉시 사용할 수 있는 구조화된 데이터(Structured Data)로 변환하는 역할에 예시 입니다.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final VisionService visionChatService;
    private final ObjectMapper objectMapper; // JSON 파싱을 위해 필요

    /**
     * 영수증 정보 추출 (Structured Data Extraction)
     */
    public ReceiptData extractReceipt(MultipartFile receiptImage) {
        String prompt = """
                당신은 영수증 분석 전문가입니다. 
                첨부된 영수증 이미지에서 다음 정보를 추출하여 정확한 JSON 형식으로 응답해주세요.
                다른 설명 없이 오직 JSON 데이터만 출력하세요.
                
                {
                  "storeName": "가게명 (문자열)",
                  "date": "결제 날짜 (YYYY-MM-DD 형식)",
                  "totalAmount": "총 결제 금액 (숫자)",
                  "items": [
                    {"name": "상품명", "price": "가격 (숫자)"}
                  ]
                }
                """;

        try {
            // VisionService의 analyzeImage 호출 (ImageAnalysis DTO 활용)
            ImageAnalysisResponse response = visionChatService.analyzeImage(ImageAnalysis.of(prompt, receiptImage));

            // AI 응답(JSON 문자열)을 객체로 변환
            return objectMapper.readValue(response.analysis(), ReceiptData.class);
        } catch (IOException e) {
            log.error("영수증 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("영수증 데이터를 처리할 수 없습니다.", e);
        }
    }

    /**
     * 명함 정보 추출 (Entity Extraction)
     */
    public BusinessCard extractBusinessCard(MultipartFile cardImage) {
        String prompt = """
                명함 이미지에서 연락처 정보를 추출해주세요. 
                정보가 식별되지 않는 항목은 null로 표시하세요.
                형식: 이름 / 회사명 / 직책 / 전화번호 / 이메일 / 주소
                """;

        try {
            ImageAnalysisResponse response = visionChatService.analyzeImage(ImageAnalysis.of(prompt, cardImage));
            // 텍스트 분석 로직 호출 (구현 예시 생략)
            return parseBusinessCard(response.analysis());
        } catch (IOException e) {
            log.error("명함 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("명함 정보를 읽을 수 없습니다.", e);
        }
    }

    /**
     * 제품 결함 검사 (Visual Quality Inspection)
     */
    public DefectReport inspectProduct(MultipartFile productImage) {
        String prompt = """
                제조 공정의 품질 검사관 역할을 수행하세요. 
                이미지를 정밀 분석하여 다음 항목에 대한 보고서를 작성하세요:
                
                1. 육안으로 보이는 결함 유무 및 위치
                2. 표면 손상(긁힘, 찌그러짐) 상세 설명
                3. 색상 일관성 및 이상 여부
                4. 전체 품질 점수 (1: 폐기, 10: 완벽)
                """;

        try {
            ImageAnalysisResponse response = visionChatService.analyzeImage(ImageAnalysis.of(prompt, productImage));
            return parseDefectReport(response.analysis());
        } catch (IOException e) {
            log.error("제품 검사 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("이미지 분석에 실패했습니다.", e);
        }
    }
}

```

**1. 영수증 정보 추출 (`extractReceipt`)**

가장 정교한 처리가 필요한 부분입니다. AI에게 단순 설명을 요구하는 것이 아니라 **JSON 형식**으로 응답하도록 강제합니다.

- **구조화된 출력(Structured Output):** `storeName`, `totalAmount` 등 정해진 키(Key) 값을 지정함으로써, 이후 Java 객체(`ReceiptData`)로의 파싱을 용이하게 합니다.
- **활용 사례:** 비용 정산 시스템, 가계부 앱, 세무 처리 자동화.

**2. 명함 정보 추출 (`extractBusinessCard`)**

이미지 내의 텍스트 간의 관계를 파악하는 **개체명 인식(Named Entity Recognition)** 기능이 핵심입니다.

- **맥락 이해:** "010-..."은 전화번호로, "@..."은 이메일로, 회사 로고 근처의 큰 글자는 회사명으로 AI가 스스로 판단하여 분류합니다.
- **활용 사례:** 영업 관리 도구(CRM) 입력 자동화, 인맥 관리 서비스.

**3. 제품 결함 검사 (`inspectProduct`)**

이 부분은 AI의 **시각적 추론(Visual Reasoning)** 능력을 활용합니다.

- **다각도 분석:** 단순한 OCR을 넘어 이미지의 픽셀 정보를 분석하여 '긁힘'이나 '변색' 같은 물리적 상태를 판별합니다.
- **정성적 평가:** 품질 점수(1-10점)를 매기게 함으로써 관리자가 우선순위를 정해 검수할 수 있는 지표를 제공합니다.
- **활용 사례:** 제조 공정 자동 검수, 중고 거래 물품 상태 확인.