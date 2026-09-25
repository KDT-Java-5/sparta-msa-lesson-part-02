# 1일차. Spring AI와 VectorDB

### **이번에 배울 것**

<aside>
❗

이번 학습에서는 현대적인 AI 애플리케이션의 핵심 저장소인 PostgreSQL과 pgvector를 활용하여 고성능 Vector Database를 직접 구축합니다. 단순히 데이터를 쌓는 것을 넘어, LLM이 이해할 수 없는 기업 내부의 기밀 문서나 최신 기술 매뉴얼 같은 '비정형 데이터'를 AI가 읽을 수 있는 형태로 변환하는 과정을 깊이 있게 다룹니다.

특히 텍스트를 수치화하는 임베딩(Embedding)의 원리를 파악하고, 대용량 문서를 효율적으로 관리하기 위한 문서 분할(Chunking) 전략을 실습하며, Spring AI를 통해 데이터가 벡터화되어 저장되는 전체 파이프라인을 완벽히 이해하게 됩니다.

</aside>

## **1. AI의 언어, 임베딩(Embedding)과 벡터 DB**

### 임베딩(Embedding)과 필요성

기계는 텍스트(Text)를 직접 이해할 수 없습니다. 따라서 텍스트에 담긴 '의미'를 숫자의 나열(Vector)로 변환해야 합니다. 이를 **임베딩**이라고 합니다.

- **전통적 방식 (Keyword Matching):** "강아지"와 "개"를 서로 다른 단어로 인식함.
- **임베딩 방식 (Semantic Understanding):** 두 단어가 좌표 평면상에서 가까운 위치에 있음을 파악하여 '유사하다'고 판단함.

```
텍스트: "강아지가 뛰어놉니다"
↓ 임베딩 모델
벡터: [0.2, 0.8, 0.1, 0.5, 0.3, ..., 0.7]  (1536차원)

```

### 고차원 벡터와 수학적 거리

임베딩 모델(예: OpenAI의 `text-embedding-3-small`)은 단어를 수백~천 개 이상의 차원을 가진 좌표로 보냅니다.

#### A. 다차원 특징 (Dimensions)

우리가 사는 세상은 3차원(가로, 세로, 높이)이지만, AI가 사용하는 임베딩 공간은 보통 **768차원~1536차원**에 달합니다.

차원이 높다는 것은 **하나의 단어를 설명하는 '기준(특징)'이 그만큼 많다**는 뜻입니다.

- **1차원:** [크기] → (개미는 작고, 코끼리는 크다)
- **2차원:** [크기, 공격성] → (개미는 작고 순하지만, 벌은 작고 공격적이다)
- **1536차원:** [크기, 공격성, 서식지, 포유류여부, 색상, 수명, 식성, ...]

이렇게 수많은 기준(차원)을 통해 단어를 정의하면, "강아지"와 "개"가 단순히 글자가 달라도 **수천 개의 특징 값이 거의 일치**하기 때문에 AI는 두 단어가 매우 유사하다고 판단할 수 있습니다. 우리는 단순하게 표현하지만, 실제 모델은 다음과 같은 수많은 '특징'을 숫자로 추출합니다.

- **특징 1 (생물인가?):** 강아지(0.9), 고양이(0.9), 자동차(0.1)
- **특징 2 (바퀴가 있는가?):** 강아지(0.0), 고양이(0.0), 자동차(1.0)
- **특징 3 (귀여운가?):** 강아지(0.8), 고양이(0.7), 자동차(0.2)

```json
"고양이" → [0.8, 0.2, 0.1, ...]
"강아지" → [0.7, 0.3, 0.1, ...]  ← 유사!
"자동차" → [0.1, 0.1, 0.9, ...]  ← 다름!
```

#### B. 거리 계산법 (Similarity Metrics)

임베딩 공간에서 두 데이터가 얼마나 유사한지는 두 벡터 사이의 거리를 계산하여 유사도를 측정합니다. 가장 대표적인 3가지 방법은 다음과 같습니다.

| **방식**               | **설명**                | **특징**                              |
|----------------------|-----------------------|-------------------------------------|
| **코사인 유사도**          | 두 벡터 사이의 **각도**를 측정   | 가장 대중적. 데이터의 크기보다 방향성(의미)이 중요할 때 사용 |
| **유클리드 거리**          | 두 점 사이의 **직선 거리**를 측정 | 값이 작을수록 유사함. 물리적인 값의 차이가 중요할 때 사용   |
| **내적 (Dot Product)** | 두 벡터의 방향과 크기를 모두 고려   | 검색 속도가 빠르며, 추천 시스템에서 자주 활용          |

```
거리:
"고양이" ↔ "강아지" = 0.15 (가까움)
"고양이" ↔ "자동차" = 0.95 (멈)
```

OpenAI의 `text-embedding-3-small` 모델이 만든 1536차원 공간에 세 단어가 있다고 가정해 봅시다.

$$
A = \text{"사과"}, \quad B = \text{"배"}, \quad C = \text{"스마트폰"}
$$

- **A와 B의 거리:** "먹는 것", "과일", "단맛" 등의 수많은 차원에서 값이 비슷합니다. 따라서 **거리가 매우 가깝습니다.**
- **A와 C의 거리:** "전자제품", "금속성", "통신" 등의 차원에서 값이 완전히 다릅니다. 따라서 **거리가 매우 멉니다.**

<aside>
💡

벡터 DB에서 검색을 한다는 것은 "내가 입력한 질문(벡터)과 가장 짧은 거리에 있는 데이터(벡터)를 찾아라"라는 수학 문제를 푸는 것과 같습니다.

</aside>

