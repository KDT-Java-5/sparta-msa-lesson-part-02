# 3일차. **Advisor와** FunctionCalling

### **이번에 배울 것**

<aside>
❗

이번 학습에서는 Spring AI의 Advisor 패턴과 Function Calling을 깊이 있게 다룹니다.

비즈니스 로직과 AI 공통 관심사를 명확하게 분리해 주는 Advisor 패턴을 학습합니다. Spring AOP처럼 채팅 메시지 흐름을 가로채어 로깅, 보안 검증, 컨텍스트 보강 등을 처리하는 내장 Advisor의 동작 원리를 이해하고, 커스텀 Advisor를 직접 구축하며 유지보수하기 쉬운 AI 코드 작성법을 체득합니다.

AI가 외부 시스템과 직접 상호작용하게 만드는 Function Calling을 마스터합니다. 사용자의 의도를 파악해 스스로 함수 호출을 판단하고 파라미터를 추출하여 실시간 API나 데이터베이스를 조작하는 과정을, DTO 정의부터 에이전트 시나리오 설계까지 단계별로 실습합니다.

</aside>

## **1. 코드를 더 아름답게: Spring AI Advisor 가이드**

### Advisor란 무엇인가?

**Advisor**는 Spring AI에서 `ChatClient`의 요청(Request)과 응답(Response) 사이를 가로채어 추가적인 로직을 수행하는 미들웨어(Middleware)이자 **인터셉터(Interceptor)** 패턴의 구현체입니다.

복잡한 비즈니스 로직(대화 저장, 문서 검색 등)을 `ChatClient` 외부로 분리하여, 핵심 서비스 로직을 간결하게 유지하는 것이 목적입니다.

### Advisor의 동작 방식

Advisor는 마치 양파 껍질처럼 LLM 호출 과정을 감싸고 있습니다. 요청이 나갈 때는 밖에서 안으로(**Before**), 응답이 돌아올 때는 안에서 밖으로(**After**) 순차적으로 실행됩니다.

1. **사용자 질문**: 사용자가 `call()`을 호출합니다.
2. **Advisor 1 (Before)**: 요청 전처리 (예: 질문에 '한국어로 답변해줘' 추가).
3. **Advisor 2 (Before)**: 컨텍스트 추가 (예: 벡터 DB에서 관련 문서 조회 후 프롬프트에 삽입).
4. **LLM 호출**: 최종 수정된 프롬프트가 모델로 전달됩니다.
5. **Advisor 2 (After)**: 응답 후처리 (예: 답변의 토큰 사용량 로깅).
6. **Advisor 1 (After)**: 최종 응답 수정 (예: 답변을 데이터베이스에 저장).
7. **최종 응답**: 사용자에게 결과 전달.

```text
사용자 질문
    ↓
[Advisor 1] ← 전처리: before()
    - 요청 전처리
    - 프롬프트 수정
    - 컨텍스트(Context) 추가
    ↓
[Advisor 2] ← 전처리: before() - Advisor Chain
    ↓
LLM 호출
    ↓
[Advisor 2] ← 후처리: after()
    - 응답 후처리
    - 로깅/모니터링
    ↓
[Advisor 1] ← 후처리: after() - Advisor Chain
    ↓
최종 응답
```

#### Context (공유 저장소)

**Context**는 Advisor 체인을 따라 흐르는 **공유 바구니**와 같습니다.

- **정의**: 단순한 `Map<String, Object>` 형태의 저장소입니다.
- **역할**:
    - `Advisor A`가 계산한 데이터를 `Advisor B`가 사용할 수 있게 전달합니다.
    - 요청(Request) 시점에 생성된 데이터를 보관했다가 응답(Response) 시점에 꺼내어 사용합니다.
- **활용 예시**: `ChatMemoryAdvisor`는 `CHAT_MEMORY_CONVERSATION_ID`라는 키로 대화방 ID를 관리하여 해당 방의 이전 기록을 가져옵니다.

### 왜 Advisor를 사용해야 하는가? (Before vs After)

**❌ 사용하지 않을 때 (Spaghetti Code)**

모든 부가 기능이 Service 계층에 섞여 있어, 코드가 길어지고 재사용이 어렵습니다.

```java
public String chat(String message) {
    // 1. 대화 기록 조회 (관심사 1)
    List<Message> history = chatMemory.get(conversationId);

    // 2. 문서 검색 (관심사 2)
    List<Document> docs = vectorStore.similaritySearch(message);

    // 3. 프롬프트 구성 (지저분한 문자열 결합)
    String context = docs.stream().map(Document::getContent).collect(Collectors.joining());
    String fullPrompt = "Context: " + context + "\nHistory: " + history + "\nQuestion: " + message;

    // 4. LLM 호출
    String response = chatClient.call(fullPrompt);

    // 5. 대화 기록 저장 (관심사 1의 마무리)
    chatMemory.add(conversationId, message, response);

    return response;
}
```

**✅ Advisor 사용할 때 (Clean Code)**

각 기능을 Advisor로 독립시키면 Service 코드는 오직 "질문하고 답 받기"에만 집중합니다.

