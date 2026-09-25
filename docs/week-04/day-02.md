# 2일차.Spring AI와 RAG

### **이번에 배울 것**

<aside>
❗

이번 학습에서는 Vector DB를 기반으로, AI 서비스의 표준 모델인 RAG(Retrieval-Augmented Generation) 패턴을 본격적으로 구현합니다.

사용자의 질문이 들어왔을 때 Vector DB에서 가장 관련성이 높은 정보를 찾아내는 유사도 검색(Similarity Search) 기법을 익히고, 검색된 컨텍스트를 LLM의 프롬프트와 결합하여 환각 현상(Hallucination)이 억제된 정확한 답변을 도출하는 과정을 학습합니다.

</aside>

## 1. RAG(Retrieval-Augmented Generation)

### RAG(검색 증강 생성)란 무엇인가?

RAG(Retrieval-Augmented Generation)는 외부의 신뢰할 수 있는 데이터 소스에서 정보를 검색(Retrieval)한 후, 그 내용을 바탕으로 LLM이 답변을 생성(Generation)하도록 하는 기술입니다.

쉽게 말해, LLM에게 '오픈북 테스트'를 보게 하는 것과 같습니다. 스스로 기억하는 지식에만 의존하지 않고, 옆에 놓인 참고서를 보고 답을 찾게 하는 방식입니다.

### 왜 RAG가 필요한가? (LLM의 한계)

거대 언어 모델(LLM)은 매우 똑똑하지만, 태생적인 한계를 가지고 있습니다.

**❌ 문제 상황: LLM의 3대 약점**

1. **지식의 단절 (Knowledge Cutoff):** 모델이 학습을 마친 시점 이후의 최신 정보는 알지 못합니다.
2. **폐쇄적 데이터 (Private Data):** 기업 내부 문서, 개인 일정 등 공개되지 않은 데이터는 학습 데이터에 포함되지 않습니다.
3. **환각 현상 (Hallucination):** 모르는 내용에 대해 마치 아는 것처럼 그럴듯한 거짓말을 지어낼 위험이 있습니다.

**예시:**

```
사용자: "우리 회사의 2024년 휴가 정책은?"
Claude: "죄송하지만 귀사의 내부 정책은 알 수 없습니다."
❌ 답변 불가능

```

**RAG 적용 후:**

```
사용자: "우리 회사의 2024년 휴가 정책은?"

1. Vector DB에서 "휴가 정책" 문서 검색
2. 관련 문서를 LLM에 함께 제공
3. LLM이 문서 기반으로 정확한 답변 생성

Claude: "2024년 휴가 정책에 따르면, 연차는 15일이며..."
✅ 정확한 답변!

```

### RAG 적용 아키텍처

RAG는 단순히 질문에 답하는 것이 아니라, 데이터를 **찾기 좋은 형태로 가공하여 저장**하는 과정과, 질문에 맞는 **최적의 조각을 찾아 답변하는 과정**으로 나뉩니다.

#### Phase 1: 문서 저장 (사전 준비 작업)

LLM이 읽을 수 있도록 방대한 문서를 잘게 쪼개고 '숫자(Vector)'로 바꾸어 보관하는 과정입니다.

| **단계**              | **작업 내용**        | **상세 설명**                                        |
|---------------------|------------------|--------------------------------------------------|
| **1. 문서 수집**        | **Data Loading** | PDF, Word, 사내 Wiki 등 다양한 형태의 비정형 데이터를 불러옵니다.     |
| **2. 문서 분할**        | **Chunking**     | 문서가 너무 길면 LLM이 읽기 힘들기 때문에, 의미 있는 단위(문단 등)로 쪼갭니다. |
| **3. 임베딩 생성**       | **Embedding**    | 텍스트를 AI가 이해할 수 있는 다차원 수치(Vector)로 변환합니다.         |
| **4. Vector DB 저장** | **Storing**      | 변환된 벡터 데이터와 원문 조각을 검색 엔진(Vector DB)에 저장합니다.      |

<aside>
💡

**핵심 개념: 임베딩(Embedding)**

텍스트를 좌표 평면상의 점으로 표시하는 것입니다. "사과"와 "배"는 가까운 좌표에, "사과"와 "자동차"는 먼 좌표에 배치되어 의미적 유사도를 계산할 수 있게 합니다.