인간은 1536차원을 볼 수 없기 때문에, 이를 시각화할 때는 **t-SNE**나 **PCA** 같은 기술을 써서 2차원이나 3차원으로 압축하여 표현합니다. 이때 비슷한 의미를 가진 단어들이 끼리끼리 뭉쳐 있는 '클러스터(Cluster)'를 확인할 수 있습니다.

### 일반 DB vs **Vector**DB : AI에게 **Vector DB가 필요한 이유**

검색 패러다임의 변화이 키워드에서 '의미'로 변화하였습니다. 기존의 데이터베이스는 "문자가 일치하는가?"를 묻지만, 벡터 DB는 "맥락이 통하는가?"를 묻습니다.

| **구분**     | **일반 관계형 DB (RDB)**      | **벡터 데이터베이스 (Vector DB)**     |
|------------|--------------------------|-------------------------------|
| **핵심 기술**  | SQL (B-Tree 인덱스)         | 임베딩 & ANN (근사 최근접 이웃)         |
| **검색 방식**  | **키워드 일치 (Exact Match)** | **의미적 유사도 (Semantic Search)** |
| **데이터 형태** | 텍스트, 숫자, 날짜 (정형)         | 고차원 수치 배열 (비정형 벡터)            |
| **검색 결과**  | 단어가 포함되어야만 나옴            | 단어가 달라도 의미가 같으면 나옴            |

#### ❌ 일반 DB의 한계 (Keyword Search)

사용자가 "아파서 쉬고 싶을 때 어떻게 해?"라고 질문하면:

- **쿼리:** `SELECT * FROM docs WHERE content LIKE '%아파서%'`
- **결과:** "병가"라는 공식 용어로 작성된 문서는 **검색 결과에서 누락**됩니다. '아프다'와 '병가'는 글자가 완전히 다르기 때문입니다.

    ```json
    질문: "병가는 어떻게 신청하나요?"
    
    일반 DB:
    **SELECT * FROM documents WHERE content LIKE '%병가%'**
    → "병가"라는 단어가 있는 문서만 찾음
    ```

#### ✅ 벡터 DB의 혁신 (Vector Search)

사용자가 동일하게 질문하면:

- **쿼리:** `ORDER BY embedding <-> query_vector` (수학적 거리 측정)
- **결과:** "아프다", "몸이 안 좋다", "결근", "병가"는 임베딩 공간에서 **매우 가까운 거리**에 위치합니다. 따라서 단어가 일치하지 않아도 가장 관련 있는 **'병가 규정'** 문서를 정확히 찾아냅니다.

    ```sql
    질문: "병가는 어떻게 신청하나요?"
    
    Vector DB:
    **SELECT * FROM documents ORDER BY embedding <-> query_embedding LIMIT 5**
    → "병가", "휴가", "결근", "아플 때" 등 의미적으로 유사한 키워드를 가지고 있는 모든 문서 찾음
    
    ```

- **핵심 장점**:
    - **자연어 이해 (Natural Language Understanding):**
      사람이 말하는 일상적인 문장(구어체)으로도 전문적인 정보를 찾아낼 수 있습니다.
    - **유연한 검색 (Fuzzy Search):**
      오타가 있거나, 동의어를 사용하더라도 문맥상 가장 적절한 답변을 추천합니다.
    - **비정형 데이터 통합:**
      텍스트뿐만 아니라 이미지, 오디오, 비디오도 벡터로 변환하면 동일한 방식으로 '유사한 이미지 찾기' 등이 가능해집니다.

<aside>
💡

**기술적 메커니즘: 어떻게 작동하나요?**

1. **임베딩 생성:** 모든 지식(문서)을 임베딩 모델을 통해 벡터(숫자 리스트)로 바꿉니다.
2. **인덱싱:** 이 숫자들을 벡터 DB에 좌표 형태로 저장합니다.
3. **쿼리 변환:** 사용자의 질문도 똑같은 임베딩 모델로 벡터화합니다.
4. **유사도 계산:** 질문 벡터와 가장 **거리가 가까운** 데이터 벡터들을 순서대로 나열합니다.

</aside>

### PostgreSQL + pgvector 선택 이유

최근 AI 애플리케이션 개발에서 가장 중요한 결정 중 하나는 "벡터 데이터를 어디에 저장할 것인가?"입니다. 수많은 전용 Vector DB들 사이에서 **PostgreSQL**이 다시금 주목받는 이유를 분석합니다.

#### pgvector란 무엇인가?

**pgvector**는 세계에서 가장 대중적인 오픈소스 관계형 데이터베이스인 PostgreSQL을 벡터 데이터베이스로 변환시켜주는 확장 도구(Extension)입니다. 이를 설치하면 PostgreSQL은 기존의 텍스트, 숫자 데이터뿐만 아니라 AI가 생성한 고차원 벡터 데이터를 저장하고, '가장 가까운 거리'를 계산하는 기능을 갖게 됩니다.

#### 왜 pgvector를 선택해야 하는가?

**① 기존 인프라와의 완벽한 통합**

가장 큰 장점은 **새로운 서버를 구축할 필요가 없다**는 점입니다. 이미 많은 서비스가 PostgreSQL을 메인 DB로 사용하고 있습니다. 별도의 Vector DB를 도입하면 데이터 동기화, 보안 설정, 모니터링 시스템을 새로 만들어야 하지만, pgvector는 기존 DB에 명령어 한 줄(`CREATE EXTENSION pgvector;`)만 실행하면 즉시 사용 가능합니다.

**② ACID 트랜잭션의 강력한 보호**