```java
// 1. 설정 단계: 필요한 기능을 조립(Compose)합니다.
ChatClient chatClient = chatClientBuilder
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory), // 대화 기록 자동 관리
                        new QuestionAnswerAdvisor(vectorStore)    // RAG(문서 검색) 자동 처리
                )
                .build();

// 2. 실행 단계: 비즈니스 로직이 매우 단순해집니다.
public String chat(String message) {
    return chatClient.prompt()
            .user(message)
            .call()
            .content();
}
```

### BaseAdvisor 아키텍처 상세 분석

Spring AI의 Advisor 시스템을 깊이 있게 이해하려면 그 근간이 되는 **`BaseAdvisor` 인터페이스**와 데이터를 주고받는 **객체 구조**를 파악해야 합니다. Advisor는 단순히 데이터를 전달하는 것이 아니라, 요청과 응답의 생명주기(Lifecycle)를 관리합니다.

#### BaseAdvisor의 핵심 메서드와 흐름

`BaseAdvisor`는 동기(`Call`) 방식과 비동기(`Stream`) 방식 모두를 지원하며, 개발자가 복잡한 흐름 제어 대신 비즈니스 로직(Before/After)에만 집중할 수 있도록 설계되었습니다.

**동기 호출 처리 (`adviseCall`)**

가장 기본적인 흐름으로, 직관적인 **샌드위치 구조**를 가집니다.

- **작동 원리**:
    1. `before()`를 실행하여 요청(Request)을 가공합니다.
    2. `chain.nextCall()`을 호출하여 다음 Advisor 혹은 최종 LLM에게 제어권을 넘깁니다.
    3. 결과가 돌아오면 `after()`를 실행하여 응답(Response)을 최종 수정합니다.

```java
public interface BaseAdvisor extends CallAdvisor, StreamAdvisor {

    /**
     * ✅ 동기 호출 처리 (자동 구현됨)
     */
    @Override
    default ChatClientResponse adviseCall(
            ChatClientRequest request,
            CallAdvisorChain chain) {

        // 1. 요청 전처리
        ChatClientRequest processedRequest = before(request, chain);

        // 2. 다음 Advisor 또는 LLM 호출
        ChatClientResponse response = chain.nextCall(processedRequest);

        // 3. 응답 후처리
        return after(response, chain);
    }
}
```

**반드시 구현해야 할 두 핵심 기능**

- **`before(request, chain)`**: LLM에 전달되기 전의 **메시지, 옵션, 컨텍스트**를 수정합니다. (예: 시스템 프롬프트 추가)
- **`after(response, chain)`**: LLM이 반환한 **결과값**을 검증하거나 저장합니다. (예: 대화 기록 DB 저장)

```java
/**
 * ⭐ 구현해야 할 메서드 1: 요청 전처리
 */
ChatClientRequest before(ChatClientRequest request, AdvisorChain chain);

/**
 * ⭐ 구현해야 할 메서드 2: 응답 후처리
 */
ChatClientResponse after(ChatClientResponse response, AdvisorChain chain);

/**
 * 선택 사항: 스케줄러 커스터마이징
 */
default Scheduler getScheduler() {
    return Schedulers.boundedElastic();
}
```

#### Request & Response 예시

Advisor 체인을 타고 흐르는 데이터 객체들은 단순한 텍스트 이상의 정보를 담고 있습니다.

**🔹 ChatClientRequest (요청 데이터)**

LLM을 호출하기 위해 필요한 모든 '재료'가 담겨 있습니다.

- **`adviseContext`**: **가장 중요한 속성**입니다. Advisor들끼리 정보를 공유하는 `Map`입니다. (예: RAG Advisor가 찾은 문서 리스트를 여기 담아두면, 다음 로그 Advisor가 이를 읽어 기록할 수 있습니다.)
- **`chatOptions`**: 온도(Temperature), Top-P 등 모델 제어 설정입니다.
- **`messages` & `userText`**: 실제 대화 내용과 사용자 질문입니다.

```java
public class ChatClientRequest {
    private Map<String, Object> adviseContext;    // Advisor 간 데이터 공유
    private ChatOptions chatOptions;              // LLM 옵션
    private List<Media> media;                    // 미디어 (이미지 등)
    private List<Message> messages;               // 메시지 목록
    private String userText;                      // 사용자 입력
    private Map<String, Object> userParams;       // 사용자 파라미터
}
```

**🔹 ChatClientResponse (응답 데이터)**

LLM의 결과물과 함께 처리 과정에서 생성된 메타데이터를 담고 있습니다.

- **`chatResponse`**: 모델이 생성한 실제 답변 텍스트와 토큰 사용량 정보가 들어있습니다.
- **`adviseContext`**: 요청 시 사용되었던 컨텍스트가 그대로 유지되어, 후처리 과정에서도 참조가 가능합니다.

```java
public class ChatClientResponse {
    private ChatResponse chatResponse;            // LLM 응답
    private Map<String, Object> adviseContext;    // Advisor 컨텍스트
}
```

### **내장 Advisor 종류**

<aside>
🗨️

**PromptChatMessageAdvisor**