</aside>

#### Phase 2: 질문 응답 (런타임/실행)

사용자가 질문했을 때 실시간으로 정보를 찾아 답변을 조립하는 과정입니다.

**🔍 실행 순서 시뮬레이션**

1. **질문 임베딩:** 사용자의 질문 "휴가는 몇 일인가요?"를 Phase 1과 동일한 방식으로 벡터화합니다.
2. **유사도 검색 (Similarity Search):** 질문 벡터와 가장 가까운 위치에 있는 문서 조각(Chunk)을 Vector DB에서 찾습니다.
    - *결과: "연차는 15일입니다" 조각 당첨!*
3. **컨텍스트 증강 (Augmentation):** 검색된 조각과 질문을 하나로 합쳐 LLM에게 보낼 프롬프트를 만듭니다.
4. **최종 생성 (Generation):** LLM은 사전에 학습된 지식이 아닌, **방금 전달받은 문서 조각**만을 근거로 답변합니다.

### 실례로 보는 데이터 변환 과정

문서가 어떻게 숫자가 되고, 어떻게 다시 답변으로 돌아오는지의 흐름입니다.

**[데이터 저장 예시]**

- **원문:** "휴가 정책.pdf" → "연차는 15일입니다."
- **컴퓨터의 이해:** `[0.2, 0.8, 0.1, ...]` (벡터값으로 변환되어 DB 저장)

**[질문 처리 예시]**

- **사용자:** "휴가는 몇 일인가요?"
- **질문 벡터:** `[0.19, 0.82, 0.09, ...]`
- **매칭:** DB에 있는 `[0.2, 0.8, 0.1, ...]`와 매우 유사하므로 해당 문서를 꺼내옴.
- **최종 프롬프트:** > "아래 문서를 참고해 답변해줘. [문서: 연차는 15일입니다.] 질문: 휴가는 몇 일인가요?"

| **구분**       | **일반 LLM (RAG 미적용)**     | **RAG 적용 LLM**          |
|--------------|--------------------------|-------------------------|
| **지식 범위**    | 학습된 데이터 (과거)             | 학습 데이터 + 실시간 외부 데이터     |
| **사내 보안 문서** | 접근 불가 (답변 거부 또는 추측)      | 보안 DB 연동으로 **정확한 답변**   |
| **신뢰도**      | 환각 현상 발생 가능성 높음          | 출처가 명확한 근거 기반 답변        |
| **업데이트 비용**  | 재학습(Retraining) 필요 (고비용) | 데이터베이스 업데이트만으로 충분 (저비용) |

### RAG vs Fine-tuning 핵심 비교

**Fine-tuning이란?**

- Fine-tuning(미세 조정)은 이미 방대한 지식을 학습한 모델(Pre-trained Model)에게 **특정 도메인의 데이터나 스타일을 추가로 학습**시키는 과정입니다.

<aside>
💡

**비유로 이해하기**

- **사전 학습 모델:** 일반 상식을 갖춘 대학교 졸업생
- **Fine-tuning:** 그 졸업생이 특정 회사의 업무 방식이나 전문 용어를 익히기 위해 **'직무 교육(OJT)'**을 받는 과정

</aside>

RAG vs Fine-tuning 두 방식은 정보를 처리하는 메커니즘 자체가 다릅니다. RAG는 '참고서를 찾아보는 것'이고, Fine-tuning은 '머릿속에 외우는 것'입니다.

| **비교 항목**    | **RAG (검색 증강 생성)**        | **Fine-tuning (미세 조정)**       |
|--------------|---------------------------|-------------------------------|
| **핵심 원리**    | 외부 DB에서 관련 지식을 **검색**     | 모델의 파라미터(뇌)를 직접 **수정**        |
| **비용**       | **저렴** (데이터 저장/검색 비용만 발생) | **매우 비쌈** (고성능 GPU 자원 필요)     |
| **업데이트 속도**  | **즉시 반영** (문서만 추가하면 끝)    | **느림** (데이터 준비 및 재학습에 수일 소요)  |
| **정보의 최신성**  | 실시간 뉴스, 최신 문서 반영 유리       | 학습 시점의 데이터에 머무름               |
| **투명성 (근거)** | **높음** (답변의 출처 제공 가능)     | **낮음** (모델 내부에서 나온 결과 - 블랙박스) |
| **주요 목적**    | 특정 지식, 문서 기반의 **정확한 답변**  | 특정 말투, 형식, **도메인 특화 스타일** 학습  |