전용 Vector DB들은 속도에 치중하느라 데이터의 안정성(ACID)을 놓치는 경우가 많습니다. 반면 PostgreSQL은 수십 년간 검증된 ACID(원자성, 일관성, 고립성, 지속성)를 지원합니다. 벡터 데이터를 업데이트하는 도중 서버가 꺼지더라도 데이터가 꼬이거나 유실될 걱정이 없으며, 백업과 복구 프로세스도 기존 방식을 그대로 쓸 수 있습니다.

**③ 익숙한 SQL 언어 사용**

개발자들에게 가장 친숙한 언어인 **SQL**을 그대로 사용합니다. 새로운 API나 복잡한 SDK를 배울 필요 없이, 익숙한 `SELECT`, `INSERT`, `JOIN` 문법에 벡터 거리 계산 연산자만 추가하여 즉시 비즈니스 로직에 적용할 수 있습니다. 이는 개발 속도를 비약적으로 높여줍니다.

일반 SQL 문법과 거의 동일하게 벡터 검색을 수행할 수 있습니다.

```sql
-- 1. 벡터 데이터를 포함한 테이블 생성
CREATE TABLE items
(
    id        serial PRIMARY KEY,
    content   text,
    embedding vector(1536) -- 1536차원 벡터 저장
);

-- 2. 유사도 기반 검색 (L2 거리가 가장 가까운 5개 찾기)
SELECT content
FROM items
ORDER BY embedding < - > '[0.12, 0.05, ...]' LIMIT 5;
```

위와 같이 `<->` 연산자 하나로 복잡한 수학적 거리 계산을 간단하게 처리할 수 있다는 것이 pgvector의 가장 큰 매력입니다.

**④ 데이터 거버넌스 및 보안**

기업용 서비스에서 데이터 보안은 필수입니다. pgvector를 쓰면 사용자 정보(이름, 이메일 등)와 해당 사용자의 벡터 데이터를 **한 곳에서 관리**할 수 있습니다. 서로 다른 DB에 데이터를 쪼개어 저장할 때 발생하는 보안 취약점과 관리의 복잡성을 원천적으로 차단합니다.

**⑤ 비용 효율성**

상용 클라우드 전용 Vector DB(예: Pinecone)는 데이터 양이 늘어날수록 비용이 기하급수적으로 증가합니다. 하지만 pgvector는 **완전 무료 오픈소스**입니다. 자체 서버나 저렴한 클라우드 인스턴스에 설치하여 비용 부담 없이 대규모 서비스를 운영할 수 있습니다.

#### **다른 Vector DB 옵션과의 비교 분석**

상황에 따라 다른 DB가 유리할 수도 있습니다. 각 옵션의 특징을 이해하는 것이 중요합니다.

| **옵션**       | **특징**          | **PostgreSQL + pgvector와의 차이점**           |
|--------------|-----------------|-------------------------------------------|
| **Pinecone** | 클라우드 전용 (SaaS)  | 관리가 편하지만, 데이터가 외부 서버에 저장되며 비용이 높음.        |
| **Weaviate** | 객체 지향 Vector DB | 별도의 서버 인프라를 구축하고 관리해야 하는 운영 부담이 있음.       |
| **ChromaDB** | Python 기반 경량 DB | 로컬 테스트나 프로토타이핑엔 좋지만, 대규모 운영 환경의 안정성은 부족함. |
| **Milvus**   | 대규모 분산 처리 전용    | 수억 건 이상의 초거대 데이터를 다룰 때 유리하지만, 설정이 매우 복잡함. |

---

## **2. pgvector 개발 환경 구축 가이드**

pgvector는 PostgreSQL에서 **벡터 유사도 검색**을 가능하게 해주는 확장 도구입니다. Docker를 이용하면 복잡한 설치 과정 없이 환경을 만들 수 있습니다.

#### **Docker 컨테이너 실행**

터미널(또는 CMD)에서 아래 명령어를 실행하세요. 이 명령어는 pgvector가 사전 설치된 PostgreSQL 16 버전을 실행합니다.

```bash
docker run -d --name local-postgres --restart always -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=sparta -p 5432:5432 pgvector/pgvector:pg16
```

- `d`: 백그라운드에서 실행 (컨테이너가 꺼지지 않음)

#### 데이터베이스 접속 및 확장 활성화

컨테이너가 실행되었다고 바로 벡터 기능을 쓸 수 있는 것은 아닙니다. DB 내부에서 **`vector` 확장 기능**을 켜줘야 합니다.

**확장(Extension) 설치 쿼리 실행**

```sql
-- 벡터 기능을 이 데이터베이스에서 활성화합니다.
CREATE EXTENSION IF NOT EXISTS vector;
```

**설치 확인**

정상적으로 설치되었는지 시스템 테이블을 조회해 봅니다.

```sql
SELECT *
FROM pg_extension
WHERE extname = 'vector';
```

**성공 시:** `extname` 컬럼에 `vector`가 표시됩니다.

```bash
pgvector=# CREATE EXTENSION IF NOT EXISTS vector;
NOTICE:  extension "vector" already exists, skipping
CREATE EXTENSION

pgvector=# SELECT * FROM pg_extension WHERE extname = 'vector';
  oid  | extname | extowner | extnamespace | extrelocatable | extversion | extconfig | extcondition
-------+---------+----------+--------------+----------------+------------+-----------+--------------
 16389 | vector  |       10 |         2200 | t              | 0.8.2      |           |
(1 row)
```

<aside>
💡

**유용한 팁 (Troubleshooting)**

- **포트 충돌**: 만약 `5433` 포트도 이미 사용 중이라면 `p 5434:5432` 처럼 앞의 숫자를 바꿔주세요.
- **GUI 툴 사용**: DBeaver나 pgAdmin 같은 툴로 접속할 때는 **Host:** `localhost`, **Port:** `5433`, **User:** `root`, **Password:** `test`를 입력하면 됩니다.
- **차원 수 주의**: 사용하려는 모델(Qwen, OpenAI 등)마다 벡터 차원 수가 다릅니다. 반드시 모델 스펙에 맞춰 `vector(N)`의 N 값을 설정하세요.