Spring AI에서 가장 빈번하게 사용되는 기능 중 하나는 "이전 대화를 기억하는 것"입니다. LLM은 기본적으로 상태가 없는(Stateless) 구조이기 때문에, 앞선 대화를 기억하게 하려면 누군가 과거 내역을 질문과 함께 전달해줘야 합니다. 이 역할을 수행하는 핵심 Advisor가 바로 `PromptChatMemoryAdvisor`입니다.

- **목적**: 대화 기록(Chat History)을 현재 프롬프트에 직접 포함시켜, 모델이 문맥(Context)을 이해하고 연속성 있는 답변을 하도록 돕는 가장 기본적인 메모리 관리 방식입니다.
- **핵심 가치**: 개발자가 일일이 과거 메시지를 리스트에 담아 보낼 필요 없이, Advisor가 `ChatMemory` 저장소에서 데이터를 꺼내와 자동으로 조립해줍니다.

**동작 원리: "기억의 조립" 과정**

사용자가 질문을 던질 때마다 Advisor는 [조회 → 조립 → 저장]의 사이클을 반복합니다.

**첫 번째 대화: 정보 입력**

- **사용자**: "내 이름은 김철수야"
- **Advisor (Before)**: 저장소 확인 (데이터 없음). "내 이름은 김철수야"만 전송.
- **LLM**: "안녕하세요 김철수님!"
- **Advisor (After)**: 질문("내 이름은...")과 답변("안녕하세요...")을 쌍으로 묶어 `ChatMemory`에 저장.

**두 번째 대화: 문맥 추론**

- **사용자**: "내 이름이 뭐였지?"
- **Advisor (Before)**: 저장소에서 이전 대화(`User: 내 이름은... / Assistant: 안녕하세요...`)를 조회.
- **프롬프트 조립**:

    ```java
    [이전 대화 내역]
    
    User: 내 이름은 김철수야
    
    Assistant: 안녕하세요 김철수님!
    
    [현재 질문]
    
    User: 내 이름이 뭐였지?
    ```

- **LLM**: 조립된 정보를 보고 답변합니다. **"김철수님이십니다!"**

**사용 방법 및 설정**

`PromptChatMemoryAdvisor`를 사용하려면 데이터를 담을 그릇(`ChatMemory`)과 관리자(Advisor)를 설정해야 합니다.

**ChatMemory 빈 등록**

대화 내용을 어디에 저장할지 결정합니다. 테스트용으로는 메모리(`InMemoryChatMemory`)를, 운영 환경에서는 Redis나 JDBC 기반 저장소를 사용합니다.

```java
@Bean
public ChatMemory chatMemory() {
    // 서버 메모리에 대화 내역을 저장 (재시작 시 초기화됨)
    return new InMemoryChatMemory();
}
```

**ChatClient에 Advisor 추가**

`ChatClient` 빌더를 통해 Advisor를 등록합니다.

```java
@Bean
public ChatClient conversationalChatClient(
        ChatClient.Builder builder,
        ChatMemory chatMemory) {

    return builder
            .defaultAdvisors(
                    new PromptChatMemoryAdvisor(
                            chatMemory,           // 저장소 구현체
                            "user-123",           // 대화 식별 ID (사용자별/세션별 구분)
                            10                    // 윈도우 사이즈: 최근 10개 메시지만 프롬프트에 포함
                    )
            )
            .build();
}
```

**⚠️ 주의사항 및 팁**

- **토큰 제한 (Window Size)**: 대화가 길어지면 프롬프트 크기가 너무 커져 비용이 증가하거나 모델의 최대 토큰 제한에 걸릴 수 있습니다. `10`과 같이 적절한 메시지 유지 개수를 설정하는 것이 중요합니다.
- **Conversation ID 관리**: 위 예시의 `"user-123"`처럼 고정된 ID를 쓰면 모든 사용자가 같은 대화 기록을 공유하게 됩니다. 실제 서비스에서는
  `advisors(adv -> adv.param("chat_memory_conversation_id", sessionId))`와 같이 **동적으로 세션 ID를 전달**해야 합니다.
- **메모리 휘발성**: `InMemoryChatMemory`는 서버가 꺼지면 기억을 잃습니다. 영구적인 기억이 필요하다면 `CassandraChatMemory`나 `JdbcChatMemory`를 검토하세요.

</aside>

<aside>
🗣️

**MessageChatMemoryAdvisor**

대화형 AI를 개발할 때, 단순 텍스트 결합보다 더 정교한 관리가 필요할 때가 있습니다. `MessageChatMemoryAdvisor`는 대화 내역을 단순한 문자열이 아닌 **구조화된 객체(Message Object)** 단위로 다루는 진화된 방식의 메모리 Advisor입니다.

- **목적**: 대화 기록을 프롬프트에 텍스트로 이어 붙이는 대신, LLM이 이해하는 **메시지 리스트(List of Messages)** 형태로 관리하여 더 세밀한 컨텍스트 제어를 제공합니다.
- **핵심 가치**: 각 메시지의 역할(발화자), 메타데이터, 타임스탬프 등을 유지하므로 모델이 "누가 어떤 의도로 말했는지"를 훨씬 더 정확하게 파악할 수 있습니다

