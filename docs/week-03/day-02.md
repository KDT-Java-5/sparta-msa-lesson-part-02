# 2일차. **LLM과 대화하기**

### **이번에 배울 것**

<aside>
❗

이번 과정에서는 단순한 API 호출을 넘어, 실제 프로젝트 환경에서 강력한 지능을 갖춘 생성형 AI 서비스를 구축하는 역량을 배양합니다.

Spring AI 프레임워크의 핵심인 `ChatClient` 활용법부터 프로젝트 환경 설정, 그리고 사용자와의 연속적인 대화를 가능하게 하는 히스토리 관리 (Context)와
실시간 응답 기술인 스트리밍 (Streaming)까지 단계별로 깊이 있게 파고듭니다.

단순한 기능 구현을 넘어, 실제 비즈니스 로직에 AI를 녹여내어 맥락 있는 답변을 제공하고 상품 리뷰의 만족도까지 평가할 수 있는 완성도 높은 RESTful API 챗봇을
구현하며, Spring Boot 기반의 AI 통합 역량을 확실히 내 것으로 만듭니다.

</aside>

## **1. Spring AI로 LLM과 대회하기**

LLM과의 대화는 단순히 텍스트를 주고받는 것이 아니라, 세 가지 서로 다른 역할 (Role)이 정교하게 맞물려 돌아가는 과정입니다. 각 메시지의 특성을 이해해야 효율적이고 똑똑한
AI 서비스를 설계할 수 있습니다.

### LLM 대화의 3가지 Message Types

#### **1. System Message (지시서)**

- **역할:** AI의 페르소나 (정체성)와 **행동 규칙**을 정의합니다.
- **특징:** 사용자 화면에는 노출되지 않는 '비밀 지침'입니다.
    - 대화의 전체적인 톤앤매너와 제약 사항을 결정합니다.

```java
// 1. AI의 정체성 설정 (System)
Message systemMessage = new SystemMessage("당신은 요리 전문가입니다.");
```

#### **2. User Message (사용자 입력)**

- **역할:** 사용자가 AI에게 던지는 **실제 질문이나 명령**입니다.
- **특징:** 대화의 흐름을 주도하며, 매번 새로운 입력을 생성합니다.

```java
// 2. 사용자의 첫 번째 질문 (User)
Message userMessage1 = new UserMessage("김치찌개 맛있게 만드는 비법이 뭐야?");
```

#### **3. Assistant Message (AI의 기억)**

- **역할:** AI가 이전에 내놓은 **응답 결과**입니다.
- **특징:** 다음 질문에 답변할 때 '과거에 내가 뭐라고 했더라?'를 참조하는 근거가 됩니다.
    - 대화 이력 (History)을 구성하는 핵심 요소입니다.

```java
// 3. AI의 이전 응답 (Assistant) - 이 내용이 있어야 AI가 과거 답변을 기억함
Message assistantMessage = new AssistantMessage("김치찌개의 비법은 충분히 볶은 김치와 쌀뜨물입니다.");
```

| **구분**       | **System**                | **User**              | **Assistant**              |
|----------------|---------------------------|-----------------------|----------------------------|
| **핵심 목적**  | 가이드라인 및 정체성 부여 | 문제 해결 요청 (질문) | 답변 제공 및 문맥 유지     |
| **가시성**     | 사용자에게 숨겨짐         | 사용자에게 보임       | 사용자에게 보임            |
| **위치**       | 대화의 최상단 (고정)      | 대화 중간중간         | 대화 중간중간              |
| **영향력**     | 대화 전체 (Global)        | 해당 질문 (Local)     | 직전 문맥 (Context)        |
| **비용(토큰)** | 매 요청 시 포함됨         | 매 요청 시 포함됨     | 히스토리에 포함되어 누적됨 |

#### 대화 히스토리의 구조

대화가 길어질수록 AI에게 보내는 메시지 뭉치는 다음과 같이 눈덩이처럼 불어납니다.

- **1회차 요청:** `[System]` + `[User 1]` → 응답: `[Assistant 1]`
- **2회차 요청:** `[System]` + `[User 1]` + `[Assistant 1]` + `[User 2]` → 응답: `[Assistant 2]`
- **3회차 요청:** `[System]` + `[User 1]` + `[Assistant 1]` + `[User 2]` + `[Assistant 2]` +
  `[User 3]` → 응답: `[Assistant 3]`

```java
public class ChatService {

    private final ChatModel chatModel;

    public void startConversation() {
        // 1. AI의 정체성 설정 (System)
        Message systemMessage = new SystemMessage("당신은 요리 전문가입니다.");

        // 2. 사용자의 첫 번째 질문 (User)
        Message userMessage1 = new UserMessage("김치찌개 맛있게 만드는 비법이 뭐야?");

        // 3. AI의 이전 응답 (Assistant) - 이 내용이 있어야 AI가 과거 답변을 기억함
        Message assistantMessage = new AssistantMessage("김치찌개의 비법은 충분히 볶은 김치와 쌀뜨물입니다.");

        // 4. 사용자의 두 번째 질문 (User) - 맥락이 필요한 질문
        Message userMessage2 = new UserMessage("그럼 된장찌개는?");

        // 5. 전체 메시지를 리스트로 묶어서 전송
        // AI는 assistantMessage를 보고 "아, 김치찌개 비법을 알려줬으니 이번엔 된장찌개 비법을 묻는구나"라고 이해합니다.
        List<Message> history = List.of(
            systemMessage,
            userMessage1,
            assistantMessage,
            userMessage2
        );

        chatModel.call(new Prompt(history));
    }
}
```

<aside>
💡

**만약 `Assistant 2`만 보내면?**
AI는 `User 1`이나 `Assistant 1`에서 했던 약속이나 정보를 기억하지 못합니다. 예를 들어 "내 이름은 유진호야"라고 1회차에 말했다면, 3회차에
`Assistant 2`만 보냈을 때 AI는 당신의 이름을 모릅니다.

</aside>

### LLM의 기억 저장소 Context & Context Window

LLM이 사용자의 이름을 기억하고, 긴 문서를 분석하며, 복잡한 지시를 따를 수 있는 이유는 무엇일까요? 그 핵심 비밀은 바로 'Context'라는 개념에 있습니다.

#### 1. Context (컨텍스트)란 무엇인가?