</aside>

#### RAG 시스템을 위한 pgvector 테이블 설계

**1. 데이터베이스 스키마 정의 (V5__create_vector_table.sql)**

```sql
-- [Step 1] 원본 문서 관리 테이블
CREATE TABLE vector_documents
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- 문서 고유 ID
    file_name    VARCHAR(255) NOT NULL,                      -- 파일명
    content      TEXT         NOT NULL,                      -- 문서 전체 내용
    content_type VARCHAR(50)  NOT NULL,                      -- 파일 확장자 (PDF, TXT 등)
    metadata     TEXT,                                       -- 추가 정보 (저자, 페이지 수 등)
    chunk_count  INTEGER      NOT NULL DEFAULT 0,            -- 분할된 조각 개수
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [Step 2] 벡터 데이터 저장 테이블 (청크 단위)
CREATE TABLE vector_store
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content    TEXT      NOT NULL,
    metadata   JSONB,
    embedding  vector(3072),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [Step 3] 검색 성능 최적화 인덱스
CREATE INDEX idx_documents_filename ON vector_documents (file_name);

-- 메타데이터 필터링 가속화
CREATE INDEX idx_vector_store_metadata ON vector_store USING gin (metadata);

```

<aside>
💡

**테이블 데이터 흐름**

1. **업로드**: 사용자가 `매뉴얼.pdf`를 시스템에 올립니다.
2. **부모 생성**: `documents` 테이블에 기본 정보가 저장됩니다. (ID: `A1` 발급)
3. **청크 분할**: 시스템이 텍스트를 3개(`조각1`, `조각2`, `조각3`)로 쪼갭니다.
4. **벡터화**: AI 모델이 각 조각을 숫자 배열(Embedding)로 변환합니다.
5. **자식 저장**: `vector_store`에 `document_id: A1`을 머리에 달고 3개의 행이 저장됩니다.
    1. 조각 1: `document_id: A1`, `content: "1단계..."`, `embedding: [...]`
    2. 조각 2: `document_id: A1`, `content: "2단계..."`, `embedding: [...]`
    3. 조각 3: `document_id: A1`, `content: "3단계..."`, `embedding: [...]`

</aside>

#### 의존성 관리 (`build.gradle`)

Spring AI 프레임워크가 벡터 데이터베이스와 통신할 수 있도록 필요한 라이브러리를 추가합니다.

```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:2023.0.2"
        mavenBom "org.springframework.ai:spring-ai-bom:1.0.0"
    }
}

dependencies {
    implementation 'org.springframework.ai:spring-ai-starter-model-openai'
    implementation 'org.springframework.ai:spring-ai-starter-vector-store-pgvector'
}
```

#### 애플리케이션 환경 설정 (`application.yml`)

프로젝트의 심장부인 설정 파일입니다. 크게 AI 모델(OpenAI/Ollama)과 **벡터 저장소(pgvector)** 설정으로 나뉩니다.

**벡터 저장소 핵심 설정 (pgvector) :** 설계한 DB 테이블과 Spring AI를 동기화하는 가장 중요한 부분입니다.

```yaml
spring:
  application:
    name: lesson

  autoconfigure:
    exclude:
      - org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration

  ai:
    openai:
      # 1. API 키: Google AI Studio에서 발급받은 키를 입력합니다.
      # ${GOOGLE_AI_GEMINI_API_KEY}
      api-key: GOOGLE_AI_GEMINI_API_KEY
      embedding:
        base-url: https://generativelanguage.googleapis.com/v1beta/openai/
        options:
          model: gemini-embedding-001
      chat:
        # 2. Base URL: Google Gemini가 제공하는 OpenAI 호환 API 주소입니다.
        base-url: "https://generativelanguage.googleapis.com/v1beta/openai/"
        options:
          # 모델명: 현재 가장 최신/경량 모델인 gemini-2.0-flash-lite 등을 지정합니다.
          model: "gemini-2.5-flash-lite"
          # 온전성(Temperature): 0.0은 가장 보수적이고 사실적인 답변을 생성합니다.
          # (분석, 요약, 데이터 추출에 최적화된 설정)
          temperature: 0.7

        # 4. 엔드포인트 경로: 대화형 API를 호출하기 위한 표준 경로입니다.
        completions-path: "/chat/completions"

    # --- [추가] pgvector 설정 ---
    vectorstore:
      pgvector:
        initialize-schema: false          # 직접 SQL로 테이블을 만들었으므로 false 권장
        dimensions: 3072                  # 임베딩 모델 최적화 차원
        distance-type: COSINE_DISTANCE    # 유사도 계산 방식 (cosine, l2, inner_product)
        index-type: hnsw                  # 고속 검색을 위한 인덱스 방식

```

**① `initialize-schema: false`인 이유**

Spring AI는 기본적으로 실행 시 테이블을 자동으로 만들려고 시도합니다. 하지만 우리는 앞서 **외래 키(FK)**와 **HNSW 인덱스**가 포함된 정교한 SQL을 직접 작성했으므로, 자동 생성 기능을 끄고 우리가 만든 테이블을 그대로 사용하게 합니다.

**② `dimensions:`** **의 중요성**

벡터는 일종의 '숫자 배열'입니다. 임베딩 모델이 내뱉는 숫자 개수와 DB 컬럼의 방 크기(`vector(N)`)가 정확히 맞아야 데이터가 저장됩니다. 하나라도 틀리면 `Dimension Mismatch` 에러가 발생합니다.