| **구분**        | **PromptChatMemoryAdvisor** | **MessageChatMemoryAdvisor** |
|---------------|-----------------------------|------------------------------|
| **관리 단위**     | 하나의 거대한 **String**(텍스트)     | 개별적인 **Message 객체** 리스트      |
| **구조**        | 비구조화 (단순 텍스트 합치기)           | 구조화 (역할, 메타데이터 보존)           |
| **LLM 전달 방식** | 사용자 질문의 일부로 포함됨             | 독립된 메시지 이력으로 전달됨             |
| **장점**        | 구현이 매우 단순함                  | **정밀한 제어 가능**, 멀티모달 대응 유리    |

**Message 객체의 내부 구조**

`MessageChatMemoryAdvisor`가 다루는 데이터는 아래와 같은 속성을 가진 객체입니다. 이 구조 덕분에 단순 텍스트 이상의 정보를 LLM에 전달할 수 있습니다.

- **MessageType**: 메시지의 주체를 구분합니다.
    - `USER`: 사용자의 질문
    - `ASSISTANT`: AI의 답변
    - `SYSTEM`: AI의 성격이나 지침 (System Prompt)
- **Content**: 실제 대화 내용(텍스트)입니다.
- **Metadata**: 메시지 생성 시간, 사용된 토큰 수, 특정 필터링 결과 등 부가 정보를 담는 바구니입니다.

**사용 방법 및 설정**

설정 방식은 `PromptChatMemoryAdvisor`와 유사하지만, 내부적으로 메시지 객체를 처리하는 로직이 작동합니다.

```java
@Bean
public ChatClient messageChatClient(
        ChatClient.Builder builder,
        ChatMemory chatMemory) {

    return builder
            .defaultAdvisors(
                    new MessageChatMemoryAdvisor(
                            chatMemory,           // 저장소 (InMemory, JDBC 등)
                            "conversation-123",    // 대화 식별 ID
                            20                    // 윈도우 사이즈: 최근 20개 메시지 유지
                    )
            )
            .build();
}
```

**왜 MessageChatMemoryAdvisor를 써야 할까요?**

1. **모델 최적화**: 최신 모델(GPT-4, Claude 등)은 텍스트 덩어리보다 메시지 리스트 형식을 입력받을 때 더 정확하게 문맥을 파악합니다.
2. **멀티모달 대응**: 메시지 객체는 텍스트뿐만 아니라 이미지, 문서 파일 등의 미디어 정보를 포함할 수 있어, 과거에 공유한 이미지를 기억해야 하는 서비스에 필수적입니다.
3. **데이터 정제**: 특정 역할(예: SYSTEM 메시지)은 메모리에서 제외하거나, 메타데이터를 기반으로 특정 조건의 대화만 골라서 프롬프트에 넣는 등의 커스터마이징이 용이합니다.

</aside>

<aside>
🛡️

**SafeGuardAdvisor**

AI 서비스가 실제 사용자에게 노출될 때 가장 중요한 것은 안전성(Safety)입니다. 모델이 부적절한 질문에 답변하거나, 예기치 않게 편향되거나 유해한 내용을 출력하는 것을 방지해야 합니다. `SafeGuardAdvisor`는 이러한 **안전 가드레일(Safety Guardrails)** 역할을 수행합니다.

- **목적**: 입력(질문)과 출력(답변)을 실시간으로 감시하여 부적절한 콘텐츠를 필터링하고 서비스의 윤리적 가이드라인을 준수하게 합니다.
- **핵심 가치**:
    - **비용 절감**: 유해한 질문을 LLM에 전달하기 전에 차단하여 불필요한 API 호출 비용을 아낍니다.
    - **브랜드 보호**: AI가 부적절한 발언을 하여 발생할 수 있는 리스크를 사전에 방지합니다.

**동작 원리: 이중 필터링 시스템**

`SafeGuardAdvisor`는 요청 전(`before`)과 응답 후(`after`) 두 번의 검문을 실시합니다.

**CASE 1: 입력 차단 (Input Validation)**

사용자가 악의적인 의도를 가진 질문을 던질 때 작동합니다.

- **사용자**: "비속어가 포함된 질문"
- **Advisor (before)**: 질문 텍스트에서 차단 키워드를 발견하거나 외부 안전성 검증 API(Service)를 호출하여 유해성을 감지합니다.
- **결과**: **LLM을 호출하지 않고** 즉시 예외를 발생시키거나 안내 메시지를 반환합니다.

```text
사용자: "욕설이 포함된 질문"
    ↓
[SafeGuardAdvisor - before()]
    - 입력 검증
    - 욕설 감지!
    - 예외 발생: "부적절한 내용이 포함되어 있습니다"
    ↓
❌ LLM 호출 안 함 (조기 차단)

사용자: "정상적인 질문"
    ↓
[SafeGuardAdvisor - before()]
    - 입력 검증 통과
    ↓
LLM 호출
```

**CASE 2: 출력 정화 (Output Sanitization)**

질문은 정상이었으나, 모델이 생성한 답변이 부적절할 때 작동합니다.

- **사용자**: "정상적인 질문"
- **Advisor (before)**: 검증 통과 → LLM 호출.
- **LLM**: 모델의 오류나 편향으로 인해 부적절한 답변 생성.
- **Advisor (after)**: 답변 내용을 검사하여 유해성 발견 시, 답변을 **"안전한 문구"로 교체**하여 전달합니다.