Context (문맥)는 LLM이 응답을 생성하기 위해 '지금 당장 머릿속에 올려놓고 처리하는 모든 정보'를 말합니다. AI는 이 정보를 바탕으로 대화의 흐름을 파악하고 답변의
방향을 결정합니다.

#### 2. Context의 3가지 핵심 역할

**① 대화의 연속성 유지 (Conversation History)**

AI는 이전 대화 내용을 Context에 포함하여 전달받기 때문에 "내 이름이 뭐였지?" 같은 대명사나 생략된 질문을 이해할 수 있습니다.

```java
List<Message> conversationHistory = List.of(
    new UserMessage("안녕, 내 이름은 유진호라고 해."),    // 100 토큰
    new AssistantMessage("안녕하세요 유진호님!"),       // 50 토큰
    new UserMessage("내가 좋아하는 음식은 TACO야"),      // 80 토큰
    new AssistantMessage("TACO를 좋아하시는군요!"),     // 60 토큰
    new UserMessage("내 이름이 뭐였지?")                // 50 토큰
);
// 총 Context 사용량: 340 토큰 -> AI는 "유진호님입니다"라고 답변 가능
```

**② 외부 지식 참조 (RAG 패턴)**

AI가 학습하지 않은 최신 정보나 특정 문서를 Context에 넣어주면, AI는 이를 '참고 자료'로 활용해 답변합니다.

```java
// Context 구성 = 문서(5,000) + 질문(20) = 5,020 토큰
// AI는 학습 데이터에 없는 내용도 문서 전체를 참조하여 정확히 답변합니다.

@GetMapping("/ask")
public String askWithDocument() {
    String document = "Spring AI는 Spring 생태계의 AI 통합 프레임워크입니다. " +
        "주요 기능으로는 ChatClient, Vector Database 통합 등이 있습니다."; // 참고할 문서
    String userQuestion = "Spring AI의 핵심 기능은?";

    // 1. 프롬프트 템플릿 생성 (문서와 질문을 결합)
    String combinedPrompt = String.format(
        "아래 제공된 [문서] 내용을 바탕으로 질문에 답하세요.\n\n" +
            "[문서]\n%s\n\n" +
            "[질문]\n%s",
        document, userQuestion
    );

    // 2. AI에게 전송
    return chatClient.prompt()
        .user(combinedPrompt)
        .call()
        .content();
}
```

**③ 예시를 통한 학습 (Few-Shot Learning)**

질문을 던지기 전에 "질문-답변"의 예시를 몇 가지 Context에 넣어주면, AI는 그 패턴을 복사하여 훨씬 더 정확한 형식으로 답변합니다.

```java
public String askWithFewShot() {
    // 1. AI에게 학습시킬 '예시(Shot)'들을 구성
    String fewShotExamples = """
        질문: '사과'를 영어로 번역하고 짧은 예문을 만들어줘.
        답변: Apple (I like to eat a fresh apple in the morning.)
        
        질문: '바나나'를 영어로 번역하고 짧은 예문을 만들어줘.
        답변: Banana (Monkey is eating a yellow banana.)
        """;

    // 2. 실제 질문
    String userQuestion = "'수박'을 영어로 번역하고 짧은 예문을 만들어줘.";

    // 3. 예시 + 질문을 합쳐서 전달 (AI는 앞선 패턴을 그대로 복제함)
    String combinedPrompt = fewShotExamples + "\n질문: " + userQuestion + "\n답변:";

    return chatClient.prompt()
        .user(combinedPrompt)
        .call()
        .content();
    // 예상 답변: Watermelon (We enjoyed a sweet watermelon at the beach.)
}
```

| **기술**     | **핵심 아이디어**             | **사용 목적**             |
|--------------|-------------------------------|---------------------------|
| **RAG**      | "이 **정보**를 읽고 답변해줘" | 지식(Knowledge) 보완      |
| **Few-Shot** | "이 **형식**대로 답변해줘"    | 스타일/형식(Pattern) 보완 |

<aside>
💡

**Context Window가 커지면 생기는 변화**

그릇 (Context Window)이 커지면 (예: Gemini 1.5 Pro의 2M 토큰), 대화가 아주 길어져도 **내용이 잘리지 않고 끝까지 들어갑니다.**

- **해소되는 점:** 대화가 길어져서 예전 내용을 잊어버리는 '기억 상실' 현상은 확실히 줄어듭니다. 1회차에 말한 "내 이름은 유진호야"를 100회차 대화에서도 여전히 기억할
  수 있습니다.
- **유지되는 점:** 질문하신 "메시지 뭉치가 눈덩이처럼 불어나는 구조" 그 자체는 변하지 않습니다. 오히려 더 큰 눈덩이를 담을 수 있게 된 것뿐입니다.

**여전히 남는 '진짜' 문제들**

그릇이 크다고 무조건 다 담아서 보내면 다음과 같은 **실무적 한계**에 부딪힙니다.

**① 기하급수적인 비용 상승**

LLM API는 '누적된 전체 토큰'에 대해 비용을 매깁니다.

- 1회차: 100 토큰 비용
- 2회차: 100 (과거) + 100 (신규) = 200 토큰 비용
- 10회차: 1,000 토큰 비용
- **결과:** 대화가 길어질수록 똑같은 질문을 해도 비용이 10배, 100배로 뜁니다. Context Window가 아무리 커도 지갑 사정은 고려해주지 않습니다.

**② 응답 속도 저하 (Latency)**

AI가 읽어야 할 '눈덩이'가 커질수록, 전체 문맥을 파악하고 답변을 생성하는 데 걸리는 시간이 길어집니다. 실시간 챗봇에서 사용자 경험을 해치는 요인이 됩니다.

**③ 주의력 분산 (Lost in the Middle)**

최근 연구에 따르면, Context Window가 너무 크면 AI가 **중간에 있는 정보**를 무시하거나 집중하지 못하는 경향이 발생합니다. 데이터가 너무 많으면 오히려 답변의
정확도가 떨어질 수 있습니다.

해당 이슈를 해결하기 위해 슬라이딩 **윈도우 (Sliding Window)**나 **요약 (Summarization)** 기술이 필요한 것입니다.

- **슬라이딩 윈도우:** 눈덩이가 너무 커지면 뒤쪽 (오래된 것)을 떼어내서 크기를 일정하게 유지합니다.
- **요약:** 커다란 눈덩이를 꽉꽉 압축해서 작은 얼음덩어리 (요약본)로 만든 뒤, 그 위에 새 눈을 쌓습니다