**③ `COSINE_DISTANCE` vs `L2_DISTANCE`**

- **Cosine**: 단어의 '방향성(의미)'을 중시합니다. 문서 검색(RAG)에 가장 적합합니다.
- **L2 (Euclidean)**: 두 점 사이의 '절대적 거리'를 잽니다. 이미지 비교 등에 자주 쓰입니다.

---

## 3. 임베딩 모델 구축 가이드

임베딩(Embedding)은 텍스트의 의미를 고차원 벡터로 변환하는 과정입니다. OpenAI를 사용하면 호환 엔드포인트를 통해 임베딩을 생성합니다.

#### 애플리케이션 환경 설정 (`application.yml`)

`application.yml` 설정만으로 Gemini 임베딩을 구성할 수 있습니다.
**중요: Gemini 임베딩 모델(`text-embedding-004`)의 기본 차원 수는 768입니다.**

```yaml
  ai:
    openai:
      # 1. API 키: Google AI Studio에서 발급받은 키를 입력합니다.
      # ${GOOGLE_AI_GEMINI_API_KEY}
      api-key: GOOGLE_AI_GEMINI_API_KEY
      embedding:
        base-url: https://generativelanguage.googleapis.com/v1beta/openai/
        options:
          model: gemini-embedding-001

    # 벡터 저장소 차원 수도 모델에 맞춰 768로 변경해야 합니다.
    vectorstore:
      pgvector:
        initialize-schema: false          # 직접 SQL로 테이블을 만들었으므로 false 권장
        dimensions: 3072                  # 임베딩 모델 최적화 차원
        distance-type: COSINE_DISTANCE    # 유사도 계산 방식 (cosine, l2, inner_product)
        index-type: hnsw                  # 고속 검색을 위한 인덱스 방식
```

<aside>
💡

**임베딩 모델별 차원(Dimension)**

임베딩 차원이란 텍스트의 의미를 숫자의 나열(벡터)로 변환했을 때, **그 숫자가 몇 개인지**를 의미합니다. 차원이 높을수록 더 복잡한 의미를 담을 수 있지만, 계산 속도와 저장 공간 비용이 증가합니다.

| **모델 구분**          | **모델명**                  | **차원 수 (Dim)** | **특징 및 추천 용도**                                |
|--------------------|--------------------------|----------------|-----------------------------------------------|
| **Google Cloud**   | **gemini-embedding-001** | **3072**       | 제미나이 표준 모델. 한국어 성능이 매우 뛰어나며 효율적인 성능을 보여줌.     |
| **Ollama (Local)** | **nomic-embed-text**     | **768**        | 로컬 임베딩의 표준. 제미나이와 차원이 같아 교체 테스트에 용이함.         |
| **Ollama (Local)** | **mxbai-embed-large**    | **1024**       | 고성능 임베딩. 더 깊은 문맥 파악이 필요할 때 사용 (성능 위주).        |
| **Ollama (Local)** | **bge-large**            | **1024**       | 검색 정확도가 매우 높기로 유명한 오픈소스 모델.                   |
| **Ollama (Local)** | **all-minilm**           | **384**        | **초경량 모델.** 매우 빠르고 메모리를 적게 먹지만 복잡한 문장 파악은 약함. |

- **DB 스키마 고정**: PostgreSQL의 `vector` 타입은 생성 시 차원 수를 지정해야 합니다 (`vector(3072)`). 만약 3072로 설정했는데 1024차원 모델을 쓰면 **"Dimension Mismatch"** 에러가 나며 저장이 안 됩니다.
- **검색 성능**: 차원이 클수록 AI가 단어 사이의 미세한 차이를 잘 구분하지만, 검색(유사도 계산) 시 연산량이 늘어나 답변 속도가 약간 느려질 수 있습니다.
- **저장 용량**: 차원이 2배가 되면 DB에 저장되는 벡터 값의 용량도 정확히 2배가 됩니다. 수백만 건의 데이터를 다룰 때는 384나 768 차원이 경제적입니다.

</aside>

---

## 4. Spring AI & pgvector 실전 구현 가이드

이 단계는 업로드된 문서를 영구적으로 저장하고, 이를 AI가 이해할 수 있는 '벡터(Vector)' 데이터로 변환하여 관리하는 핵심 과정입니다.

### 원본 문서 엔티티

원본 파일의 전체 내용과 메타데이터를 저장하는 테이블입니다. 나중에 AI가 답변할 때 "어떤 파일에서 가져온 정보인지"를 알려주는 출처 역할을 합니다.

```java
@Entity
@Getter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "vector_documents")
public class VectorDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String fileName;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(nullable = false, length = 50)
    String contentType;

    @Column(columnDefinition = "TEXT")
    String metadata;

    @Setter
    @Column(nullable = false)
    Integer chunkCount;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder
    public VectorDocument(
            UUID id,
            String fileName,
            String content,
            String contentType,
            String metadata,
            Integer chunkCount
    ) {
        this.id = id;
        this.fileName = fileName;
        this.content = content;
        this.contentType = contentType;
        this.metadata = metadata;
        this.chunkCount = chunkCount;
    }
}
```

```java
@Repository
public interface VectorDocumentRepository extends JpaRepository<VectorDocument, UUID> {

}
```

- **`UUID`**: 분산 환경에서 고유성을 보장하기 위해 순차적인 ID 대신 UUID를 사용합니다.
- **`TEXT` 타입**: `content`는 매우 길 수 있으므로 일반적인 `VARCHAR`가 아닌 `TEXT` 타입을 명시합니다.
- **`chunkCount`**: 추후 데이터 정합성을 확인하거나 관리하기 위해 분할된 조각의 개수를 미리 저장해둡니다.