```text
LLM 호출
    ↓
LLM: "부적절한 내용이 포함된 응답"
    ↓
[SafeGuardAdvisor - after()]
    - 출력 검증
    - 부적절한 내용 감지!
    - 안전한 응답으로 대체
    ↓
응답: "죄송합니다. 적절한 답변을 생성할 수 없습니다"
```

**구현 및 사용 방법**

보통 `SafeGuardAdvisor`는 직접 커스텀하여 비즈니스 규칙에 맞게 구현합니다.

**Advisor 구성 요소**

```java
public class SafeGuardAdvisor implements BaseAdvisor {
    private final List<String> blockedKeywords; // 단순 키워드 매칭
    private final ContentSafetyService safetyService; // AI 기반 유해성 검사 서비스
}
```

**ChatClient 설정**

`ChatClient` 빌더의 가장 앞단에 배치하여 다른 Advisor들이 실행되기 전에 먼저 검사하는 것이 효율적입니다.

```java
@Bean
public ChatClient safeChatClient(
        ChatClient.Builder builder,
        ContentSafetyService safetyService) {

    return builder
            .defaultAdvisors(
                    new SafeGuardAdvisor(
                            List.of("욕설", "비속어", "혐오", "개인정보"), // 차단 키워드 리스트
                            safetyService                               // 외부 안전 검증 엔진
                    )
            )
            .build();
}
```

**SafeGuardAdvisor로 할 수 있는 일들**

1. **욕설 및 혐오 표현 차단**: 커뮤니티 가이드라인 준수.
2. **개인정보 유출 방지**: 주민등록번호, 전화번호 등이 답변에 포함될 경우 마스킹(`**`) 처리.
3. **정치/종교 편향 방지**: 민감한 주제에 대해 중립적인 답변을 하도록 유도하거나 답변 거부.
4. **프롬프트 인젝션 방어**: "이전 지시사항을 무시하고..."와 같은 해킹 시도 감지 및 차단.

</aside>

### ChatMemoryAdvisor

이 Advisor는 챗봇이 "방금 뭐라고 하셨죠?"라는 질문에 당황하지 않게 만드는 **'단기 기억 장치'** 역할을 합니다.