</aside>

### LLM의 화폐: Token Usage 이해하기

LLM 서비스에서 토큰 (Token)은 AI가 텍스트를 읽고 쓰는 기본 단위이자, 곧 비용 (Money)을 의미합니다. 효율적인 서비스를 만들려면 토큰이 어떻게 계산되고 소비되는지
정확히 알아야 합니다.

**Token (토큰)이란 무엇인가?**

AI는 문장을 글자 단위가 아닌 **토큰**이라는 덩어리로 쪼개서 처리합니다.

- **단위 기준:** 영어: 약 0.75단어 당 1토큰
    - 한글: 한 글자당 약 1~2토큰 (한글이 영어보다 토큰 소모가 큽니다)
- **체감 수치:** "안녕하세요"는 약 3~5토큰이며, 한글 10,000자 정도를 처리하는 데 약 $15 수준의 비용이 발생할 수 있습니다 (모델에 따라 상이).

<aside>
💡

**비용 계산의 핵심 공식**

대부분의 AI 서비스 (Claude, GPT 등)는 입력 (Prompt)과 출력 (Generation)의 단가를 다르게 책정합니다. 보통 출력 단가가 입력보다 3~5배 더
비쌉니다.

**💰 예상 비용 공식**`(입력 토큰 수 × 입력 단가) + (출력 토큰 수 × 출력 단가) = 총 비용`

</aside>

**개발자를 위한 토큰 계산기**

Spring AI 환경에서 `JTokkit` 라이브러리를 사용해 전송 전 토큰 수를 미리 예측해볼 수 있습니다.

```java
package com.sparta.msa.lesson.global.utils;

import java.util.List;

import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.stereotype.Component;

@Component
public class TokenCounter {

    // 단일 문자열의 토큰 수 계산
    public static Integer countTokens(String text) {
        // JTokkit: OpenAI의 tiktoken 라이브러리를 자바에서 쓸 수 있게 만든 도구
        TokenCountEstimator estimator = new JTokkitTokenCountEstimator();
        return estimator.estimate(text);
    }

    // 대화 히스토리 등 여러 메시지의 총 토큰 수 계산
    public static Integer countTokens(List<String> messages) {
        TokenCountEstimator estimator = new JTokkitTokenCountEstimator();
        return messages.stream()
            .mapToInt(estimator::estimate)
            .sum();
    }
}
```

#### 1. 토큰의 3가지 종류

**① Prompt Tokens (입력 토큰)**

**Prompt Tokens**는 사용자가 LLM (대규모 언어 모델)에게 전달한 **모든 입력 데이터의 총합**을 의미합니다. AI가 답변을 생성하기 위해 "읽어 들여야 하는
모든 정보"라고 이해하면 쉽습니다.

**구성 요소**

입력 토큰은 단순히 현재의 질문만 계산하는 것이 아니라, 다음의 세 가지 요소를 모두 합산합니다.

```text
Prompt Tokens = System Message + 이전 대화 히스토리 + 현재 질문
```

- **System Message:** AI의 역할과 규칙 (예: "당신은 전문 코딩 강사입니다.")
- **대화 히스토리:** 이전 단계에서 주고받은 모든 `User` 메시지와 `Assistant` 메시지
- **현재 질문:** 사용자가 지금 막 입력한 내용

| **구분**         | **대화 내용**                                                                                                                       | **계산식**      | **결과**      |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------|-----------------|---------------|
| **첫 번째 대화** | **Sys :** "친절한 AI입니다" (10)<br>**User :** "안녕하세요" (3)                                                                     | 10 + 3          | **13** tokens |
| **두 번째 대화** | **Sys :** (10)<br>**User (전) :** (3)<br>**Asst (전) :** "반가워요! 무엇을 도와드릴까요?" (12)<br>**User (현) :** "날씨 알려줘" (4) | 10 + 3 + 12 + 4 | **29** tokens |

- **비용의 주범:** 대화가 길어질수록 과거 내역이 계속 누적되어 전달되므로, **비용이 기하급수적으로 증가**합니다.
- **컨텍스트 제한:** 입력 토큰이 모델의 컨텍스트 윈도우 (Context Window)를 초과하면 가장 오래된 기억부터 삭제되거나 오류가 발생합니다.
- **성능 영향:** 입력 데이터가 너무 방대하면 AI가 핵심 질문을 놓치거나 응답 속도가 느려질 수 있습니다.

```java
// 1. AI에게 요청을 보냄
ChatResponse response = chatClient.prompt()
        .user("안녕, 반가워!")
        .call()
        .chatResponse(); // 여기서 ChatResponse 객체가 반환됩니다.

// 2. 답변 내용만 필요할 때
String content = response.getResult().getOutput().getText();

// 3. 토큰 사용량이 궁금할 때 (질문하신 코드)
Integer promptTokens = response.getMetadata().getUsage().getPromptTokens();
log.

info("입력 토큰 수: {}",promptTokens);
```

**② Generation Tokens (출력 토큰)**

**Generation Tokens**는 사용자의 질문에 대해 LLM이 **새롭게 생성해낸 답변 (Output)의 총량**을 의미합니다. 기술 문서나 API에 따라
'Completion Tokens'라고도 부릅니다.

**포함 내용**

입력 토큰과 달리 과거의 내역은 포함하지 않으며, 오직 **현재 응답**만을 계산합니다.

```text
Generation Tokens = 현재 생성된 Assistant 응답의 토큰 수
```

사용자가 코드 작성을 요청했을 때 AI가 답변한 분량을 측정합니다.

<aside>
💡

- **User:** "파이썬으로 Hello World 출력하는 코드 작성해줘."
- **Assistant:**

  > "파이썬으로 Hello World를 출력하는 코드는 다음과 같습니다:
  >
  >
  > `print("Hello, World!")`
  >
  > 이 코드는 print () 함수를 사용하여 문자열을 출력합니다."

>

- **결과:** 위 답변의 길이에 따라 **약 50 tokens** 내외가 Generation Tokens로 잡힙니다.

</aside>