### 벡터 저장소 설정 (`VectorStoreConfig`)

Spring AI의 `VectorStore` 빈(Bean)을 설정하여, 우리가 직접 SQL로 만든 `vector_store` 테이블과 연결합니다.

```java
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public class VectorStoreConfig {

    private int dimensions;

    @Bean
    public VectorStore vectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                // 임베딩 벡터의 차원 수를 설정합니다.
                .dimensions(dimensions)

                // 벡터 간의 유사도를 계산하는 방식을 설정합니다.
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)

                // 검색 속도를 높이기 위한 인덱스 알고리즘을 설정합니다.
                // HNSW는 대규모 데이터셋에서 빠르고 정확한 근사 최근접 이웃 검색을 지원합니다.
                .indexType(PgVectorStore.PgIndexType.HNSW)

                // 애플리케이션 시작 시 자동으로 테이블 스키마를 생성할지 여부를 결정합니다.
                // 직접 SQL로 테이블을 관리하므로 false로 설정하여 기존 구조를 유지합니다.
                .initializeSchema(false)

                // 기존에 존재하는 벡터 테이블을 삭제하고 새로 만들지 설정합니다.
                // 데이터 유실 방지를 위해 false로 설정하는 것이 안전합니다.
                .removeExistingVectorStoreTable(false)

                // 시작 시 DB 테이블의 컬럼 구성이나 차원이 설정값과 일치하는지 검증합니다.
                // 차원 설정과 실제 DB의 vector 타입 일치 여부를 체크합니다.
                .vectorTableValidationsEnabled(true)

                // 데이터가 저장될 PostgreSQL의 스키마 이름을 지정합니다.
                // 별도의 커스텀 스키마를 쓰지 않는다면 기본값인 "public"을 사용합니다.
                .schemaName("public")

                // 벡터 데이터가 실제로 저장될 테이블의 이름을 지정합니다.
                // 여기서는 직접 생성하신 "vector_store" 테이블과 연결됩니다.
                .vectorTableName("vector_store")
                .build();
    }
}
```

**분리된 저장 구조 (Hybrid Search 준비)**:

- 원본(`VectorDocument`)은 메타데이터와 전체 텍스트를 담아 데이터 무결성을 유지합니다.
- 조각난 벡터(`vector_store`)는 오직 검색 성능에 집중합니다.

**상호 보완 및 출처 관리 (Source Tracking)**:

- 검색 시 각 조각은 `768`차원의 벡터로 변환되어 저장됩니다.
- AI가 찾아낸 조각(Chunk)의 메타데이터에는 `document_id`가 포함되어 있어, 이를 통해 원본 테이블에서 파일명이나 작성자를 즉시 역추적하여 답변의 신뢰도를 높일 수 있습니다.

**성능과 정확도의 균형 (Top-K 최적화)**:

- 검색 시 사용자의 질문과 가장 유사도가 높은 상위 **5개(권장)**의 조각을 참조하여 답변을 생성함으로써, 정보 누락과 AI의 환각 현상을 동시에 방어합니다.

### Vector Request & Response 데이터 객체 생성

```java
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilaritySearchRequest {

    UUID documentId;

    String query;

    Integer topK;

}
```

사용자가 특정 문서 내에서 질문(query)을 던질 때 사용하는 **요청 객체**입니다.

- **주요 필드**:
    - `documentId`: 검색 대상이 되는 특정 문서의 고유 식별자입니다.
    - `query`: 사용자가 입력한 검색어 또는 질문 문장입니다.
    - `topK`: 유사도가 높은 순서대로 몇 개의 결과를 가져올지 결정하는 값입니다.
- **특징**: `@AllArgsConstructor`를 통해 모든 필드를 포함하는 생성자를 자동으로 생성하며, 보안을 위해 `@FieldDefaults`로 필드 접근 제어자를 `private`으로 일관되게 설정했습니다.

```java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentUploadResponse {

    String documentId;

    String filename;

    Integer chunkCount;

}
```

문서 파일을 서버나 벡터 데이터베이스에 업로드한 후, 그 결과를 반환할 때 사용하는 **응답 객체**입니다.

- **주요 필드**:
    - `documentId`: 생성된 문서의 ID입니다.
    - `filename`: 업로드된 파일의 이름입니다.
    - `chunkCount`: 큰 문서를 벡터 검색에 적합하게 쪼갠(Chunking) 조각의 개수입니다.
- **특징**: `@Builder` 패턴을 적용하여 객체 생성 시 가독성을 높였습니다.

```java
@Getter
@Builder
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
```

벡터 DB로부터 검색된 유사한 문장들과 관련 정보를 담아 사용자에게 전달하는 **최종 응답 객체**입니다.

- **구조**:
    - 전체 검색 결과의 요약 정보(`query`, `resultCount`)와 실제 검색 데이터 목록(`results`)을 포함합니다.
    - 내부 정적 클래스인 `SearchResult`를 통해 각 검색 결과의 ID, 텍스트 내용(`content`), 그리고 추가 정보(`metadata`)를 계층적으로 관리합니다.
- **특징**: `Map<String, Object> metadata`를 활용해 유연성을 확보함으로써, 문서의 페이지 번호나 작성자 등 가변적인 데이터를 처리할 수 있도록 설계되었습니다.

### VectorDocumentService: 벡터 문서 관리 및 검색 서비스