```java
@Slf4j
public class ChatMemoryAdvisor implements BaseAdvisor {

    private static final String CONTEXT_USER_TEXT = "chat_memory_user_text";

    // 대화 저장소 (실제 서비스에선 Redis나 DB로 대체 가능)
    private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();
    private final String conversationId;
    private final int maxMessages;

    public ChatMemoryAdvisor(String conversationId, int maxMessages) {
        this.conversationId = conversationId;
        this.maxMessages = maxMessages;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        // [핵심] 1. 저장소에서 이전 대화 기록을 가져옴
        List<Message> history = conversationStore.getOrDefault(conversationId,
                new CopyOnWriteArrayList<>());

        // [핵심] 2. 이전 기록 + 현재 질문을 합쳐서 새로운 메시지 리스트 생성
        List<Message> fullMessages = new ArrayList<>(history);
        fullMessages.addAll(request.prompt().getInstructions());

        // 3. 현재 질문 텍스트 추출 (나중에 after에서 저장하기 위함)
        String userText = request.prompt().getInstructions().stream()
                .filter(m -> m.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .findFirst().orElse("");

        // 4. 합쳐진 메시지들로 프롬프트를 재구성하여 반환
        return request.mutate()
                .prompt(new Prompt(fullMessages)) // AI에게 과거 기록을 함께 보냄
                .context(CONTEXT_USER_TEXT, userText)
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        List<Message> history = conversationStore.computeIfAbsent(
                conversationId, k -> new CopyOnWriteArrayList<>()
        );

        // 1. 사용자 질문 저장 (context에서 꺼냄)
        Optional.ofNullable(response.context().get(CONTEXT_USER_TEXT))
                .map(Object::toString)
                .filter(text -> !text.isBlank())
                .ifPresent(text -> history.add(new UserMessage(text)));

        // 2. AI 응답 저장
        if (response.chatResponse() != null && response.chatResponse().getResult() != null) {
            var output = response.chatResponse().getResult().getOutput();
            history.add(new AssistantMessage(output.getText(), output.getMetadata()));
        }

        // 3. FIFO 용량 관리
        while (history.size() > maxMessages && !history.isEmpty()) {
            history.remove(0);
        }

        return response;
    }

    @Override
    public String getName() {
        return "ChatMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

```java
@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        ChatMemoryAdvisor myAdvisor = new ChatMemoryAdvisor("user-123", 10);

        return builder
                .defaultSystem("""
                        당신은 친절하고 도움이 되는 AI 어시스턴트입니다.
                        사용자의 질문에 정확하고 이해하기 쉽게 답변해주세요.
                        """)
                .defaultAdvisors(myAdvisor) // 직접 만든 어드바이저 장착
                .build();
    }
}
```

**1. `before` 단계: 질문을 메모지에 적어두기**

사용자가 질문을 던지면, LLM에 보내기 직전에 질문 내용을 `adviseContext`라는 공유 저장소에 임시로 저장합니다.

- **이유**: `after` 단계(응답이 온 후)에 한꺼번에 질문과 답변을 쌍으로 묶어서 저장하기 위해서입니다.

**2. `after` 단계: 질문과 답변을 일기장에 기록하기**

LLM으로부터 답변이 돌아오면 실행됩니다.

- **질문 복구**: 아까 `before`에서 적어둔 질문을 꺼내 `UserMessage`로 만듭니다.
- **답변 추출**: 방금 LLM이 만든 따끈따끈한 답변을 `AssistantMessage`로 만듭니다.
- **저장소 업데이트**: `conversationStore`(일기장)의 해당 ID 섹션에 이 두 메시지를 추가합니다.

**3. FIFO(First-In-First-Out) 방식의 기억 유지**

무한정 기억하면 메모리가 부족해지므로, `maxMessages`만큼만 기억합니다.

- 예를 들어 10개로 설정했다면, 11번째 메시지가 들어오는 순간 **가장 첫 번째 대화**를 지워버립니다.

**4. 스레드 안전(Thread-Safe) 설계**

여러 사용자가 동시에 말을 걸어도 데이터가 꼬이지 않도록 `ConcurrentHashMap`과 `CopyOnWriteArrayList`를 사용했습니다. 한 명의 AI가 여러 명과 대화해도 각자의 대화 기록이 안전하게 분리되어 저장됩니다.

---

## 2. LLM의 손과 발: Function Calling

### Function Calling이란?

**Function Calling**은 LLM이 단순히 텍스트를 생성하는 것을 넘어, **외부 시스템이나 특정 도구(API, 데이터베이스, 계산기 등)를 직접 호출**할 수 있게 해주는 기술입니다.

LLM이 스스로 판단하여 "이 질문에 답하려면 외부 도구의 도움이 필요해!"라고 결정하고, 개발자가 준비한 함수를 실행할 수 있는 '매개변수'를 추출해주는 과정입니다.

### ❓ Function Calling이 왜 필요한가요?

**❌ 기존 LLM의 한계 (지식의 섬)**

- **실시간 정보 부재**: 학습 데이터 커트라인 이후의 뉴스, 날씨, 주가 등을 모름.
- **계산 능력 부족**: 복잡한 수학 연산에서 '환각(Hallucination)' 발생 가능성.
- **외부 시스템 고립**: 사내 DB 조회, 이메일 발송, 결제 처리 등 실질적인 '행동' 불가.

**✅ Function Calling 적용 시 (현실 세계 연결)**

- **사용자**: "오늘 서울 날씨 어때?"
- **LLM**: (학습 데이터엔 없지만, 나에게 `getWeather`라는 도구가 있네!)
- **동작**: `getWeather("서울")` 호출 → 실시간 데이터 수신 → "현재 서울은 15도이며 맑습니다."라고 답변.

### **Function Calling 동작 원리**

Function Calling은 LLM이 직접 코드를 실행하는 것이 아니라, **어떤 함수를 어떤 파라미터로 실행해야 할지 "설계도"를 그려주는 과정**입니다.

1. **함수 정의 (Step 1)**: 개발자가 실제 로직(예: 날씨 API 호출)을 코드로 작성합니다.

    ```java
    // 개발자가 함수 정의
    public String getWeather(String city) {
        // 실제 API 호출
        return weatherApi.getCurrentWeather(city);
    }
    
    ```

2. **함수 등록 (Step 2)**: LLM에게 함수의 이름, 용도, 필요한 파라미터 정보를 **JSON 형식**으로 설명해 줍니다.

    ```java
    // LLM에게 함수 설명 제공
    {
      "name": "getWeather",
      "description": "특정 도시의 현재 날씨를 조회합니다",
      "parameters": {
        "city": {
          "type": "string",
          "description": "날씨를 조회할 도시명"
        }
      }
    }
    ```

3. **사용자 질문 (Step 3)**: "서울 날씨 알려줘"라고 질문합니다.

    ```
    "서울 날씨 알려줘"
    ```

4. **LLM 판단 (Step 4)**: LLM은 질문을 분석하고 등록된 함수 중 적합한 것을 골라 호출 인자(Arguments)를 뽑아냅니다. (예: `city: "서울"`)

    ```json
    {
      "thought": "날씨 정보가 필요하니 getWeather 함수를 호출해야겠다",
      "function": "getWeather",
      "arguments": {
        "city": "서울"
      }
    }
    
    ```

5. **함수 실행 (Step 5)**: 개발자의 서버가 LLM이 준 인자로 실제 함수를 실행하고 결과값을 얻습니다.

    ```java
    String result = getWeather("서울");
    // 결과: "15도, 맑음"
    
    ```

6. **최종 답변 (Step 6)**: LLM이 결과값(15도, 맑음)을 받아 자연스러운 문장으로 변환하여 사용자에게 전달합니다.

    ```
    "서울의 현재 날씨는 15도이고 맑습니다."
    
    ```

| **비교 항목**  | **일반 API 호출 (Hard-coded)**  | **Function Calling (AI-driven)**        |
|------------|-----------------------------|-----------------------------------------|
| **호출 주체**  | 개발자가 `if-else`문으로 명시        | **LLM이 질문의 의도를 보고 자동 판단**               |
| **유연성**    | 정해진 키워드나 로직에서만 작동           | 질문 방식이 다양해도 문맥을 이해해 작동                  |
| **복잡한 의도** | 처리하기 매우 어려움                 | "내일 서울 날씨 보고 우산 챙길지 알려줘" 같은 복합 질문 처리 가능 |
| **작동 예시**  | `if (input.contains("날씨"))` | **"비가 올 것 같으니 날씨 함수를 써야겠군"**            |

---

## 3. Function Calling

### **Function Calling 특징**

제미나이는 단순한 텍스트 생성을 넘어, 외부 API와 상호작용하며 실시간 데이터를 처리하는 "실행형 AI"의 핵심 기능을 제공합니다.

- **정교한 의도 파악 (Function Selection):** 사용자의 자연어 질문을 분석하여, 수많은 도구 중 어떤 함수를 호출해야 할지 매우 높은 정확도로 결정합니다.
- **구조화된 파라미터 추출:** 비정형 대화 속에서 함수 실행에 필요한 인자(Parameters)를 JSON 스키마에 맞춰 정밀하게 추출합니다.
- **멀티모달 컨텍스트 연동:** 텍스트뿐만 아니라 이미지, 영상 분석 결과를 바탕으로 적절한 함수를 호출하는 능력이 뛰어납니다.
- **병렬 함수 호출 (Parallel Calling):** 한 번의 요청으로 여러 개의 함수를 동시에 호출하거나, 순차적으로 연쇄 호출하여 복잡한 태스크를 해결합니다.
- **네이티브 통합 운영:** Google Cloud(Vertex AI) 환경에서 인증 및 보안이 강화된 엔터프라이즈급 도구 연동을 지원합니다.

### **Function Calling 예시**

제미나이는 모델이 직접 함수를 실행하는 것이 아니라, 실행에 필요한 **구조화된 요청(Function Call)**을 생성하여 개발자에게 전달합니다.

```json
// Gemini가 생성하는 Function Call 응답 구조
{
  "functionCall": {
    "name": "get_weather",
    "args": {
      "location": "서울",
      "unit": "celsius"
    }
  }
}
```

---

## **4. Spring AI Function Calling 구현하기**

앞서 배운 Function Calling의 이론을 바탕으로, Spring AI 프레임워크를 사용하여 실제 **날씨 조회, 계산기, DB 조회** 기능을 가진 AI 에이전트를 구축하는 실전 가이드입니다.

### 도구(Tools) 정의하기

Spring AI에서는 `@Tool` 어노테이션을 사용하여 일반 Java 메서드를 AI가 호출할 수 있는 '도구'로 변환합니다.

```java
@Slf4j
@Service
public class FunctionTools {