- **길이에 비례하는 비용:** 답변이 상세하고 길어질수록 출력 토큰이 늘어나며, 보통 **입력 토큰보다 단가가 비싸기 때문에** 비용 관리에 유의해야 합니다.
- **`maxTokens`로 제어 가능:** 응답이 너무 길어져 비용이 폭주하거나 타임아웃이 발생하는 것을 막기 위해, 개발자가 최대 출력 길이를 강제로 제한할 수 있습니다.
- **스트리밍 (Streaming) 시 실시간 증가:** 실시간으로 답변이 출력되는 방식에서는 한 글자씩 생성될 때마다 이 수치가 실시간으로 합산됩니다.

```java
// AI에게 요청을 보냄
ChatResponse response = chatClient.prompt()
        .user("안녕, 반가워!")
        .call()
        .chatResponse(); // 여기서 ChatResponse 객체가 반환됩니다.

// 응답 결과에서 생성(출력) 토큰 수 추출
Integer generationTokens = response.getMetadata().getUsage().getGenerationTokens();

log.

info("=== 출력 분석 ===");
log.

info("AI가 생성한 토큰 수: {} tokens",generationTokens);

// 만약 설정한 maxTokens에 걸려 답변이 잘렸는지 확인하고 싶다면?
String finishReason = response.getResult().getMetadata().getFinishReason();
if("LENGTH".

equals(finishReason)){
    log.

warn("⚠️ 답변이 너무 길어 maxTokens 제한에 의해 끊겼습니다.");
}
```

**③ Total Tokens (전체 토큰)**

**Total Tokens**는 단일 API 호출에서 소모된 **입력 (Prompt) 토큰과 출력 (Generation) 토큰을 모두 합산한 수치**입니다. 서비스 제공업체
(Anthropic, OpenAI 등)가 과금을 산정하는 **최종 결제 기준**이 됩니다.

**계산 공식** : 가장 단순하지만 가장 중요한 공식입니다.

```text
Total Tokens = Prompt Tokens(질문+맥락) + Generation Tokens(답변)
```

<aside>
💡

**실전 예시 (총 토큰 계산)**

사용자가 질문을 던지고 AI가 답변을 마쳤을 때의 기록입니다.

- **입력 (Prompt):** 시스템 설정 + 이전 대화 + 현재 질문 = **100 tokens**
- **출력 (Generation):** AI가 새로 작성한 답변 내용 = **50 tokens**
- **결과:** **Total Tokens = 150 tokens**

</aside>

**비용 산정의 원리 (Billing)**

단순히 전체 토큰 수만 중요한 것이 아니라, **입력과 출가의 단가 차이**를 이해하는 것이 실제 운영비 계산에 필수적입니다.

- **비율의 차이:** 일반적으로 **출력 (Generation) 단가**가 입력 (Prompt) 단가보다 **약 3~5배가량 비쌉니다.**
- **최종 비용 공식:**

    ```text
    Total Cost = (Prompt Tokens × 입력 단가) + (Generation Tokens × 출력 단가)
    ```

    1. **예산 관리의 척도:** `TotalTokens`를 모니터링하면 우리 서비스가 하루에 얼마만큼의 비용을 지출하고 있는지 정확히 추정할 수 있습니다.
    2. **효율성 지표:** 같은 질문에 대해 `TotalTokens`가 너무 높게 나온다면, 프롬프트가 너무 길거나 AI가 불필요하게 서술하고 있다는 신호입니다.
    3. **데이터 타입 주의:** Spring AI 라이브러리에서 `getTotalTokens()`는 누적 수치가 커질 수 있음을 고려하여 **Long 타입**으로
       반환됩니다.

#### **2. Token 최적화 방법**

**① System Message 최적화**

System Message는 모든 API 호출마다 포함되므로, 단 몇 줄을 줄이는 것만으로도 누적 절감 효과가 엄청납니다.

- **❌ 비효율적:** "당신은 매우 친절하고 상냥하며... 이해하기 쉽도록 명확하고 간결하게... (미사여구 남발)" **(약 150 tokens)**

    ```java
    String systemMessage = """
        당신은 매우 친절하고 상냥하며 사용자를 배려하는 AI 어시스턴트입니다.
        사용자의 질문에 항상 정중하고 예의바르게 답변해야 하며,
        사용자가 이해하기 쉽도록 명확하고 간결하게 설명해주세요.
        또한 사용자의 감정을 고려하여 공감하는 태도를 보여주세요.
        전문적이면서도 친근한 톤을 유지하며,
        필요한 경우 예시를 들어 설명해주세요.
        """;
    // 예상 토큰: 약 120-150 tokens
    
    ```

- **✅ 효율적:** "친절한 AI 어시스턴트. 명확하고 간결하게 답변." **(약 15 tokens)**

    ```java
    String systemMessage = """
        친절한 AI 어시스턴트. 명확하고 간결하게 답변.
        """;
    // 예상 토큰: 약 10-15 tokens
    // 절약: 약 110-135 tokens (90% 감소!)
    
    ```

  | 구분 | 긴 System Message | 짧은 System Message | 절약 효과 |
            | --- | --- | --- | --- |
  | 토큰/요청 | 150 tokens | 15 tokens | 135 tokens |
  | 1000회 호출 | 150,000 tokens | 15,000 tokens | 135,000 tokens |
  | 비용 (Claude Sonnet 4.5) | $0.45 | $0.045 | **$0.405 절약** |

**② 대화 히스토리 관리 전략 3가지**

LLM 서비스에서 대화가 길어질수록 발생하는 **입력 토큰 증가**와 **비용 폭주**를 막기 위한 필수 기술입니다.

**전략 A: 🪟 슬라이딩 윈도우 (Sliding Window)**

**"가장 최근의 대화만 기억하자"**
전체 대화 내용은 DB에 저장하되, AI에게는 **최근 N개**의 메시지만 전달하여 토큰 소모를 일정하게 유지하는 방식입니다.

```java

@Service
public class SlidingWindowChatService {

    // 최대 유지할 메시지 수 (예: 최근 10개 질문/답변 쌍 = 20개 메시지)
    private static final int MAX_HISTORY_MESSAGES = 20;

    public ChatResponse chat(String question, String conversationId) {
        List<Message> fullHistory = loadHistoryFromDB(conversationId);

        // 1. 최근 N개 메시지만 슬라이싱
        List<Message> recentHistory = getRecentMessages(fullHistory, MAX_HISTORY_MESSAGES);

        // 2. AI 호출 (최근 문맥만 포함)
        var response = chatClient.prompt()
            .messages(recentHistory)
            .user(question)
            .call()
            .chatResponse();

        return processResponse(response, fullHistory);
    }

    private List<Message> getRecentMessages(List<Message> history, int max) {
        if (history == null || history.size() <= max)
            return history;
        return history.subList(history.size() - max, history.size());
    }
}
```