**1) 경제성 및 효율성**

- **RAG:** 새로운 정보가 생기면 Vector DB에 업로드만 하면 됩니다. 유지보수가 매우 쉽고 경제적입니다.
- **Fine-tuning:** 데이터가 바뀔 때마다 다시 모델을 학습시켜야 하므로 전문 인력과 막대한 컴퓨팅 비용이 지속적으로 발생합니다.

**2) 답변의 신뢰도 (환각 현상 방지)**

- **RAG:** "문서 A에 따르면..."과 같이 **근거를 제시**하므로 사용자가 답변을 신뢰할 수 있습니다.
- **Fine-tuning:** 모델이 학습한 기억에 의존하므로, 잘못된 정보를 마치 사실처럼 말하는 **환각(Hallucination)** 현상을 제어하기 어렵습니다.

<aside>
💡

**RAG를 선택해야 하는 경우**

- 최신 정보나 사내 보안 문서를 실시간으로 반영해야 할 때
- 답변의 출처(근거)를 반드시 사용자에게 보여줘야 할 때
- 예산이 한정적이고 빠른 도입이 필요할 때
- **예시:** 사내 규정 챗봇, 최신 뉴스 요약 서비스, 고객 상담 매뉴얼 봇

**Fine-tuning을 선택해야 하는 경우**

- 모델의 말투(Tone & Manner)를 아주 정교하게 바꿔야 할 때
- 특수한 프로그래밍 언어나 매우 복잡한 전문 도메인 용어를 모델이 체득해야 할 때
- 검색 없이도 모델 자체가 특정 작업을 수행하는 능력을 극대화해야 할 때
- **예시:** 특정 작가의 문체 흉내내기, 의료/법률 전문 용어 최적화, 특정 출력 형식 고정

</aside>

---

## 2. 지능형 사내 문서 QA 시스템 가이드

#### RAG 질문 및 답변 Request & Response

```java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionRequest {
    String question;
}
```

```java
@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerResponse {
    String answer;
}

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RagResponse {
    String answer;
    List<DocumentSource> sources;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DocumentSource {
        String filename;
        String documentId;
        String preview;
    }
}

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilaritySearchResponse {
    String query;
    int resultCount;
    List<SearchResult> results;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchResult {
        String id;
        String content;
        Map<String, Object> metadata;
    }
}

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchSummaryResponse {
    String query;
    String summary;
}
```