    @Tool(description = "특정 도시의 현재 날씨 정보를 조회합니다")
    public WeatherResponse getWeather(WeatherRequest request) {
        log.info("날씨 조회: {}", request.getCity());

        Random random = new Random();
        int temperature = 10 + random.nextInt(20);
        String[] conditions = {"맑음", "흐림", "비", "눈"};
        String condition = conditions[random.nextInt(conditions.length)];

        return WeatherResponse.builder()
                .city(request.getCity())
                .temperature(temperature)
                .condition(condition)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Tool(description = "두 숫자의 사칙연산을 수행합니다")
    public CalculatorResponse calculator(CalculatorRequest request) {
        log.info("계산: {} {} {}", request.getA(), request.getOperation(), request.getB());

        double result = switch (request.getOperation()) {
            case "add" -> request.getA() + request.getB();
            case "subtract" -> request.getA() - request.getB();
            case "multiply" -> request.getA() * request.getB();
            case "divide" -> {
                if (request.getB() == 0) {
                    throw new IllegalArgumentException("0으로 나눌 수 없습니다");
                }
                yield request.getA() / request.getB();
            }
            default -> throw new IllegalArgumentException("지원하지 않는 연산: " + request.getOperation());
        };

        return CalculatorResponse.builder()
                .result(result)
                .build();
    }

    @Tool(description = "현재 날짜와 시간을 반환합니다")
    public CurrentTimeResponse getCurrentTime() {
        log.info("현재 시간 조회");

        LocalDateTime now = LocalDateTime.now();
        return CurrentTimeResponse.builder()
                .isoFormat(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .readableFormat(now.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")))
                .build();
    }

}
```

### 데이터 구조(DTO) 정의

LLM은 텍스트를 분석하여 함수에 필요한 인자(Arguments)를 뽑아냅니다.

**Weather Request & Response**

```java
@Getter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeatherRequest {

    private String city;

}
```

```java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeatherResponse {
    String city;
    Intger temperature;
    String condition;
    LocalDateTime timestamp;
}
```

**Calculator Request & Response**

```java
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CalculatorRequest {
    double a;
    double b;
    String operation;
}
```

```java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CalculatorResponse {
    double result;
}
```

**CurrentTimeResponse**

```java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CurrentTimeResponse {
    String isoFormat;
    String readableFormat;
}
```

Question Request & Response

```java
@Getter
@NoArgsConstructor // JSON 역직렬화를 위해 필요
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionRequest {
    String question;
}
```

```java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerResponse {
    String answer;
}
```

### 서비스 계층 구현

`ChatClient`를 사용하여 LLM에게 도구 상자(`tools`)를 전달합니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionCallingService {

    private final ChatClient.Builder clientBuilder;
    private final FunctionTools functionTools;

    /**
     * 기본 Function Calling - 모든 도구 사용 가능
     */
    public String chat(String userMessage) {
        log.info("[Chat] User Message: {}", userMessage);
        try {
            return clientBuilder.build()
                    .prompt()
                    .user(userMessage)
                    .tools(functionTools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        }
    }

    /**
     * 시스템 메시지 설정 및 Function Calling
     */
    public String chatWithSystemMessage(String systemMessage, String userMessage) {
        log.info("[System Chat] System: {}, User: {}", systemMessage, userMessage);
        try {
            return clientBuilder.build()
                    .prompt()
                    .system(systemMessage)
                    .user(userMessage)
                    .tools(functionTools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        }
    }

}
```

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/function")
public class FunctionCallingController {

    private final FunctionCallingService functionCallingService;

    /**
     * 기본 Function Calling 채팅 인터페이스
     */
    @PostMapping("/chat")
    public ApiResponse<AnswerResponse> chat(@RequestBody QuestionRequest request) {
        String result = functionCallingService.chat(request.getQuestion());
        return ApiResponse.ok(AnswerResponse.builder().answer(result).build());
    }

}
```

<aside>
💡

**주요 기능 활용 팁**

**1. 특정 함수만 제한적으로 사용하기**

모든 도구를 보여주지 않고 상황에 맞는 특정 도구만 활성화할 수 있습니다.

```java
chatClient.prompt()
    .toolNames("getWeather") // 다른 도구는 무시하고 날씨 도구만 사용하도록 강제
    .call();
```

**2. 시스템 메시지와 조합하기**

AI의 페르소나를 설정하면서 도구를 사용하게 합니다.

```java
chatClient.prompt()
    .system("당신은 친절한 기상캐스터이자 고객센터 직원입니다.")
    .user(userMessage)
    .tools(functionTools)
    .call();
```

</aside>

---

## **5. 실습 프로젝트: Function Calling 통합 테스트**

AI가 실제로 외부 도구를 얼마나 영리하게 사용하는지 확인해 볼 차례입니다. 이 실습은 단일 호출부터 복합 추론까지 단계별로 진행됩니다. 이번 실습의 핵심은 "LLM이 자연어 문장에서 필요한 파라미터를 정확히 추출하여 통합 DTO에 채워 넣는가?"를 검증하는 것입니다.

### 1️⃣ 기본 도구 호출 (Single Tool Call)

가장 단순한 형태의 테스트입니다. 질문 속에 포함된 핵심 키워드(도시명, 숫자 등)를 적절한 필드에 매핑하는지 확인합니다.

#### **Case A: 실시간 정보(날씨) 조회**

```bash
curl -X POST http://localhost:8080/api/function/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "서울 날씨 알려줘"}'

# 예상 응답: "서울의 현재 날씨는 15도이고 맑습니다."
```

#### **Case B: 연산(계산기) 수행**

```bash
curl -X POST http://localhost:8080/api/function/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "253 곱하기 47은?"}'

# 예상 응답: "253 곱하기 47은 11,871입니다."
```

### 2️⃣ 상태 정보 조회 (No Parameter Call)

파라미터가 없는 도구(`getCurrentTime`)를 호출할 때, LLM이 불필요한 인자를 생성하지 않고 함수를 실행하는지 확인합니다.

```bash
curl -X POST http://localhost:8080/api/function/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "지금 몇 시야?"}'

# 예상 응답: "현재 시간은 2026년 03월 31일 22시 40분입니다."
```

### 3️⃣ 복합 조건 추론 (Multi-Step Reasoning)

하나의 질문에 대해 **동일한 함수를 여러 번 호출**하거나, **여러 함수를 조합**하여 답변을 생성하는 고난도 테스트입니다.

- **동작**: `getWeather("서울")` 호출 → `getWeather("부산")` 호출 → 두 결과값 비교 연산 수행.

```bash
curl -X POST http://localhost:8080/api/function/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "서울과 부산 날씨 비교해줘"}'

# 예상 응답: "서울은 15도로 맑고, 부산은 18도로 흐립니다. 부산이 서울보다 3도 더 따뜻합니다."
```

<aside>
💡

**검증 포인트 (Checklist)**

실습을 진행하며 다음 사항을 로그를 통해 확인해 보세요:

1. **연쇄 호출 (Chaining)**: 복합 질문 시 LLM이 한 번의 요청으로 두 번 이상의 Tool을 호출하는가?
2. **자연어 생성 품질**: Tool에서 반환된 날씨/계산기 데이터를 바탕으로 사용자 친화적인 문장을 생성하는가?
3. **에러 핸들링**: "0으로 나누기" 같은 잘못된 요청 시 Advisor나 Tool에서 발생한 예외를 AI가 어떻게 설명하는가?

</aside>