| 대화 턴 수 | 전체 히스토리 사용 | 최근 20개만 사용 | 절약 효과             |
|------------|--------------------|------------------|-----------------------|
| 100턴      | 5,000 tokens       | 1,000 tokens     | **4,000 tokens/요청** |
| 200턴      | 10,000 tokens      | 1,000 tokens     | **9,000 tokens/요청** |

**전략 B: 📝 요약 기반 압축 (Summarization)**

**"옛날 이야기는 한 줄로 요약하자"**
오래된 대화들을 AI를 통해 짧게 요약하여 **단일 System Message**로 변환하고, 최근 대화만 원본으로 유지하는 방식입니다.

```java

@Service
public class SummarizedChatService {

    private static final int SUMMARY_THRESHOLD = 30; // 30개가 넘어가면 압축 시작

    public ChatResponse chat(String question, String conversationId) {
        List<Message> history = loadHistoryFromDB(conversationId);

        if (history.size() > SUMMARY_THRESHOLD) {
            // 1. 오래된 대화(0~20번째)를 요약하여 교체
            history = summarizeOldHistory(history);
        }

        return chatClient.prompt()
            .messages(history)
            .user(question)
            .call()
            .chatResponse();
    }

    private List<Message> summarizeOldHistory(List<Message> history) {
        List<Message> toSummarize = history.subList(0, 20);
        List<Message> keepAsIs = history.subList(20, history.size());

        // AI에게 요약 요청 (내부 프롬프트 활용)
        String summary = chatClient.prompt()
            .user("다음 대화 내역을 200자 이내로 핵심만 요약해: " + convertToText(toSummarize))
            .call().content();

        List<Message> optimized = new ArrayList<>();
        optimized.add(new SystemMessage("이전 대화 요약: " + summary));
        optimized.addAll(keepAsIs);
        return optimized;
    }
}
```

| 대화 턴 수 | 전체 히스토리 | 요약 + 최근 30개 | 절약 비율    |
|------------|---------------|------------------|--------------|
| 50턴       | 2,500 tokens  | 1,700 tokens     | **32% 절약** |
| 100턴      | 5,000 tokens  | 1,900 tokens     | **62% 절약** |

**전략 C: ❗ 중요도 기반 필터링 (Smart Filtering)**

**"중요한 정보는 버리지 말고 골라내자"**
단순히 순서대로 자르는 것이 아니라, **비즈니스적으로 중요한 키워드**(결제, 주소, 개인정보 등)가 포함된 메시지를 우선적으로 선별하는 방식입니다.

```java

@Service
public class SmartHistoryService {

    public List<Message> filterHistory(List<Message> history) {
        List<Message> important = history.stream()
            .filter(this::isImportant) // 핵심 로직
            .collect(Collectors.toList());

        // 문맥 유지를 위해 최소한의 최근 메시지(예: 10개)는 강제로 합침
        if (important.size() < 10) {
            important.addAll(getRecentMessages(history, 10));
        }
        return important.stream().distinct().toList();
    }

    private boolean isImportant(Message msg) {
        String content = msg.getText().toLowerCase();
        return content.contains("결제") || content.contains("주소") || content.length() > 100;
    }
}
```

**✅ 필터링 기준 (Priority)**

- **중요 키워드:** "결제", "배송", "중요", "반드시"
- **데이터 타입:** 이름, 전화번호, 이메일 주소 등
- **분량:** 상세 설명이 담긴 긴 메시지 (100자 이상)

**③ maxTokens 동적 설정 전략**

단순히 모든 질문에 동일한 제한을 두는 것이 아니라, 사용자의 질문 의도 (Intent)를 파악하여 AI가 사용할 '연필의 길이'를 조절하는 기술입니다.

**"질문 유형에 따른 최적의 응답 길이”**

모든 질문에 큰 `maxTokens`를 할당하면 시스템 자원이 낭비되고 비용 예측이 어려워집니다. 질문의 성격에 따라 필요한 만큼만 스마트하게 할당하는 것이 핵심입니다.

| **질문 유형**  | **의도 파악 키워드**       | **권장 maxTokens** | **예상 실제 사용** |
|----------------|----------------------------|--------------------|--------------------|
| **단답형**     | 맞아, 인가요, 언제, 어디   | **100**            | 50 ~ 100           |
| **설명 요청**  | 설명, 알려줘, 뭐야, 무엇   | **500**            | 300 ~ 500          |
| **코드 생성**  | 코드, 구현, 작성, 프로그램 | **2,000**          | 1,000 ~ 2,000      |
| **긴 글 작성** | 에세이, 보고서, 써줘       | **3,000**          | 2,000 ~ 3,000      |
| **기본값**     | 기타 일반 질문             | **1,000**          | 500 ~ 1,000        |

```java

@Service
public class DynamicTokenService {

    private final ChatClient chatClient;

    public ChatResponse chatWithDynamicTokens(String question, String conversationId) {
        // 1. 질문의 의도를 분석하여 적절한 토큰 수 결정
        int maxTokens = determineMaxTokens(question);

        // 2. 결정된 maxTokens를 옵션에 적용하여 API 호출
        var response = chatClient.prompt()
            .user(question)
            .options(AnthropicChatOptions.builder()
                .maxTokens(maxTokens)
                .build())
            .call()
            .chatResponse();

        return response;
    }

    private int determineMaxTokens(String question) {
        String lowerQ = question.toLowerCase();

        // 패턴 매칭을 통한 의도 파악
        if (lowerQ.matches(".*(맞아|맞나요|인가요|예스|노|몇|얼마|언제|누가|어디).*") && question.length() < 30) {
            return 100; // 단답형
        }
        if (lowerQ.contains("코드") || lowerQ.contains("구현") || lowerQ.contains("프로그램")) {
            return 2000; // 개발 관련
        }
        if (lowerQ.contains("작성해줘") || lowerQ.contains("보고서") || lowerQ.contains("글")) {
            return 3000; // 창작/보고서
        }
        if (lowerQ.contains("설명") || lowerQ.contains("알려줘") || lowerQ.contains("뭐야")) {
            return 500; // 정보 요약
        }

        return 1000; // 기본값
    }
}

```