이 서비스는 일반 텍스트 문서를 벡터 데이터로 변환하여 저장하고, 이를 기반으로 유사도 검색을 수행하는 비즈니스 로직을 담당합니다.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorDocumentService {

    private final VectorStore vectorStore;
    private final VectorDocumentRepository vectorDocumentRepository;

    // 1. 문서 업로드 및 벡터화 (Main Flow)
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // [STEP 1] 원본 엔티티 생성 (ID 선발급)
        VectorDocument vectorDocument = VectorDocument.builder()
                .id(UUID.randomUUID())
                .fileName(filename)
                .content(content)
                .contentType(contentType)
                .build();

        // [STEP 2] 먼저 DB에 저장하여 확정된 ID를 얻습니다.
        VectorDocument savedDocument = vectorDocumentRepository.save(vectorDocument);

        // [STEP 3] 확정된 ID를 전달하여 문서 분할 및 메타데이터 설정
        List<Document> chunks = createChunks(content, savedDocument);

        // [STEP 4] 청크 개수 업데이트 및 Vector Store 저장
        vectorDocument.setChunkCount(chunks.size());
        vectorStore.add(chunks);

        return DocumentUploadResponse.builder()
                .documentId(savedDocument.getId().toString())
                .filename(savedDocument.getFileName())
                .chunkCount(savedDocument.getChunkCount())
                .build();
    }

    // 2. 문서 분할 로직 (추상화)
    private List<Document> createChunks(String content, VectorDocument entity) {
        // TokenTextSplitter 설정: (토큰수, 오버랩, 최소문장고정, 최대반복, 유무선 구분)
        TextSplitter splitter = new TokenTextSplitter(500, 100, 5, 10000, true);

        // 공통 메타데이터 생성
        Map<String, Object> metadata = Map.of(
                "document_id", entity.getId().toString(),
                "filename", entity.getFileName(),
                "source", "user_upload"
        );

        // Spring AI Document 객체 생성 후 분할
        Document rawVectorDocument = new Document(content, metadata);
        List<Document> splitChunks = splitter.split(rawVectorDocument);

        // 각 청크에 랜덤 UUID 부여 (충돌 방지 및 고유 식별자 확보)
        return splitChunks.stream()
                .map(chunk -> new Document(UUID.randomUUID().toString(), chunk.getText(),
                        chunk.getMetadata()))
                .toList();
    }

    // 3. 특정 문서 내 유사도 검색
    public SimilaritySearchResponse similaritySearchByDocument(UUID documentId, String query,
                                                               Integer topK) {
        // [STEP 1] Vector Store에서 검색 수행
        List<Document> searchResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(new Filter.Expression(
                                Filter.ExpressionType.EQ,
                                new Filter.Key("document_id"),
                                new Filter.Value(documentId.toString())
                        ))
                        .query(query)
                        .topK(topK)
                        .build()
        );

        // [STEP 2] 검색 결과를 SearchResult DTO 리스트로 변환
        List<SimilaritySearchResponse.SearchResult> results = searchResults.stream()
                .map(doc -> SimilaritySearchResponse.SearchResult.builder()
                        .id(doc.getId())
                        .content(doc.getText())
                        .metadata(doc.getMetadata())
                        .build())
                .toList();

        // [STEP 3] 최종 Response 객체 생성 및 반환
        return SimilaritySearchResponse.builder()
                .query(query)
                .resultCount(results.size())
                .results(results)
                .build();
    }

    // 4. 문서 삭제 (DB & Vector Store 동기화)
    @Transactional
    public void deleteDocument(UUID documentId) {
        VectorDocument entity = vectorDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문서입니다."));

        // [STEP 1] DB 삭제 (Cascade 설정이 없다면 수동 삭제 혹은 외래키 정책 활용)
        vectorDocumentRepository.delete(entity);

        // [STEP 2] Vector Store 삭제
        // Spring AI의 Filter 기능을 활용해 해당 document_id를 가진 모든 청크 조회 후 삭제
        try {
            List<String> chunkIds = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .filterExpression(new Filter.Expression(
                                    Filter.ExpressionType.EQ,
                                    new Filter.Key("document_id"),
                                    new Filter.Value(documentId.toString())
                            ))
                            .topK(10000)
                            .build()
            ).stream().map(Document::getId).toList();

            if (!chunkIds.isEmpty()) {
                vectorStore.delete(chunkIds);
                log.info("Vector Store 내 관련 청크 {}개 삭제 완료", chunkIds.size());
            }
        } catch (Exception e) {
            log.error("Vector Store 삭제 중 오류 발생 (DB는 삭제됨): {}", e.getMessage());
        }
    }
}
```

사용자가 파일을 업로드하면 시스템은 네 가지 핵심 단계를 거칩니다.

1. **텍스트 추출 (Extraction)**: `MultipartFile` 바이너리 데이터를 UTF-8 문자열로 변환합니다.
2. **원본 보존 (Persistence)**: `VectorDocument` 엔티티를 생성하여 DB에 저장합니다. 이는 나중에 파일 목록 조회나 삭제 시 기준점이 됩니다.
3. **청크 분할 (Chunking)**: 긴 문서를 LLM이 읽기 좋은 크기로 쪼갭니다.
4. **벡터화 및 저장 (Indexing)**: Spring AI가 각 조각을 숫자의 나열(Vector)로 변환하여 벡터 DB에 저장합니다.

#### **문서 분할 (`TokenTextSplitter`)**

왜 문서를 그냥 통째로 넣지 않고 쪼개야 할까요? LLM은 한 번에 읽을 수 있는 양(Context Window)이 정해져 있고, 필요한 부분만 콕 집어서 전달해야 답변의 정확도가 올라가기 때문입니다.

**⚙️ 파라미터 상세 분석**

```java
new TokenTextSplitter(
        500,   // defaultChunkSize: 한 청크당 약 500 토큰(단어 조각)씩 자름
        100,   // minChunkSizeChars: 최소 100자 이상은 되어야 의미가 있다고 판단
        5,     // minChunkLengthToEmbed: 너무 짧은 문장(예: "네.")은 임베딩 제외
        10000, // maxNumChunks: 한 파일당 생성될 수 있는 최대 조각 수
        true   // keepSeparator: 문단 구분자를 유지하여 가독성 보존
)
```

<aside>
💡

**🔄 오버랩(Overlap)의 중요성**

청크가 잘릴 때 문장의 맥락이 끊기는 것을 방지하기 위해 앞뒤 청크를 조금씩 겹치게 설계합니다.

- **청크 1**: "... 인공지능의 핵심은 **머신러닝입니다.**"
- **청크 2**: "**머신러닝입니다.** 이는 데이터를 통해 학습하며..."
- **효과**: "머신러닝"이라는 키워드가 두 청크에 모두 걸쳐 있어 검색 누락을 방지합니다.

</aside>

#### **메타데이터와 필터링 (`Metadata & Filter`)**

벡터 DB에 저장할 때 텍스트만 넣는 것이 아니라, 꼬리표(Metadata)를 함께 붙입니다.

```java
Map<String, Object> metadata = Map.of(
        "document_id", entity.getId().toString(),
        "filename", entity.getFileName(),
        "source", "user_upload"
);
```

<aside>
💡

**왜 필요한가요?**

사용자가 "내 문서 중에서 **'프로젝트 A' 파일 안에서만** 답을 찾아줘"라고 요청했을 때, 전체 수만 개의 청크를 다 뒤지는 대신 `document_id` 필터를 걸어 **검색 범위를 획기적으로 좁힐 수 있습니다.**

</aside>

#### 데이터 동기화와 삭제 (`deleteDocument`)

RAG 시스템에서 관리가 가장 소홀해지기 쉬운 부분이 삭제입니다. 원본 파일만 지우고 벡터 데이터를 남겨두면, AI가 이미 삭제된 문서의 내용을 바탕으로 답변하는 '환각(Hallucination)'이 발생합니다.

1. **관계형 DB 삭제**: `vectorDocumentRepository.delete(entity)` 호출.
2. **벡터 DB 추적**: `document_id` 필터를 사용하여 해당 문서에 속한 모든 청크 ID를 추출합니다.
3. **완전 삭제**: 추출된 ID들을 `vectorStore.delete(chunkIds)`로 지워 데이터 일관성을 유지합니다.

### Vector Document API 가이드

문서를 벡터화하여 저장하고, 이를 효율적으로 검색(Retrieval)하는 API 구성 방식과 검색 품질의 핵심 파라미터인 **Top-K**의 전략적 활용이 가능합니다.

사용자의 문서 업로드 요청을 처리하고, 벡터 엔진을 통해 유사도 검색을 수행하는 엔드포인트입니다.

```java
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vector-document")
public class VectorDocumentController {