#### RagService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private static final String RAG_PROMPT_TEMPLATE = """
            다음 문서들을 참고하여 질문에 답변해주세요.
            문서에 없는 내용은 답변하지 마세요.
            답변은 한국어로 작성해주세요.
            
            [참고 문서]
            %s
            
            [질문]
            %s
            
            [답변]
            """;

    public AnswerResponse ask(String question) {
        List<Document> relevantDocs = searchDocuments(question, 5, 0.0);
        if (relevantDocs.isEmpty()) {
            throw new DomainException(DomainExceptionCode.NOT_FOUND_CONVERSATION);
        }

        return AnswerResponse.builder()
                .answer(generateAnswer(question, relevantDocs))
                .build();
    }

    public AnswerResponse askInDocument(String question, String documentId) {
        List<Document> relevantDocs = searchDocumentsWithFilter(question, documentId, 3);
        if (relevantDocs.isEmpty()) {
            throw new DomainException(DomainExceptionCode.NOT_FOUND_CONVERSATION);
        }

        String answer = chatClient.prompt()
                .system("당신은 전문 문서 기반 응답 시스템입니다. 제공된 문서 내용만 사용하세요.")
                .user(String.format(RAG_PROMPT_TEMPLATE, combineDocuments(relevantDocs), question))
                .call()
                .content();

        return AnswerResponse.builder()
                .answer(answer)
                .build();
    }

    public RagResponse askWithSource(String question) {
        List<Document> docs = searchDocuments(question, 5, 0.7);
        String answer = generateAnswer(question, docs);

        List<RagResponse.DocumentSource> sources = docs.stream()
                .map(doc -> RagResponse.DocumentSource.builder()
                        .filename((String) doc.getMetadata().get("filename"))
                        .documentId(doc.getId())
                        .preview(doc.getText().substring(0, Math.min(doc.getText().length(), 100)))
                        .build())
                .toList();

        return RagResponse.builder()
                .answer(answer)
                .sources(sources)
                .build();
    }

    public List<Document> searchDocuments(String query, int topK, double threshold) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build()
        );
    }

    public List<Document> searchDocumentsWithFilter(String query, String documentId, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression("document_id == '" + documentId + "'")
                        .build()
        );
    }

    public SimilaritySearchResponse toSearchResponse(String query, List<Document> documents) {
        List<SimilaritySearchResponse.SearchResult> results = documents.stream()
                .map(doc -> SimilaritySearchResponse.SearchResult.builder()
                        .id(doc.getId())
                        .content(doc.getText())
                        .metadata(doc.getMetadata())
                        .build())
                .toList();

        return SimilaritySearchResponse.builder()
                .query(query)
                .resultCount(results.size())
                .results(results)
                .build();
    }

    public SearchSummaryResponse getSearchSummary(String query) {
        List<Document> docs = searchDocuments(query, 3, 0.7);
        String summary = chatClient.prompt()
                .user("다음 검색 결과들을 한 문장으로 요약해줘: " + combineDocuments(docs))
                .call()
                .content();

        return SearchSummaryResponse.builder()
                .query(query)
                .summary(summary)
                .build();
    }

    private String generateAnswer(String question, List<Document> docs) {
        return chatClient.prompt()
                .user(String.format(RAG_PROMPT_TEMPLATE, combineDocuments(docs), question))
                .call()
                .content();
    }

    private String combineDocuments(List<Document> documents) {
        return documents.stream()
                .map(doc -> String.format("[%s]: %s",
                        doc.getMetadata().getOrDefault("filename", "Unknown"),
                        doc.getText()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
```

#### RagController 구현

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    @PostMapping("/ask")
    public ApiResponse<AnswerResponse> ask(@RequestBody QuestionRequest request) {
        return ApiResponse.ok(ragService.ask(request.getQuestion()));
    }

    @PostMapping("/ask-with-source")
    public ApiResponse<RagResponse> askWithSource(@RequestBody QuestionRequest request) {
        return ApiResponse.ok(ragService.askWithSource(request.getQuestion()));
    }

    @PostMapping("/ask-in-document/{documentId}")
    public ApiResponse<AnswerResponse> askInDocument(@PathVariable String documentId,
                                                     @RequestBody QuestionRequest request) {
        return ApiResponse.ok(ragService.askInDocument(request.getQuestion(), documentId));
    }

    @GetMapping("/search")
    public ApiResponse<SimilaritySearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        List<Document> docs = ragService.searchDocuments(query, topK, 0.0);
        return ApiResponse.ok(ragService.toSearchResponse(query, docs));
    }

    @GetMapping("/search-summary")
    public ApiResponse<SearchSummaryResponse> getSummary(@RequestParam String query) {
        return ApiResponse.ok(ragService.getSearchSummary(query));
    }
}
```

---

## 3. 실습 가이드: 사내 문서 기반 RAG 시스템 검증

**Spring AI**로 구축한 벡터 스토어에 사내 규정 4종을 인덱싱하고, 사용자의 자연어 질문에 대해 정확한 컨텍스트를 찾아 답변하는지 확인하는 것입니다.

먼저, 앞서 만든 4개의 문서(`vacation_policy.txt`, `salary_policy.txt`, `working_hours.txt`, `benefits.txt`)가 프로젝트의 `src/main/resources/docs/` 경로에 정상적으로 로드되었는지 확인합니다.

#### **SCENARIO 1: 단순 지식 검색 (Fact Retrieval)**

문서에 명시된 수치를 정확히 가져오는지 확인합니다.

- **질문**: "연차는 몇 일인가요?"
- **검증 포인트**: `vacation_policy.txt`에서 '15일'이라는 키워드를 정확히 추출하는가?

vacation_policy.txt

```text
# 2024년도 유급 휴가 및 휴직 규정

시행일: 2024년 1월 1일
적용 대상: 전 임직원(정규직, 계약직 포함)
관리 부서: 인사팀

...
```

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "연차는 몇 일인가요?"}'
```

#### **SCENARIO 2: 조건부 질문 (Conditional Logic)**

단순 답변이 아닌, 특정 상황(공휴일 등)에 따른 예외 규정을 이해하는지 확인합니다.

- **질문**: "급여는 언제 나오나요?"
- **검증 포인트**: "25일 지급" 외에 "주말/공휴일 시 전 영업일 지급"이라는 예외 조건까지 답변에 포함하는가?

salary_policy.txt

```text
# 급여 및 보상 관리 규정

## 1. 급여 지급일 및 정산
- **지급일**: 매월 25일(토요일 또는 공휴일인 경우 전 영업일 지급).
- **산정 기간**: 당월 1일부터 당월 말일까지를 기준으로 하며, 중도 입사자는 일할 계산함.

...
```

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "급여는 언제 나오나요?"}'
```

#### **SCENARIO 3: 복합 조건 추론 (Complex Reasoning)**

질문 하나에 담긴 여러 요구사항을 한꺼번에 처리하는지 확인합니다.

- **질문**: "재택 근무는 가능한가요? 조건이 있나요?"
- **검증 포인트**: 가능 여부(주 2회)와 조건(팀장 승인, 온라인 회의 참석)을 모두 누락 없이 답변하는가?

working_hours.txt

```text
# 근무 시간 및 업무 환경 규정

## 1. 표준 근무 시간
- **정규 시간**: 09:00~18:00(점심시간 12:00~13:00, 실 근무 8시간).
- **주 소정근로**: 주 5일, 총 40시간.

...
```

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "재택 근무는 가능한가요? 조건이 있나요?"}'
```

#### **SCENARIO 4: 검색 결과 요약 (Search Summary)**

검색된 다수의 문서 내용을 파악하여 핵심 정보를 한 문장으로 압축 전달하는지 확인합니다.

**질문**: "재택 근무는 가능한가요? 조건이 있나요?"

**검증 포인트**: 검색된 `working_hours.txt`의 다수 조각(Chunk)을 참조하여 **'주 2회 가능'**, **'팀장 승인'**, **'온라인 회의 참석'**이라는 핵심 키워드를 포함한 **'한 문장의 요약문'**을 생성하는가?

**working_hours.txt (참고 데이터)**

```text
# 근무 시간 및 업무 환경 규정

## 3. 유연 근무 및 재택근무
- **재택근무 시행**: 효율적인 업무 환경 조성을 위해 주 2회 재택근무를 허용한다.
- **사전 승인 절차**: 재택근무 실시 전, 소속 팀장에게 업무 계획을 공유하고 최종 승인을 받아야 한다.
- **복무 규정**: 재택근무 중에도 정규 근무 시간을 준수해야 하며, 모든 온라인 화상 회의에 필수적으로 참석해야 한다.
```

**API 테스트 (Curl)**

```bash
curl -X GET "http://localhost:8080/api/rag/search-summary?query=%EC%9E%AC%ED%83%9D%20%EA%B7%BC%EB%AC%B4%20%EA%B0%80%EB%8A%A5%20%EC%97%AC%EB%B6%80%EC%99%80%20%EC%A1%B0%EA%B1%B4" \
  -H "Accept: application/json"
```

**예상 응답 (JSON)**

```json
{
  "query": "재택 근무 가능 여부와 조건",
  "summary": "재택근무는 팀장의 사전 승인과 온라인 회의 필수 참석을 조건으로 주 2회까지 가능합니다."
}
```

### 결과 분석 및 튜닝 가이드

만약 테스트 결과가 만족스럽지 않다면 다음을 점검하세요:

| **현상**        | **원인**                | **해결 방안**                              |
|---------------|-----------------------|----------------------------------------|
| **엉뚱한 문서 참조** | 유사도 임계값(Threshold) 낮음 | `withSimilarityThreshold(0.7)` 이상으로 상향 |
| **답변이 너무 짧음** | 컨텍스트 문서 조각이 너무 작음     | Chunk Size를 키우거나 `TopK` 값을 5 이상으로 설정   |
| **할루시네이션 발생** | LLM이 문서 외 지식으로 답변     | 프롬프트에 "제공된 문서에 내용이 없으면 모른다고 답하세요" 추가   |

---

## 4. RAG 시스템 최적화 및 운영 가이드

RAG 시스템은 단순히 구축하는 것보다 '얼마나 정확한지'와 '얼마나 효율적인지'가 성패를 결정합니다. 자주 발생하는 3가지 문제 해결법과 모니터링, 비용 관리 기법을 정리합니다.

### 트러블슈팅: 자주 발생하는 문제와 해결책

#### **문제 1: 검색 결과의 품질 저하 (Noise)**

사용자 질문과 관련 없는 문서가 검색되어 답변의 품질이 떨어지는 경우입니다.

- **해결책:** `SimilarityThreshold`를 설정하여 하위 유사도 문서를 필터링합니다.
- **코드 가이드:**

    ```jsx
    // 0.7 이상의 유사도를 가진 상위 10개 문서만 선별
    List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.query(question)
            .withTopK(10)
            .withSimilarityThreshold(0.7) 
    );
    ```

#### **문제 2: 답변의 장황함 및 불필요한 비용 발생**

LLM이 너무 길게 답변하여 가독성이 떨어지고 출력 토큰 비용이 과다하게 발생하는 경우입니다.

- **해결책:** `MaxTokens` 옵션을 통해 출력 길이를 제한합니다.
- **코드 가이드:**

    ```jsx
    ChatResponse response = chatClient.prompt()
        .user(prompt)
        .options(AnthropicChatOptions.builder()
            .withMaxTokens(500) // 비즈니스 요구사항에 맞춰 조절
            .build())
        .call()
        .chatResponse();
    ```

#### **문제 3: 대량 문서 인덱싱 속도 저하**

문서를 하나씩 벡터 저장소에 저장할 경우 네트워크 오버헤드로 인해 시간이 오래 걸립니다.

- **해결책:** `Batch Processing`을 통해 API 호출 횟수를 줄입니다.
- **코드 가이드:**

    ```jsx
    int BATCH_SIZE = 100;
    for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
        int end = Math.min(i + BATCH_SIZE, documents.size());
        List<Document> batch = documents.subList(i, end);
        vectorStore.add(batch); // 묶음 단위 저장
    }
    ```

### 성능 모니터링: AOP를 활용한 지연 시간 측정

RAG는 [문서 검색 -> 프롬프트 생성 -> LLM 호출]의 단계를 거치므로 각 단계의 속도 체크가 필수적입니다. Spring AOP를 사용하면 비즈니스 로직을 건드리지 않고 성능을 측정할 수 있습니다.

**성능 모니터링 관점(Aspect) 구현**

```java
@Aspect
@Component
@Slf4j
public class RagPerformanceMonitor {

    @Around("execution(* org.sprain.ai.service.RagService.ask(..))")
    public Object monitorAsk(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("⏱️ RAG 전체 처리 시간: {}ms", duration);
            // 필요 시 추가로 검색 시간, LLM 응답 시간을 분리해서 측정 가능
        }
    }
}
```

### 비용 최적화 전략

RAG 운영 비용은 크게 임베딩(저장)과 LLM 호출(추론)로 나뉩니다. (2026년 기준 시뮬레이션)

**① 임베딩 비용 (OpenAI text-embedding-3-small)**

- **단가:** $0.02 / 1M tokens
- **계산:** 1,000개 문서 × 500토큰 = 50만 토큰 → **단돈 $0.01 (약 13원)**
- **Tip:** 임베딩은 한 번 해두면 재사용하므로 초기 구축 비용에 해당합니다.

**② LLM 호출 비용 (Claude 3.5 Sonnet 기준)**

RAG는 컨텍스트(참조 문서)를 프롬프트에 포함하므로 입력 토큰 관리가 핵심입니다.

| **항목**                   | **평균 사용량 (Token)** | **단가 (per 1M)** | **1,000회 수행 시 비용**   |
|--------------------------|--------------------|-----------------|----------------------|
| **입력 (Context + Query)** | 2,050              | $3.00           | $0.00615             |
| **출력 (Answer)**          | 200                | $15.00          | $0.00300             |
| **합계**                   | **2,250**          | -               | **$0.00915 (약 12원)** |

<aside>
💡

**비용 절감 팁:**

- **Context 압축:** 관련성이 낮은 문서 조각은 과감히 제거하세요.
- **캐싱(Caching):** 동일한 질문이나 빈번한 검색 결과는 캐시를 활용해 LLM 호출을 줄이세요.

</aside>