**💰 절약 효과 시뮬레이션**

단답형 질문 (평균 100 토큰 소모) 100개를 처리할 때의 차이입니다.

- **❌ 고정 방식 (maxTokens=2,000):** 필요 이상으로 높은 제한을 두어 시스템 예약 리소스와 비용이 낭비됩니다. → **예상 비용 $2.85**
- **✅ 동적 방식 (maxTokens=100):** 실제 필요한 양에 맞춰 최적화합니다. → **예상 비용 $0.15**

**결과:** 약 **95%의 비용 절감 효과**를 기대할 수 있습니다.

<aside>
💡

- **안전 장치:** 너무 낮게 설정하면 답변이 중간에 잘릴 수 있습니다 (`FinishReason: LENGTH`). 서비스 성격에 따라 `determineMaxTokens`
  수치를 10~20% 정도 여유 있게 설정하는 것이 좋습니다.
- **사용자 경험:** 답변이 잘릴 경우 사용자에게 "내용이 길어 중간에 끊겼습니다. 계속 진행할까요?"와 같은 가이드를 제공하는 로직을 추가하면 좋습니다.

</aside>

---

## 2. 실습 : Spring AI 기반의 멀티 세션 챗봇 API 서버 구축

대화의 맥락을 기억하고 실시간으로 응답하는 고성능 RESTful API 서버를 밑바닥부터 구현합니다. 단순 호출을 넘어 **멀티 세션 관리와 토큰 모니터링**까지 포함된
엔터프라이즈급 설계를 학습합니다.

### Configuration 설정

애플리케이션 전역에서 사용할 AI의 '기본 인격'과 '공통 규칙'을 정의하는 단계입니다. `application.yml`에 정의된 모델 설정을 바탕으로 실제 대화를 수행할 객체를
생성합니다.

**GeminiChatConfig :** `ChatClient.Builder`를 주입받아, 모든 대화 요청에 공통적으로 적용될 시스템 프롬프트 (System Prompt)를
구성합니다.

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // yml에 등록된 설정을 바탕으로 ChatClient를 생성하며,
        // 공통 지침(defaultSystem)만 여기서 추가합니다.
        return builder
            .defaultSystem("""
                당신은 친절하고 도움이 되는 AI 어시스턴트입니다.
                사용자의 질문에 정확하고 이해하기 쉽게 답변해주세요.
                """)
            .build();
    }
}
```

**1. `defaultSystem()`: 전역 페르소나 부여**

- **역할:** 모든 대화 요청의 맨 앞에 자동으로 붙는 "지시 사항"입니다.
- **이점:** 개별 서비스 로직에서 매번 "너는 AI 비서야"라고 말해줄 필요가 없습니다.
- **일관성:** 어떤 기능 (채팅, 요약, 번역)에서 호출하더라도 AI는 동일한 말투와 규칙을 따르게 됩니다.

**2. `ChatClient.Builder` 활용**

- Spring Boot가 `application.yml` 설정을 읽어 미리 준비해둔 `Builder`를 주입받습니다.
- 이 방식은 내부적으로 API 키, 베이스 URL, 선택된 모델명 (`gemini-2.0-flash-lite` 등)을 자동으로 포함하고 있어 코드가 매우 간결해집니다.

**3. 유지보수의 편의성**

- AI의 말투를 바꾸거나 특정 주의사항을 추가하고 싶을 때, 비즈니스 로직 (Service)을 수정할 필요 없이 이 설정 파일의 `defaultSystem` 내용만 수정하면
  됩니다.

### 데이터 모델링: Request & Response 설계

**Request :** 사용자의 입력 메시지와 대화의 연속성을 위한 세션 ID를 전달받습니다.

```java

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContextChatRequest {

    // 사용자가 AI에게 보내는 질문 또는 명령 텍스트
    String message;

    // 대화의 연속성을 유지하기 위한 고유 세션 ID
    // (기존 대화를 이어갈 경우 필수, 처음 시작할 경우 null 가능)
    String conversationId;

}
```

**Response :** AI의 응답뿐만 아니라 생성 시각, 토큰 사용량 등 메타데이터를 포함하여 클라이언트에게 제공합니다.

```java

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContextChatResponse {

    // AI가 생성한 최종 답변 텍스트
    String message;

    // 대화의 연속성을 식별하기 위한 고유 세션 ID (이후 요청 시 이 ID를 전달하여 대화 문맥을 유지함)
    String conversationId;

    // 서버에서 응답이 생성된 시각
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp;

    // 이번 API 호출에서 발생한 상세 토큰 사용량 정보
    TokenUsage tokenUsage;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TokenUsage {

        // 질문 및 과거 대화 내역을 포함한 입력(Input) 토큰 수
        Integer promptTokens;

        // AI가 답변을 생성하며 소비한 출력(Output) 토큰 수
        Integer completionTokens;

        // 입력과 출력을 합산한 총 토큰 수 (실제 과금 및 비용 계산의 기준)
        Integer totalTokens;
    }
}
```

### 비즈니스 로직 구현 : Service 레이어

AI 모델과의 통신을 담당하며, 특히 **'기억력이 없는' LLM에게 대화 문맥 (Context)을 제공**하기 위해 메모리 상에서 대화 이력을 관리하는 핵심 로직을 포함합니다.

```java

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiChatService {

    // GeminiChatConfig에서 생성된 ChatClient 주입
    private final ChatClient chatClient;

    // 대화 이력을 관리하기 위한 저장소 (Key: conversationId, Value: 메시지 리스트)
    // 멀티 스레드 환경에서 안전하도록 ConcurrentHashMap 사용
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    // 기본 채팅 (히스토리 없음) 질문 하나에 답변 하나만 제공하며 맥락을 유지하지 않습니다.
    public ContextChatResponse chat(String question) {
        String responseContent = chatClient.prompt()
            .user(question)
            .call()
            .content();

        return ContextChatResponse.builder()
            .message(responseContent)
            .conversationId(UUID.randomUUID().toString()) // 새 세션 ID 부여
            .timestamp(LocalDateTime.now())
            .build();
    }

    // 대화 히스토리를 유지하는 채팅 (핵심 기능) 과거 대화 내용을 포함하여 질문을 던짐으로써 맥락 있는 답변을 유도합니다.
    public ContextChatResponse chatWithHistory(String question, String conversationId) {
        // ID가 없으면 새로 생성
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 기존 대화 이력을 가져오거나 새로 생성
        List<Message> history = conversations.getOrDefault(conversationId, new ArrayList<>());

        // 사용자 질문 추가
        UserMessage userMessage = new UserMessage(question);
        history.add(userMessage);

        try {
            // AI 호출: 지금까지의 history 전체를 메시지로 전달
            ChatResponse response = chatClient.prompt()
                .messages(history)
                .call()
                .chatResponse();

            String assistantResponse = response.getResult().getOutput().getText();

            // AI 답변을 히스토리에 추가
            AssistantMessage assistantMessage = new AssistantMessage(assistantResponse);
            history.add(assistantMessage);

            // 업데이트된 히스토리 저장
            conversations.put(conversationId, history);

            // 토큰 사용량 정보 추출 및 DTO 변환
            var usage = response.getMetadata().getUsage();
            ContextChatResponse.TokenUsage tokenUsage = ContextChatResponse.TokenUsage.builder()
                .promptTokens(usage.getPromptTokens().intValue())
                .completionTokens(usage.getCompletionTokens().intValue())
                .totalTokens(usage.getTotalTokens().intValue())
                .build();

            return ContextChatResponse.builder()
                .message(assistantResponse)
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .tokenUsage(tokenUsage)
                .build();

        } catch (Exception e) {
            log.error("AI 호출 중 오류 발생: {}", e.getMessage());
            throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        }
    }

    // 스트리밍 채팅 답변을 실시간으로 한 글자씩 끊어서 반환합니다.
    public Flux<String> chatStream(String question) {
        return chatClient.prompt()
            .user(question)
            .stream()
            .content();
    }

    // 모든 대화 메모리 초기화
    public void clearAll() {
        conversations.clear();
        log.info("모든 대화 세션 초기화 완료");
    }

}
```

**1. 대화 맥락 (Context) 유지 원리**

LLM은 자체적으로 과거 대화를 기억하지 못합니다.

- **해결책:** `List<Message>`에 `UserMessage`와 `AssistantMessage`를 순서대로 쌓은 뒤,
  `chatClient.prompt().messages(history)`를 통해 **지금까지 나눈 대화 전체를 다시 읽어주며** 질문하는 방식입니다.

**2. 왜 `ConcurrentHashMap`인가?**

일반 `HashMap`은 여러 사용자가 동시에 요청을 보낼 때 데이터가 깨지거나 오류가 발생할 수 있습니다. `ConcurrentHashMap`을 사용함으로써 서버가 여러 명의
대화를 동시에 안전하게 처리할 수 있도록 설계했습니다.

**3. 비용 추적 (Token Usage)**

`ChatResponse`의 메타데이터에서 토큰 정보를 추출하여 `ContextChatResponse` DTO에 담습니다. 이를 통해 클라이언트는 현재 대화가 얼마나 많은 비용
(토큰)을 소모하고 있는지 실시간으로 확인할 수 있습니다.

**4. 스트리밍 응답 (`Flux<String>`)**

`stream()` API를 사용하면 AI가 답변을 완성할 때까지 기다리지 않고, 생성되는 즉시 한 조각씩 클라이언트에 전달합니다. 이는 사용자 체감 대기 시간을 획기적으로
줄여줍니다.

### **REST API 구현 : Controller 레이어**

```java

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/gemini")
public class GeminiChatController {