    private final VectorDocumentService vectorDocumentService;

    // 1. 파일 업로드 및 벡터화
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentUploadResponse> uploadDocument(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(vectorDocumentService.uploadDocument(file));
    }

    // 2. 특정 문서 내에서 유사도 검색
    @GetMapping("/similarity")
    public ApiResponse<SimilaritySearchResponse> searchInDocument(SimilaritySearchRequest request) {
        return ApiResponse.ok(
                vectorDocumentService.similaritySearchByDocument(request.getDocumentId(),
                        request.getQuery(), request.getTopK()));
    }

    // 3. 문서 삭제 (DB & Vector Store) 서비스의 deleteDocument()와 대응됩니다.
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteDocument(@PathVariable UUID documentId) {
        vectorDocumentService.deleteDocument(documentId);
        return ApiResponse.ok();
    }
}
```

#### 검색의 핵심 변수: Top-K 전략

**Top-K**란 검색 시 질문과 가장 유사한 벡터를 **상위 몇 개까지 가져올 것인가**를 결정하는 변수입니다. 이 값은 LLM의 답변 품질과 비용에 직접적인 영향을 미칩니다.

| **구분**  | **값이 작을 때 (1~2)**                | **값이 적당할 때 (3~5)**             | **값이 클 때 (10 이상)**                         |
|---------|----------------------------------|--------------------------------|--------------------------------------------|
| **정확도** | 핵심 내용만 집어낼 수 있지만, 문맥이 끊길 수 있습니다. | 필요한 정보를 충분히 가져오면서 노이즈를 최소화합니다. | 관련 없는 정보가 섞여 답변의 질이 떨어질 수 있습니다. (환각 현상 증가) |
| **속도**  | LLM이 읽을 양이 적어 답변이 매우 빠릅니다.       | 성능과 속도 사이의 최적의 균형점입니다.         | LLM의 컨텍스트 제한을 초과하거나 비용이 증가할 수 있습니다.        |
| **비용**  | 토큰 사용량이 적어 경제적입니다.               | 합리적인 수준의 토큰을 소비합니다.            | 입력 토큰 수가 많아져 API 비용이 상승합니다.                |

#### 실습 가이드: API 호출 예시

**📝 Step 1. 가이드 파일 업로드**

example-service-guide.txt

`example-service-guide.txt` 파일을 전송하여 벡터화를 진행합니다.

```bash
curl -X POST http://localhost:8080/api/vector-document/upload \
  -F "file=@example-service-guide.txt"
```

**📝 Step 2. 유사도 검색 (Top-K=3 설정)**

업로드 후 받은 `documentId`를 사용하여 질문을 던집니다.

```bash
curl -G http://localhost:8080/api/vector-document/similarity \
  -d "documentId=550e8400-e29b-41d4-a716-446655440000" \
  -d "query=서비스 이용 요금은 얼마인가요?" \
  -d "topK=3"
```