    private final GeminiChatService chatService;

    // 단발성 채팅 (히스토리 없음) 맥락 유지 없이 일회성 질문에 대한 답변을 제공합니다.
    @PostMapping("/simple")
    public ApiResponse<ContextChatResponse> simpleChat(@RequestBody ContextChatRequest request) {
        return ApiResponse.ok(chatService.chat(request.getMessage()));
    }

    // 대화 히스토리 유지 채팅 (핵심 기능) conversationId를 통해 과거 대화 맥락을 포함한 답변을 제공합니다.
    @PostMapping
    public ApiResponse<ContextChatResponse> chat(@RequestBody ContextChatRequest request) {
        return ApiResponse.ok(
            chatService.chatWithHistory(request.getMessage(), request.getConversationId()));
    }

    // 스트리밍 채팅 (Server-Sent Events) 답변이 생성되는 대로 실시간으로 클라이언트에 전송합니다.
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ContextChatRequest request) {
        return chatService.chatStream(request.getMessage());
    }

    // 모든 대화 메모리 초기화
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteAllConversations() {
        chatService.clearAll();
        return ApiResponse.ok();
    }

}
```

**1. 상태 유지 (Stateful) vs 상태 비저장 (Stateless)**

- **Stateless (`/simple`)**: LLM은 기본적으로 이전 질문을 기억하지 못합니다. 단발성 질문에 유리하며 서버 리소스를 적게 소모합니다.
- **Stateful (`/`)**: `conversationId`를 키로 하여 서버 메모리나 DB에 이전 대화 (`Message` 리스트)를 저장하고, 매 요청마다 **전체
  히스토리 + 새로운 질문**을 함께 모델에 보냅니다. 이것이 챗봇이 "맥락"을 이해하는 원리입니다.

**2. 토큰 (Token) 관리의 변화**

최근 Spring AI 및 관련 라이브러리 (LangChain4j 등)는 업계 표준 용어에 맞춰 메서드명을 변경하고 있습니다.

- **Prompt Tokens**: AI에게 보낸 입력 메시지의 양.
- **Completion Tokens**: AI가 생성한 답변의 양.
- **Total Tokens**: 위 둘의 합계. 비용 계산 및 모델 제한 (Limit) 관리에 필수적입니다.

**3. 비동기 스트리밍 (Reactive Stream)**

- `Flux<String>`과 `MediaType.TEXT_EVENT_STREAM_VALUE`를 사용하면 AI가 답변을 한 자 한 자 생성할 때마다 클라이언트에게 즉시 전달할 수
  있습니다.
- 사용자 경험 (UX) 측면에서 전체 답변이 완성될 때까지 기다리는 지루함을 줄여줍니다.

### **Swagger API 테스트**

#### **1. 기본 채팅 (히스토리 없음)**

```
curl -X 'POST' \
  'http://localhost:8080/api/chat/gemini/simple' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "message": "안녕하세요!"
}'
```

```json
{
  "data": {
    "message": "안녕하세요! 무엇을 도와드릴까요? 😊",
    "conversationId": "8f1d670a-8b26-496d-9267-444b0c45d3be",
    "timestamp": "2026-03-24 16:26:13",
    "tokenUsage": null
  }
}
```

#### **2. 대화 히스토리 유지**

**첫 번째 메시지:**

```
curl -X 'POST' \
  'http://localhost:8080/api/chat/gemini' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "message": "Spring AI에 대해 설명해줘",
  "conversationId": null
}'
```

```json
{
  "data": {
    "message": "## Spring AI: Spring 프레임워크와 AI의 만남\n\nSpring AI...",
    "conversationId": "67cdbe2d-3b54-43ad-af5b-5a0008a56154",
    "timestamp": "2026-03-24 16:26:13",
    "tokenUsage": {
      "promptTokens": 39,
      "completionTokens": 996,
      "totalTokens": 1035
    }
  }
}
```

**두 번째 메시지 (같은 대화):**

```
curl -X 'POST' \
  'http://localhost:8080/api/chat/gemini' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "message": "방금 설명한 내용을 한줄로 요약해줘",
  "conversationId": "67cdbe2d-3b54-43ad-af5b-5a0008a56154"
}'
```

```json
{
  "data": {
    "message": "Spring AI는 Spring 프레임워크를 사용하여 AI 모델 및 서비스를 애플리케이션에 쉽고 효율적으로 통합할 수 있도록 돕는 프로젝트입니다.",
    "conversationId": "67cdbe2d-3b54-43ad-af5b-5a0008a56154",
    "timestamp": "2026-03-24 16:26:13",
    "tokenUsage": {
      "promptTokens": 1049,
      "completionTokens": 33,
      "totalTokens": 1082
    }
  }
}
```

#### **3. 스트리밍 채팅**

```
curl -X 'POST' \
  'http://localhost:8080/api/chat/gemini/stream' \
  -H 'accept: text/event-stream' \
  -H 'Content-Type: application/json' \
  -d '{
  "message": "긴 이야기를 들려줘"
}'
```

```
data:옛

data:날 옛날 아주 먼 옛날, 깊고 푸른 숲 속에 작은

data: 마을이 있었습니다. 그 마을에는 '엘라'라는 이름의 용감하고 호기심 많은 소녀가 살고 있었습니다. 엘라는 숲을 탐험하고 신비로운 생명체들을 만나는 것을 아주 좋아했습니다.
data:
data:어느

data: 날, 엘라는 숲 깊숙한 곳에서 반짝이는 무언가를 발견했습니다. 그것은 아름다운 나비였는데, 날개에는 별처럼 빛나는 무늬가 새겨져 있었습니다. 엘라는 나비를

data: 따라갔고, 나비는 그녀를 숲 가장자리에 있는 거대한 나무 앞으로 이끌었습니다.
data:
data:나무에는 오래된 문이 있었는데, 신비로운 문양으로 뒤덮여 있었습니다. 엘라는 호기심에

data: 문을 열었고, 안으로 들어서자 눈앞에 펼쳐진 광경에 숨을 멈췄습니다. 그곳은 환상적인 빛으로 가득 찬 비밀의 정원이었습니다.
data:
data:정원에는 다양한 색

...
```

#### **4. 대화 삭제**

```
curl -X 'DELETE' \
  'http://localhost:8080/api/chat/gemini' \
  -H 'accept: */*'
```

---

## **3. 실습 : 상품 리뷰 만족도 등급 분석 API 추가하기**

이커머스 플랫폼에서 고객이 남긴 **상품 리뷰**의 내용을 AI가 분석하여, 배송·품질·가격·서비스 등에 대한 평가를 근거로 상품 만족도를 **상 (high), 중 (
medium), 하 (low)** 3단계로 분류하는 API를 설계하고 구현합니다. 이 결과는 판매자 대시보드에서 상품별 만족도를 빠르게 파악하거나, 만족도가 낮은 리뷰를 자동으로
CS 팀에 우선 배정하는 데 활용할 수 있습니다.

### API 명세

이 규격에 맞춰 `ChatController`에 새로운 엔드포인트를 추가하세요.

- **Method**: `POST`
- **Endpoint**: `/api/reviews/satisfaction`
- **Content-Type**: `application/json`
- **Request Body**:

    ```java
    {
      "productName": "무선 이어폰 Pro",
      "review": "배송은 빨랐는데 착용감이 불편하고 음질도 기대 이하였어요."
    }
    ```

- **Response Body:**

    ```java
    {
      "satisfaction": "low",
      "confidence": 0.88,
      "detected_at": "2026-03-24 16:26:13"
    }
    ```

### 서비스 계층 구현 힌트

시스템 프롬프트를 통해 AI의 역할을 정의하는 것이 핵심입니다.

```java
/**
 * 상품 리뷰 내용을 분석하여 상품 만족도 등급을 반환합니다.
 * @param productName 리뷰가 작성된 상품명
 * @param review 분석할 리뷰 원문
 * @return high, medium, low 중 하나의 문자열
 */
public String evaluateReviewSatisfaction(String productName, String review) {
    return chatClient.prompt()
        .system("""
            당신은 이커머스 플랫폼의 상품 리뷰 평가 전문가입니다.
            배송/품질/가격/서비스 등에 대해
            리뷰가 실제로 전달하는 평가 내용을 근거로 상품 만족도를 판단하고,
            반드시 high, medium, low 중 하나로만 답변하세요.
            여러 요소에 대한 평가가 엇갈릴 경우 리뷰 전체 내용을 종합하여 판단하세요.
            """)
        .user("상품명: %s\n리뷰: %s".formatted(productName, review))
        .call()
        .content();
}
```

1. **System Prompt의 활용**: AI에게 특정 '페르소나 (상품 리뷰 평가 전문가)'를 부여하고, 리뷰 내용을 근거로 판단하도록 응답 형식을 제한하는 방법을
   익힙니다.
2. **데이터 정형화**: 자유로운 대화가 아닌, 정해진 키워드 (`high` 등)로 응답을 받아내어 판매자 대시보드 집계나 CS 자동 배정 같은 프로그램 로직 (If/Else
   등)에서 활용하는 법을 학습합니다.
3. **유효성 검사**: 컨트롤러에서 입력값 (`productName`, `review`)이 비어있거나 리뷰가 너무 길 경우에 대한 예외 처리를 고민해 봅니다.