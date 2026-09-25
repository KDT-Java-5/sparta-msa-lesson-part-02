# 1일차. **Spring AI 시작하기**

### **이번에 배울 것**

<aside>
❗

이번 학습에서는 인공지능(AI)의 기본 개념과 Spring AI 프레임워크를 활용한 AI 통합 방법을 학습합니다.
첫 번째로, LLM(Large Language Model)의 작동 원리와 주요 AI 모델을 이해하고 Spring AI를 통해 LLM 모델을 Spring Boot 애플리케이션에 통합하는 방법을 배웁니다.

두 번째로, Spring AI의 핵심 컴포넌트인 ChatClient를 활용하여 AI와 대화하는 방법을 익히고, Prompt Template을 사용한 동적 프롬프트 생성과 구조화된 응답 처리 방법을 학습합니다.

마지막으로, Spring AI를 활용한 간단한 AI API를 구축하여 실전 AI 애플리케이션 개발의 기초를 다질 것입니다.

</aside>

## **1. AI와 LLM 개요**

### **인공지능(AI)과 머신러닝 기본 개념**

인공지능은 단순한 프로그램이 아니라, 데이터를 통해 스스로 '규칙'을 찾아내는 거대한 기술 체계입니다. 이를 계층적으로 이해하면 다음과 같습니다.

!image.png

**인공지능 (Artificial Intelligence, AI)**

인간의 지적 능력을 컴퓨터 소프트웨어로 구현한 포괄적인 개념입니다. 과거에는 사람이 모든 규칙을 일일이 입력하는 방식(Rule-based)이었으나, 현재는 스스로 학습하는 방식으로 진화했습니다.

- **핵심 목표:** 추론(Reasoning), 학습(Learning), 계획(Planning), 소통(Communication).
- **주요 활용 분야:**
    - **자연어 처리 (NLP):** 기계 번역(파파고, 구글 번역), 챗봇, 감성 분석.
    - **컴퓨터 비전:** 자율주행차의 장애물 인식, 얼굴 인식 잠금 해제, 의료 영상 판독.
    - **음성 인식 및 합성:** **STT**(Speech-to-Text)를 통한 자막 생성, **TTS**(Text-to-Speech)를 통한 AI 목소리 구현.

**머신러닝 (Machine Learning, ML)**

데이터를 이용하여 컴퓨터를 '훈련'시키는 방법론입니다. 사람이 직접 알고리즘을 짜는 대신, 컴퓨터가 방대한 데이터 속에서 통계적 패턴을 찾아내도록 합니다.

- **학습 방식의 분류:**
    1. **지도 학습 (Supervised Learning):** 정답(Label)이 있는 데이터를 학습 (예: 스팸 메일 분류).
    2. **비지도 학습 (Unsupervised Learning):** 정답 없이 데이터의 유사성으로 그룹화 (예: 고객 군집 분석).
    3. **강화 학습 (Reinforcement Learning):** 보상과 처벌을 통해 최적의 행동을 학습 (예: 알파고).

**딥러닝 (Deep Learning, DL)**

인간의 뇌 구조인 **생물학적 뉴런**에서 영감을 얻은 인공 신경망(Artificial Neural Networks)을 활용한 머신러닝의 한 종류입니다.

- **특징:**
    - **심층 신경망:** 데이터가 여러 '은닉층(Hidden Layers)'을 통과하며 복잡한 특징을 스스로 추출합니다.
    - **비정형 데이터에 강함:** 텍스트, 이미지, 영상처럼 복잡하고 정해진 규격이 없는 데이터를 처리하는 데 압도적인 성능을 보입니다.
    - **LLM의 기반:** 우리가 흔히 쓰는 ChatGPT와 같은 거대언어모델(LLM) 역시 이 딥러닝 기술을 기반으로 탄생했습니다.

| **구분** | **인공지능 (AI)** | **머신러닝 (ML)** | **딥러닝 (DL)** |
| --- | --- | --- | --- |
| **개념** | 지능적인 기계를 만드는 과학 | 데이터를 통한 학습 알고리즘 | 인공 신경망 기반의 고도화된 학습 |
| **특징** | 가장 포괄적인 범위 | 통계적 기법 활용 | 대규모 데이터와 컴퓨팅 파워 필요 |
| **예시** | 체스 프로그램, 자율주행 | 이메일 스팸 필터, 추천 알고리즘 | 생성형 AI, 실시간 통역, 사물 인식 |

### LLM (Large Language Model) 이해

딥러닝 기술 중에서도 트랜스포머(Transformer)라는 신경망 구조를 사용하여, 엄청나게 큰 규모로 텍스트 데이터를 학습한 모델인 LLM은 단순히 글을 쓰는 프로그램을 넘어, 인류가 축적한 방대한 지식을 학습하여 인간처럼 사고하고 소통할 수 있도록 설계된 '거대 신경망'입니다.

**LLM이란?**

LLM은 딥러닝의 한 분야인 **트랜스포머(Transformer)** 아키텍처를 기반으로 하며, 다음과 같은 세 가지 '거대함'을 특징으로 합니다.

- **거대한 데이터 (Large Data):** 인터넷 문서, 서적, 코드 등 페타바이트(PB) 단위의 방대한 텍스트를 학습합니다
- **거대한 매개변수 (Large Parameters):** 모델의 '지능'을 결정하는 파라미터가 수천억 개에 달합니다. 예를 들어 Google의 PaLM 모델은 5,400억 개의 파라미터를 가집니다
- **범용성 (General Purpose):** 하나의 모델로 번역, 요약, 코딩, 창의적 글쓰기 등 수많은 작업을 동시에 수행할 수 있습니다

**LLM의 주요 학습 및 작동 방식**

LLM이 똑똑해지는 과정은 크게 두 단계로 나뉩니다.

- **사전 학습 (Pre-training):** 강아지에게 '앉아', '기다려' 같은 기본 매너를 가르치듯, 방대한 일반 데이터를 통해 언어의 구조와 상식을 배웁니다.
- **미세 조정 (Fine-tuning):** 기본 교육을 마친 모델을 특정 분야(의료, 법률, 고객 상담 등)에 맞게 전문적으로 훈련시키는 과정입니다.
- **프롬프트 설계 (Prompt Design):** 복잡한 코딩 없이도 모델에게 명확한 지시어(Prompt)를 주는 것만으로 원하는 결과를 얻을 수 있습니다.

**주요 LLM 모델 및 특징**

현재 시장을 주도하고 있는 모델들은 각기 다른 강점을 가지고 있습니다.

| **모델명** | **개발사** | **주요 특징 및 강점** |
| --- | --- | --- |
| **GPT-4 / 4o** | **OpenAI** | 현존하는 가장 강력한 **범용성**. 논리적 추론과 창의적 작업에 탁월하며, 텍스트/이미지/음성을 동시에 처리하는 옴니(Omni) 기능을 지원합니다. |
| **Claude (클로드)** | **Anthropic** | **안전성과 가독성**. 인간의 가치관에 부합하는 답변(Constitutional AI)을 생성하며, 자연스럽고 정중한 말투가 특징입니다. |
| **Gemini (제미나이)** | **Google** | **멀티모달 & 구글 생태계**. 텍스트뿐만 아니라 이미지, 영상 등을 깊게 이해하며 구글 문서, 이메일 등과 강력하게 연동됩니다. |
| **Llama (라마)** | **Meta** | **오픈소스의 리더**. 누구나 내려받아 수정하고 사용할 수 있어, 기업들이 자체적인 보안 AI를 구축할 때 가장 많이 활용됩니다. |

### **LLM 작동 원리**

LLM은 사용자의 질문을 받으면 내부적으로 **[텍스트 → 숫자 → 문맥 분석 → 확률 계산 → 다시 텍스트]**의 정교한 변환 과정을 거칩니다.

<aside>
1️⃣

**질의문 입력; Question**

🧑🏻‍🎓 사용자가 "Spring Boot란 무엇인가?"와 같은 자연어 문장을 입력합니다.

LLM은 이 문장을 곧바로 이해하는 것이 아니라, 처리 가능한 데이터 형태로 쪼개기 시작합니다.

</aside>

<aside>
2️⃣

**토큰화 (Tokenization)**

문장을 토큰(Token)이라 불리는 최소 단위로 나눕니다.

`["Spring", "Boot", "란", "무엇", "인가", "?"]`

- 특징
    - 단어, 형태소, 또는 문자 단위일 수 있습니다.
    - 단어보다 작을 수도, 클 수도 있다.
- 중요성
    - 토큰은 AI의 과금 단위이자 메모리 단위입니다.
    - AI 모델의 비용은 토큰 수로 결정됩니다.
    - GPT-4o 입력 $2.5/1M tokens, 출력 $10/1M tokens
    - 보통 영어 4글자 혹은 한글 1.5~2글자가 1토큰으로 계산됩니다.
    - 토큰화를 어떻게 하느냐에 따라 모델의 언어 이해도가 달라집니다.
</aside>

<aside>
3️⃣

**임베딩 (Embedding)**

컴퓨터는 글자를 모릅니다. 따라서 각 토큰을 수천 개의 숫자로 이루어진 **고차원 벡터(Vector)로 변환**합니다. LLM에서는 해당 과정을 **임베딩**이라 합니다.

`“Spring" → [0.2, 0.8, 0.1, ...]”`

단순히 숫자를 매기는 것이 아니라, '의미'를 좌표 평면상의 위치로 나타냅니다. 이과정에서 의미적으로 유사한 단어는 가까운 벡터 값을 가집니다.

```groovy
- "강아지" → [0.8, 0.2, 0.9, 0.1, ...]
- "개"       → [0.78, 0.22, 0.88, 0.12, ...] (유사한 벡터)
- "자동차" → [0.1, 0.9, 0.2, 0.8, ...] (다른 벡터)
```

- "강아지"와 "개"는 좌표상에서 매우 가까운 거리에 위치하고, "자동차"는 멀리 떨어지게 배치됩니다.
- 이 과정을 통해 AI는 단어 사이의 **유사성과 관계**를 파악합니다.
</aside>

<aside>
4️⃣

**트랜스포머(Transformer) 처리**

변환된 벡터 데이터는 LLM의 심장인 트랜스포머 블록으로 들어갑니다.

**❶ 셀프 어텐션 (Self-Attention)**

문장 내에서 각 단어가 **다른 단어들과 얼마나 관련이 있는지**를 수학적으로 계산하는 과정입니다.

- **핵심 역할:** "문장 내 단어들 간의 관계를 파악하는 메커니즘"으로, LLM이 인간의 질문 의도와 문맥을 이해하는 가장 중요한 핵심 기술입니다.
- **작동 방식:** 특정 단어를 처리할 때, 문장 속의 모든 단어를 훑어보며 어떤 단어에 '주의(Attention)'를 집중해야 할지 결정합니다.
- **문맥 파악:** "Spring"이라는 단어가 들어왔을 때, 주변에 "Boot"나 "Java"가 있다면 이를 계절이 아닌 '소프트웨어 프레임워크'로 즉시 해석합니다.
- **중요도 배분:** 질문에서 가장 중요한 핵심 키워드에 '주의(Attention)'를 집중하여 답변의 방향을 잡습니다.

<aside>
💡

**어텐션 메커니즘의 문맥 이해 예시**

똑같은 단어라도 주변 단어와의 관계(Attention 점수)에 따라 의미를 다르게 해석합니다.

**질문 A: "Spring Boot는 Java 기반인가요?"**

- **Self-Attention 분석:** `Spring`이라는 단어를 처리할 때, 바로 뒤의 `Boot`와 강한 연관성(High Attention Score)을 보입니다.
- **결과:** "여기서 Spring은 계절이 아니라 **IT 프레임워크**구나!"라고 판단합니다. (프레임워크 확률 90%)

**질문 B: "Spring 계절에 피는 꽃은 무엇인가요?"**

- **Self-Attention 분석:** `Spring`을 처리할 때, 뒤에 오는 `계절`, `꽃`이라는 단어와 밀접하게 연결됩니다.
- **결과:** "여기서 Spring은 소프트웨어가 아니라 **따뜻한 봄**을 의미하네!"라고 문맥을 확정합니다. (계절 확률 85%)
</aside>

**❷ 피드 포워드 (Feed Forward): 패턴 인식 및 정제**

셀프 어텐션 단계에서 "이 문장에서 'Spring'은 계절이 아니라 프레임워크야"라는 관계를 파악했다면, 피드 포워드 단계에서는 "그럼 프레임워크 Spring에 대해 내가 알고 있는 지식은 뭐지?"를 계산합니다.

!image.png

- **정의:** 각 토큰(단어)의 위치에서 독립적으로 적용되는 비선형 신경망입니다.
- **구조:** 보통 두 개의 선형 변환(Linear Layer)과 그 사이의 활성화 함수(주로 **ReLU** 또는 **GELU**)로 구성됩니다.
    - **확장:** 입력받은 데이터를 훨씬 더 큰 차원으로 확대한 뒤(Expansion), 핵심 정보를 남기고 다시 원래 차원으로 압축(Projection)합니다.

**핵심 역할: "지식의 저장소 및 정제"**

- **패턴 인식 및 지식 인출:** 모델이 학습 과정에서 배운 방대한 데이터(Spring Boot의 특징, Java와의 관계 등) 중 현재 문맥에 가장 적합한 정보를 불러옵니다.
- **비선형성 추가:** 단순한 연산을 넘어 활성화 함수를 통해 데이터에 복잡한 '굴곡'을 줍니다. 이를 통해 AI가 단순 암기를 넘어 복잡한 논리를 처리할 수 있게 됩니다.
- **토큰별 독립 처리:** 어텐션이 단어들을 서로 연결했다면, 피드 포워드는 각 단어의 벡터를 개별적으로 강화합니다.

<aside>
💡

- **Self-Attention:** 여러 명의 전문가가 모여 서로 의견을 나누며 "누가 제일 중요한 말을 했지?"를 정하는 토론 단계입니다.
- **Feed Forward:** 토론 결과를 바탕으로 각 전문가가 자신의 자리에 돌아가 "내 지식을 총동원해서 정답을 정리해보자"라고 개별적으로 깊게 생각하는 단계입니다.
</aside>

</aside>

<aside>
5️⃣

**다음 토큰 예측**

LLM은 문장을 한 번에 완성하는 것이 아니라, **다음에 올 가장 확률 높은 토큰을 하나씩** 찾아냅니다.

- **확률 분포 계산:** 학습된 방대한 데이터를 바탕으로 후보군을 뽑습니다.
`"Spring" (5%), "프레임워크" (85%), "입니다" (3%)…”`
- **확률적 선택:** 가장 높은 확률인 `Java`를 선택합니다.

**이 과정을 왜 하는 걸까요?**

LLM은 본질적으로 '다음에 올 가장 그럴싸한 단어를 맞히는 기계'이기 때문입니다.

- **창의성과 유연성:** 정해진 답변을 출력하는 것이 아니라, 매 순간 확률에 따라 단어를 선택하므로 같은 질문에도 매번 조금씩 다른, 자연스러운 답변을 생성할 수 있습니다.
- **맥락 유지:** "Spring Boot는" 다음에 "Java"를 선택했다면, 그다음 단계에서는 "Spring Boot는 Java"라는 **더 길어진 맥락**을 다시 읽습니다. 이를 통해 문장의 앞뒤 맞춤법과 논리를 끝까지 유지할 수 있습니다.
- **지식의 인출:** 학습 데이터에서 "Spring Boot"와 "Java"가 같이 쓰인 수억 개의 문장을 보았기 때문에, 통계적으로 가장 정답에 가까운 지식을 문장 형태로 풀어내는 것입니다.

**예측은 모델의 어디에서 일어날까요?**

다음 토큰 예측은 트랜스포머 구조의 가장 마지막 층인 '**출력층(Output Layer)**'에서 일어납니다.

1. **트랜스포머 블록의 통과:** 앞서 설명한 4단계(Attention, Feed Forward)를 거치면, 모델 내부에는 문맥 정보가 가득 담긴 '추상적인 숫자 뭉치(벡터)'가 생성됩니다.
2. **선형 층 (Linear Layer):** 이 숫자 뭉치를 모델이 알고 있는 전체 단어 사전(Vocabulary)의 개수만큼 넓게 펼칩니다. (예: 모델이 5만 개의 단어를 알고 있다면 5만 개의 칸을 만듭니다.)
3. **소프트맥스 (Softmax) 함수:** 펼쳐진 숫자들을 **0에서 1 사이의 확률값**으로 변환합니다. 모든 후보 단어의 확률 합은 100%가 됩니다.
    - `Java`: 0.6 (60%)
    - `프레임워크`: 0.25 (25%)
    - `무엇`: 0.1 (10%)
4. **디코딩 전략 (Decoding Strategy):** 이 확률 분포에서 최종적으로 어떤 단어를 뽑을지 결정합니다.
    - **Greedy Search:** 무조건 확률이 가장 높은 것만 선택.
    - **Sampling:** 약간의 확률이 있는 다른 단어도 섞어서 더 창의적인 답변 유도.

| **단계** | **수행 작업** | **비유** |
| --- | --- | --- |
| **셀프 어텐션** | 문장 내 단어 간 관계 파악 | **"주변 분위기 파악하기"** |
| **피드 포워드** | 관련 지식 인출 및 데이터 정제 | **"머릿속 지식 끄집어내기"** |
| **최종 출력층** | 확률이 가장 높은 단어 선택 | **"입 밖으로 한 단어 내뱉기"** |
</aside>

<aside>
6️⃣

**토큰 선택 및 반복**

확률이 높은 토큰 선택 → 다시 4번으로

선택된 토큰(`Java`)을 기존 문장 뒤에 붙여 다시 입력값으로 넣습니다.

- **입력:** "Spring Boot는 Java" → **예측:** "기반의"
- **입력:** "Spring Boot는 Java 기반의" → **예측:** "프레임워크"

이 과정을 '문장 종료 토큰'이 나올 때까지 수십, 수백 번 반복합니다.

```
프롬프트: "Spring Boot는"
Step 1: 다음 토큰 예측
- "Java" (60%)
- "프레임워크" (25%)
- "무엇" (10%)
- ...
→ "Java" 선택

Step 2: "Spring Boot는 Java"
- "기반의" (70%)
- "로" (20%)
- "언어" (5%)
→ "기반의" 선택

Step 3: "Spring Boot는 Java 기반의"
- "프레임워크" (80%)
- "애플리케이션" (15%)
→ "프레임워크" 선택

... 반복하여 완전한 문장 생성
```

| **단계** | **입력(Input / Context)** | **모델 내부 처리 (어텐션+FFN)** | **예측 결과 (Output)** |
| --- | --- | --- | --- |
| **1회차** | "Spring Boot는" | 문맥 파악 후 확률 계산 | **"Java"** |
| **2회차** | "Spring Boot는 **Java**" | 전체 문맥 다시 읽기 | **"기반의"** |
| **3회차** | "Spring Boot는 Java **기반의**" | 전체 문맥 다시 읽기 | **"프레임워크"** |
| **종료** | ... "프레임워크입니다." | 문장이 끝났음을 의미하는 **`<EOS>` 토큰** 예측 | **(멈춤)** |
</aside>

<aside>
7️⃣

**최종 응답 생성**

생성된 토큰 뭉치를 다시 인간이 읽을 수 있는 텍스트로 변환하여 화면에 출력합니다.

🧠 **최종 답변:** "Spring Boot는 Java 기반의 오픈소스 프레임워크입니다."

</aside>

### 토큰화, 비용, 성능의 삼각관계

LLM 서비스(API)는 보통 '100만 토큰당 얼마'라는 방식으로 과금됩니다.

**비용과의 관계 (직설적 영향)**

- **토큰 수 = 돈:** 문장을 잘게 쪼개서 토큰 수가 많아질수록 비용은 선형적으로 증가합니다.
- **언어별 차이:** 영어는 단어 단위로 깔끔하게 잘리는 편이지만, 한글은 형태소 분석 특성상 같은 의미라도 영어보다 **1.5배~2배** 더 많은 토큰을 소모하는 경우가 많습니다.

**성능과의 관계 (효율적 영향)**

- **문맥 유지 능력:** 모델이 한 번에 기억할 수 있는 '용량(Context Window)'은 토큰 개수로 정해져 있습니다. 토큰화를 너무 낭비하게 하면, 모델이 대화의 앞부분을 금방 잊어버리게 되어 성능이 떨어집니다.
- **학습 효율:** 토큰이 너무 잘게 쪼개지면(글자 단위 등), AI가 단어 사이의 관계를 파악하는 데 더 많은 연산이 필요해져 답변 속도가 느려집니다.

### **토큰 비용을 줄이기 위한 실전 전략**

비용을 줄인다는 것은 결국 **'전달하려는 정보의 양은 유지하면서 토큰의 개수만 줄이는 것'**이 핵심입니다.

**① 불필요한 서술어 및 공백 제거**

AI는 완벽한 문장이 아니어도 문맥을 파악합니다. '다이어트 프롬프트'를 작성하세요.

- **Bad:** "안녕하세요, 제가 지금 스프링 부트를 공부하고 있는데, 스프링 부트가 정확히 무엇인지 아주 자세하고 친절하게 설명해 주실 수 있을까요?" (약 40~50토큰)
- **Good:** "Spring Boot 정의 및 특징 요약해줘." (약 10~15토큰)

**② 영어 혼용 및 전문 용어 사용**

한글 조사(`~은/는/이/가`)나 서술어는 토큰을 많이 잡아먹습니다. 전문 용어는 영어로 쓰는 것이 토큰 효율이 좋습니다.

- **예시:** "자바 기반의 웹 프레임워크" $\rightarrow$ "Java Web Framework" (영어가 토큰을 덜 사용함)

**③ 출력 형식 지정 (Output Formatting)**

AI가 구구절절 서론을 쓰지 않게 제약 조건을 겁니다.

- "답변은 불필요한 인사말 없이 **불렛 포인트**로만 작성해줘."
- "핵심 내용만 **3문장 이내**로 요약해줘."

**④ 시스템 프롬프트 활용**

반복되는 지시사항은 대화마다 넣지 말고, '시스템 역할(System Role)'에 한 번만 정의하여 대화 도중 발생하는 중복 토큰을 방지합니다.

<aside>
💡

OpenAI나 Anthropic 등 각 제조사에서 제공하는 **'Tokenizer'** 웹사이트를 이용하면, 내가 입력한 문장이 실제로 몇 토큰으로 계산되는지 미리 확인해 볼 수 있습니다.

</aside>

---

## **2. Spring AI 시작하기**

**Spring AI**는 현대적인 생성형 AI(Generative AI) 애플리케이션을 개발할 때 발생하는 복잡한 통합 과정을 Spring의 철학(POJO, DI, 추상화)으로 해결한 프레임워크입니다.

<aside>
💡

 다양한 AI 모델(OpenAI, Ollama, Gemini 등)과 데이터 소스를 Java 객체 지향 방식으로 연결해주는 표준화된 가교 역할

</aside>

### Spring AI의 주요 특징

**① 모델 추상화 (Model Abstraction)**

가장 강력한 특징입니다. 각 AI 제조사마다 제각각인 API 호출 방식을 **통일된 인터페이스**로 제공합니다.

- **Benefit:** 코드 한 줄 수정으로 모델을 교체할 수 있습니다.
- **예시:** 테스트 때는 비용이 저렴한 `GPT-3.5`를 쓰다가, 배포 시 `GPT-4o`나 `Claude`로 바꿔도 비즈니스 로직은 변하지 않습니다.

**② Spring 생태계 완벽 통합**

Spring 프레임워크의 핵심 기능을 AI 개발에 그대로 적용합니다.

- **의존성 주입 (DI):** `ChatClient` 등을 빈(Bean)으로 등록해 어디서든 주입받아 사용합니다.
- **AOP 활용:** AI 호출 전후의 로깅, 보안, 트랜잭션 처리를 선언적으로 관리합니다.
- **Spring Boot Starter:** 복잡한 라이브러리 설정 없이 의존성 추가만으로 바로 시작할 수 있습니다.

**③ 선언적 설정 (Declarative Configuration)**

Java 코드로 API 키나 파라미터를 일일이 설정할 필요가 없습니다. `application.yml` 파일에서 모든 것을 관리합니다.

```yaml
spring:
  ai:
    openai:
      # 1. API 키: Google AI Studio에서 발급받은 키를 입력합니다.
      # ${GOOGLE_AI_GEMINI_API_KEY}
      api-key: GOOGLE_AI_GEMINI_API_KEY
      chat:
        # 2. Base URL: Google Gemini가 제공하는 OpenAI 호환 API 주소입니다.
        base-url: "https://generativelanguage.googleapis.com/v1beta/openai/"
        options:
          # 모델명: 현재 가장 최신/경량 모델인 gemini-2.0-flash-lite 등을 지정합니다.
          model: "gemini-2.5-flash-lite"
          # 온전성(Temperature): 0.0은 가장 보수적이고 사실적인 답변을 생성합니다.
          # (분석, 요약, 데이터 추출에 최적화된 설정)
          temperature: 0.7
          # 최대 토큰 수: AI가 생성할 답변의 최대 길이를 제한합니다.
          max-tokens: 4096
        # 4. 엔드포인트 경로: 대화형 API를 호출하기 위한 표준 경로입니다.
        completions-path: "/chat/completions"
```

**④ 확장성 및 벡터 데이터베이스 지원**

단순 대화뿐만 아니라 AI 성능의 핵심인 데이터 연동 기능이 포함되어 있습니다.

- **Vector Store 지원:** Pinecone, Redis, PostgreSQL 등 다양한 벡터 DB를 표준화된 인터페이스로 지원합니다.
- **RAG(검색 증강 생성) 구현:** 외부 지식을 AI에게 전달하는 복잡한 파이프라인을 쉽게 구축할 수 있도록 설계되었습니다.

| **구분** | **도입 전 (직접 연동)** | **도입 후 (Spring AI)** |
| --- | --- | --- |
| **코드 가독성** | HTTP 클라이언트로 복잡한 JSON 처리 | `ChatClient` 인터페이스 호출 |
| **모델 교체** | API 명세가 달라 대대적인 코드 수정 필요 | 설정값(Model Name) 변경만으로 완료 |
| **데이터 연동** | 벡터 DB 연동 로직 직접 구현 | 표준 인터페이스(`VectorStore`) 사용 |
| **유지 보수** | 각 제조사 SDK 업데이트마다 대응 필요 | Spring AI 업데이트만으로 최신 기능 유지 |

<aside>
💡

Spring AI는 "Java 개발자가 파이썬(Python) 환경으로 넘어가지 않고도, 익숙한 Spring Boot 환경에서 엔터프라이즈급 AI 서비스를 빠르게 구축할 수 있게 해주는 도구"입니다.

</aside>

### **AI 애플리케이션 아키텍처**

!image.png

### AI API와 백엔드 통합의 필요성

**① API Key 보안 및 권한 관리**

- **키 노출 방지:** 클라이언트(앱/웹) 코드에 API Key를 포함하면 해킹에 매우 취약합니다. 백엔드 서버(환경 변수나 Vault)에서 키를 관리해야 도난 및 부정 결제를 막을 수 있습니다.
- **인증 및 인가:** 로그인한 사용자만 AI 기능을 쓰게 하거나, 사용자 등급에 따라 모델(GPT-4 vs GPT-3.5)을 다르게 배정하는 로직은 오직 백엔드에서만 가능합니다.

**② 비즈니스 로직 및 데이터베이스 연동**

- **컨텍스트 강화:** AI는 사용자의 과거 주문 내역이나 프로필을 모릅니다. 백엔드는 DB에서 관련 데이터를 조회해 AI에게 전달(RAG 방식 등)함으로써 '나에게 특화된 답변'을 만들게 합니다.
- **결과 저장:** AI가 생성한 응답을 단순히 보여주고 끝내는 게 아니라, DB에 저장하여 나중에 다시 보거나 관리자 페이지에서 분석할 수 있습니다.

**③ 비용 관리 및 최적화**

- **처리량 제한 (Rate Limiting):** 특정 사용자가 무제한으로 AI를 호출해 비용 폭탄이 발생하는 것을 방지합니다.
- **캐싱 (Caching):** 동일한 질문(예: "배송 정책 알려줘")에 대해서는 AI를 다시 호출하지 않고, 기존에 저장된 답변을 내보내 API 비용을 획기적으로 줄입니다.

**④ 품질 관리 및 프롬프트 엔지니어링**

- **프롬프트 캡슐화:** 사용자의 짧은 질문을 백엔드에서 정교한 **프롬프트 템플릿**에 끼워 넣어 품질 높은 답변을 유도합니다. (사용자는 "요약해줘"라고만 해도, 백엔드는 "너는 전문 요약가야. 다음 글을 세 줄로..."라고 보강함)
- **응답 검증 및 필터링:** AI가 부적절한 답변을 하거나 잘못된 정보를 줄 경우, 사용자에게 전달되기 전 백엔드에서 이를 검사하고 차단할 수 있습니다.

| **사례** | **구체적인 구현 내용** | **기대 효과** |
| --- | --- | --- |
| **고객 지원 챗봇** | 사내 FAQ DB와 연동하여 24시간 상담 진행. 복잡한 문제는 상담원 연결 로직 실행. | 인건비 절감, 응대 속도 향상 |
| **문서 자동 요약** | 사용자가 업로드한 PDF/Word 파일을 텍스트로 추출하여 핵심 요점 정리. | 업무 효율성 극대화 |
| **콘텐츠 추천** | 사용자의 클릭 로그를 분석하여 AI가 좋아할 만한 상품이나 뉴스 추천. | 클릭률 및 매출 상승 |
| **코드 리뷰 자동화** | 개발자가 제출한 코드의 버그나 성능 이슈를 AI가 먼저 검토 후 코멘트 작성. | 코드 품질 상향 평준화 |

### 아키텍처 비교: 프론트엔드 직접 호출 vs 백엔드 통합

#### **1) 프론트엔드 직접 호출 방식 문제점 (❌ 안티 패턴)**

사용자의 브라우저에서 OpenAI와 같은 AI API를 직접 호출하는 구조입니다.

```tsx
┌──────────────┐
│   브라우저     │
│  (React 등)   │
└──────┬───────┘
       │ API Key 포함
       │ fetch("https://api.openai.com/v1/chat/completions", {
       │   headers: { "Authorization": "Bearer sk-..." }
       │ })
       ↓
┌──────────────┐
│  OpenAI API  │
└──────────────┘
```

1. **보안 파괴 (Critical):** 브라우저의 '개발자 도구(Network 탭)'를 열면 API Key가 그대로 노출됩니다. 누구나 이 키를 복사해 본인의 서비스에 사용할 수 있으며, 그 비용은 고스란히 서비스 제공자가 부담하게 됩니다.

    ```tsx
    // ❌ 브라우저 코드에서 직접 호출
    const response = await fetch('https://api.openai.com/v1/chat/completions', {
          headers: {
            'Authorization': 'Bearer sk-proj-abc123...' // 🚨 API Key 노출!
          }
    });
    ```

    ```
    - API Key가 브라우저 개발자 도구에 그대로 노출됨.
    - 누구나 복사하여 무제한으로 사용 가능 → 막대한 비용 청구.
    - 실제 사례: GitHub에 API Key 올려서 몇 시간 만에 수천 달러 청구된 사례 多
    ```

2. **비용 통제 불가:** 악의적인 사용자가 반복문(Loop) 스크립트를 실행해 수만 번의 요청을 보내도 막을 방법이 없습니다. 며칠 만에 수천 달러의 청구가 발생할 수 있습니다.

    ```jsx
    // 악의적인 사용자가 브라우저 콘솔에서
    for(let i = 0; i < 10000; i++) {
    	 callOpenAI("무거운 요청"); // 🚨 무제한 호출 가능
    }
    ```

    ```jsx
    - 사용자가 무제한으로 API를 호출할 수 있음.
    - 월 한도 설정이 불가능.
    ```

3. **비즈니스 로직 부재:** AI는 단순히 입력받은 질문에만 답합니다. 해당 사용자가 유료 회원인지, 질문 내용이 부적절한지, 우리 회사의 데이터베이스(DB) 정보는 무엇인지 알 길이 없습니다.

    ```jsx
    - 사용자의 권한 확인 불가.
    - 프롬프트 검증 불가(부적절한 내용 차단 못함).
    - 데이터베이스 연동 불가.
    ```

4. **CORS 제한:** 대부분의 AI API 서버는 보안상 브라우저에서의 직접 호출을 제한(CORS Policy)하므로, 이를 우회하기 위한 추가적인 보안 취약점이 발생합니다.

    ```jsx
    - 브라우저 보안 정책으로 인해 외부 API 호출 제한.
    ```


#### **2) 백엔드 통합 방식 : 백엔드 통합의 5가지 핵심 장점 (✅ 권장)**

브라우저는 우리 서버(Spring Boot)에 요청을 보내고, 서버가 안전하게 AI API와 통신하는 구조입니다.

```tsx
┌──────────────┐
│   브라우저    │
│  (React 등)  │
└──────┬───────┘
       │ API Key 없음
       │ fetch("https://myserver.com/api/ai/chat", {
       │   body: { message: "안녕?" }
       │ })
       ↓
┌──────────────────────────────────────┐
│        Spring Boot 백엔드            │
│  ┌────────────────────────────────┐  │
│  │  1. 사용자 인증 확인            │  │
│  │  2. 요청 검증 (부적절한 내용)    │  │
│  │  3. 사용량 체크 (하루 10회 제한) │  │
│  │  4. 프롬프트 템플릿 적용         │  │
│  │  5. DB에서 추가 정보 조회       │  │
│  └────────────────────────────────┘  │
│           ↓                          │
│  API Key를 안전하게 보관              │
│  (환경 변수, AWS Secrets Manager)     │
└──────────┬───────────────────────────┘
           │ API Key 포함 (안전)
           ↓
    ┌──────────────┐
    │  OpenAI API  │
    └──────────────┘
```

1. **보안 및 자산 보호**

    API Key를 서버 내부(환경 변수, Secrets Manager)에 숨깁니다. 사용자에게는 절대 노출되지 않습니다.

    ```java
    @Service
    public class ChatService {

    	@Value("${spring.ai.openai.api-key}")
    	private String apiKey; // 서버 외부로 절대 유출되지 않음

    }
    ```

2. **정교한 비용 및 사용량 관리**

    사용자별 일일 호출 횟수를 제한하여 예기치 못한 비용 지출을 방지합니다.

    ```java
    public String chatWithLimit(String userId, String message) {
        if (usageService.getTodayUsage(userId) >= 10) {
            throw new UsageLimitException("오늘 사용량을 모두 소진했습니다.");
        }
        return chatClient.prompt().user(message).call().content();
    }
    ```

3. **개인화된 비즈니스 로직**

    DB에 저장된 사용자 프로필, 구매 이력 등을 프롬프트에 결합하여 "나만을 위한 답변"을 생성합니다.

    - **예:** "이 고객은 30대 남성이고 최근 등산화를 구매했어. 이 정보를 바탕으로 상품을 추천해줘."

    ```java
    @Service
    public class ProductRecommendationService {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ProductRepository productRepository;

        public String recommendProduct(Long userId, String query) {
            // 1. DB에서 사용자 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException());

            // 2. 사용자 구매 이력 조회
            List<Product> purchaseHistory = productRepository
                    .findByUserId(userId);

            // 3. 개인화된 프롬프트 생성
            String prompt = String.format("""
                다음 사용자에게 상품을 추천해주세요:
                - 연령대: %s
                - 관심사: %s
                - 구매 이력: %s
                - 질문: %s
                """,
                user.getAgeGroup(),
                user.getInterests(),
                purchaseHistory.stream()
                    .map(Product::getName)
                    .collect(Collectors.joining(", ")),
                query
            );

            // 4. AI 호출
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        }
    }
    ```

4. **응답 품질 관리 및 모니터링**

    사용자가 대충 질문해도 백엔드에서 '전문가 페르소나'를 입혀 높은 품질의 답변을 유도합니다. 또한, AI가 답한 내용에 부적절한 표현이 있는지 검증한 뒤 사용자에게 전달합니다.

    ```java
    @Service
    public class ManagedChatService {

        public String chat(String message) {
            // 1. 입력 검증
            if (containsInappropriateContent(message)) {
                return "부적절한 내용이 포함되어 있습니다.";
            }

            // 2. 프롬프트 템플릿 적용 (일관된 응답 품질)
            String enhancedPrompt = """
                당신은 전문적이고 친절한 고객 지원 AI입니다.
                다음 규칙을 따르세요:
                1. 존댓말 사용
                2. 3문장 이내로 답변
                3. 확실하지 않으면 "정확한 답변을 드리기 어렵습니다"라고 답변

                사용자 질문: %s
                """.formatted(message);

            try {
                // 3. AI 호출
                String response = chatClient.prompt()
                        .user(enhancedPrompt)
                        .call()
                        .content();

                // 4. 로깅 (모니터링)
                log.info("AI 호출 - 입력 토큰: {}, 출력 토큰: {}, 비용: ${}",
                        inputTokens, outputTokens, cost);

                return response;

            } catch (Exception e) {
                // 5. 에러 처리
                log.error("AI 호출 실패", e);
                return "죄송합니다. 일시적인 오류가 발생했습니다.";
            }
        }
    }
    ```

5. **성능 최적화 (캐싱)**

    자주 묻는 질문은 AI를 호출하지 않고 캐시(Cache)에서 즉시 반환하여 **비용 절감과 속도 향상**을 동시에 잡습니다.

    ```java
    @Service
    public class CachedChatService {

        private final Cache<String, String> cache =
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        public String chat(String message) {
            // 동일한 질문은 캐시에서 반환 (비용 절감)
            return cache.get(message, key -> {
                return chatClient.prompt()
                        .user(key)
                        .call()
                        .content();
            });
        }
    }
    ```


---

## **3. Spring AI 프로젝트 설정**

### build.gradle 추가

Spring AI는 다양한 모델의 버전을 효율적으로 관리하기 위해 **BOM(Bill of Materials)** 방식을 권장합니다.

```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.0.0-M6"
    }
}

dependencies {
    // Google AI Gemini 스타터 추가
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
}
```

### **application.yml 설정**

Gemini 모델을 사용하기 위한 필수 설정입니다.

```yaml
spring:
  ai:
    openai:
      # 1. API 키: Google AI Studio에서 발급받은 키를 입력합니다.
      # ${GOOGLE_AI_GEMINI_API_KEY}
      api-key: GOOGLE_AI_GEMINI_API_KEY
      chat:
        # 2. Base URL: Google Gemini가 제공하는 OpenAI 호환 API 주소입니다.
        base-url: "https://generativelanguage.googleapis.com/v1beta/openai/"
        options:
          # 모델명: 현재 가장 최신/경량 모델인 gemini-2.0-flash-lite 등을 지정합니다.
          model: "gemini-2.5-flash-lite"
          # 온전성(Temperature): 0.0은 가장 보수적이고 사실적인 답변을 생성합니다.
          # (분석, 요약, 데이터 추출에 최적화된 설정)
          temperature: 0.7
          # 최대 토큰 수: AI가 생성할 답변의 최대 길이를 제한합니다.
          max-tokens: 4096
        # 4. 엔드포인트 경로: 대화형 API를 호출하기 위한 표준 경로입니다.
        completions-path: "/chat/completions"
```

### **API KEY 발급**

**1. Google AI Studio 접속**

Google AI Studio에 접속합니다. Google 계정으로 로그인이 필요합니다.

**2. 약관 동의 (최초 접속 시)**

처음 접속하면 서비스 이용 약관 동의 팝업이 뜹니다. 내용을 확인하고 동의 버튼을 클릭하여 대시보드에 진입합니다.

**3. 'Get API key' 메뉴 클릭**

왼쪽 사이드바 메뉴 상단에 있는 **[Get API key]** 아이콘(열쇠 모양)을 클릭합니다.

**4. API 키 생성 버튼 클릭**

중앙 화면에 보이는 **[Create API key]** 버튼을 클릭합니다.

<aside>
💡

기존에 만든 Google Cloud 프로젝트가 있다면 해당 프로젝트를 선택할 수 있고,
없다면 "Create API key in new project"를 선택하여 새 프로젝트와 함께 키를 생성합니다.

</aside>

**5. 키 복사 및 저장**

화면에 생성된 API 키(`AIzaSy...`로 시작하는 문자열)가 나타납니다.

- **[Copy]** 버튼을 눌러 복사합니다.
- **주의:** 이 키는 생성 직후에만 전체를 확인할 수 있는 경우가 많으므로, 반드시 안전한 곳(메모장이나 `.env` 파일 등)에 즉시 저장하세요.

!image.png

---

## **4. Spring AI 기본 사용법**

### **ChatClient를 활용한 기본 대화**

**ChatClient**는 Spring AI에서 AI 모델과 통신하기 위한 **가장 핵심적인 인터페이스**입니다. 기존의 복잡한 API 호출 방식을 지양하고, Fluent API(체이닝 방식)를 통해 직관적인 코드를 작성할 수 있게 해줍니다.

- **Builder 패턴:** `ChatClient.Builder`를 주입받아 프로젝트 전역 혹은 서비스별로 최적화된 클라이언트를 생성합니다.
- **핵심 메서드:**
    - `prompt()`: 대화 시작
    - `user()`: 사용자 질문 설정
    - `call()`: AI 모델 호출
    - `content()`: 응답 메시지(String) 추출

**실습 예제: AI 컨트롤러 구현**

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

  private final ChatClient chatClient;

  // 생성자 주입을 통한 ChatClient 빌드
  public AiChatController(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  @PostMapping("/chat")
  public String chat(@RequestBody String message) {
    return chatClient.prompt()
        .user(message)
        .call()
        .content(); // 문자열로 결과 반환
  }
}
```

```groovy
curl -X 'POST' \
  'http://localhost:8080/api/ai/chat' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '"너가 힐 수 있는 일 알려줘"'
```

### **Prompt Template 활용**

프롬프트에 매번 같은 문구를 반복 입력하는 것은 비효율적입니다. **Prompt Template**은 프롬프트의 구조(뼈대)와 가변 데이터(변수)를 분리하여 재사용성을 극대화합니다.

- **플레이스홀더:** `{variableName}` 형식을 사용하여 동적 데이터를 삽입합니다.
- **장점:** 비즈니스 로직(Java)과 프롬프트 엔지니어링(Text)을 깔끔하게 분리할 수 있습니다.

**실습 예제: 마케팅 문구 생성 서비스**

```java
  @GetMapping("/marketing")
  public String generateMarketing(
      @RequestParam(value = "productName") String productName,
      @RequestParam(value = "features") String features) {

    String template = """
        제품명 {productName}의 마케팅 문구를 작성하세요.
        주요 특징: {features}
        조건: 감성적이고 100자 이내로 작성할 것.
        """;

    return chatClient.prompt()
        .user(u -> u.text(template)
            .param("productName", productName)
            .param("features", features))
        .call()
        .content();
  }
```

```java
curl -X 'GET' \
  'http://localhost:8080/api/ai/marketing?productName=옥수수&features=기름짐' \
  -H 'accept: */*'
```

### System Message를 활용한 역할 정의

AI에게 '전문가'라는 페르소나를 부여하면 답변의 품질이 비약적으로 향상됩니다. **System Message**는 AI의 행동 지침과 배경 지식을 설정하는 데 사용됩니다.

- **우선순위:** 모델은 사용자 메시지보다 시스템 메시지의 지침을 더 근본적인 규칙으로 인식합니다.
- **활용:** 말투 설정(존댓말/반말), 금기 사항 설정, 특정 분야 전문가 설정 등.

**System Message 활용 예제:**

```java
  @GetMapping("/translate")
  public String translate(
      @RequestParam(value = "text") String text,
      @RequestParam(value = "targetLanguage", defaultValue = "영어") String targetLanguage) {

    return chatClient.prompt()
        // 1. AI의 페르소나 설정 (System Message)
        .system("당신은 전문 번역가입니다. 주어진 텍스트를 문맥에 맞게 자연스럽게 번역해주세요.")

        // 2. 동적 파라미터 주입 (Prompt Template)
        .user(u -> u.text("다음 텍스트를 {lang}로 번역해주세요: {text}")
            .param("lang", targetLanguage)
            .param("text", text))
        .call()
        .content();
  }
```

```java
curl -X 'GET' \
  'http://localhost:8080/api/ai/translate?text=간장공장 공장장&targetLanguage=영어' \
  -H 'accept: */*'
```

#### **System Message &Prompt Template : 핵심 비교표**

| **구분** | **System Message** | **Prompt Template** |
| --- | --- | --- |
| **핵심 역할** | **역할 부여 (Who)** | **구조 정의 (What)** |
| **변화 빈도** | 고정적 (서비스의 성격 정의) | 유동적 (입력값에 따라 매번 변함) |
| **비유** | **배우의 배역** (판사, 요리사 등) | **대본의 빈칸** (사건번호, 식재료 등) |
| **Spring AI 메서드** | `.system("...")` | `.user(u -> u.text("...").param("..", ..))` |
| **강제성** | 매우 높음 (탈옥 방지 등) | 중간 (사용자 입력 내용에 따라 달라짐) |

실제 서비스에서는 이 두 가지를 **동시에 사용**할 때 가장 강력한 효과를 냅니다.

```groovy
return chatClient.prompt()
    // 1. 시스템 메시지로 AI의 성격 고정 (고정값)
    .system("너는 맛집 추천 전문가야. 답변은 항상 '반말'로 친근하게 해줘.")

    // 2. 프롬프트 템플릿으로 사용자 입력 가공 (변수 사용)
    .user(u -> u.text("오늘 {location} 근처에서 {food} 맛집 3곳 추천해줘.")
                .param("location", "강남역")
                .param("food", "삼겹살"))
    .call()
    .content();
```

### Structured Output (구조화된 응답 파싱)

**Structured Output**은 AI의 답변을 개발자가 정의한 클래스에 맞춰 자동으로 파싱해주는 기능입니다.

AI의 답변을 서비스 로직에서 사용하려면 단순 문자열보다 **Java 객체(DTO)** 형태가 훨씬 유리합니다. Spring AI는 JSON 파싱의 번거로움을 자동화해줍니다.

- **`.entity()` 메서드:** 응답을 원하는 클래스 타입으로 즉시 변환합니다.
- **핵심 기능:** `.entity(Class<T> type)` 메서드 하나로 JSON 파싱 과정을 생략하고 타입 안정성을 확보합니다.
- **동작 원리:** Spring AI가 프롬프트 뒤에 해당 객체의 구조를 설명하는 **JSON 스키마**를 자동으로 덧붙여 LLM이 규격에 맞는 답변을 하도록 유도합니다.

**텍스트 응답 vs 구조화된 응답 비교**

LLM(거대언어모델)은 기본적으로 '자연어(Text)'를 출력하도록 설계되어 있습니다. 하지만 우리가 만드는 백엔드 서비스는 '객체(Object)'나 '데이터(Data)'를 필요로 합니다. 이 간극을 메워주는 것이 바로 구조화된 응답 기능입니다.

```tsx
일반 응답 (String):
"이 리뷰는 긍정적입니다. 점수는 8점이고, 제품 품질이 우수합니다."

구조화된 응답 (Java Object):
ProductAnalysis {
  sentiment = "positive",
  score = 8,
  summary = "제품 품질이 우수합니다"
}
```

기존 방식처럼 AI의 답변을 문자열로 받아 직접 쪼개려고 하면 다음과 같은 **문제점**에 봉착합니다.

**❌ 문제 1: 파싱 로직의 복잡성**

AI는 매번 똑같은 문장 구조로 대답하지 않습니다. "8점입니다"라고 할 때도 있고, "점수는 8점"이라고 할 때도 있습니다. 이를 모두 대응하는 정규 표현식을 짜는 것은 매우 고통스러운 작업입니다.

**❌ 문제 2: 형식의 불일치**

어느 날 갑자기 AI가 "점수를 매기기 어렵지만 굳이 따지자면 8점입니다"라고 수다를 떨기 시작하면, 기존에 잘 작동하던 파싱 로직은 즉시 망가집니다.

**❌ 문제 3: 타입 안정성(Type Safety) 부족**

문자열에서 '8'을 찾아냈더라도, 이것이 숫자인지 문자인지 컴파일 타임에 체크할 수 없습니다. 런타임에 `Integer.parseInt()`를 하다가 에러가 날 확률이 높습니다.

**❌ 문제 4: 에러 처리의 한계**

AI가 실수로 JSON 형식을 깨뜨리거나 콤마(,)를 빼먹는 경우, 개발자가 직접 예외 처리를 하나하나 구현해야 합니다.

```tsx
// ❌ 기존 방식 - 문자열 파싱
String response = chatClient.prompt()
    .user("이 리뷰를 분석해주세요: " + review)
    .call()
    .content();

// 결과: "긍정적이며, 8점입니다. 품질이 좋습니다."
// 문제점:
// 1. 파싱 로직이 복잡함
// 2. 형식이 일정하지 않음
// 3. 타입 안정성이 없음
// 4. 에러 처리가 어려움

String sentiment = extractSentiment(response); // 😰 복잡한 파싱
int score = extractScore(response);            // 😰 에러 가능성
```

```java
// ✅ 권장 방식 - 타입 안정성 확보 및 파싱 자동화
ProductAnalysis result = chatClient.prompt()
    .user("이 리뷰 분석해줘: " + review)
    .call()
    .entity(ProductAnalysis.class); // Spring AI가 JSON 스키마 강제 및 파싱을 알아서 수행

int score = result.getScore(); // 😃 안전하게 바로 사용!
```

**❶ 분석 결과를 담을 DTO 정의**

AI가 응답할 JSON 키값을 `@JsonProperty`로 정확히 매핑해줍니다.

```java
package com.sparta.msa.lesson.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@NoArgsConstructor // JSON 역직렬화를 위해 기본 생성자 필수
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAnalysisResponse {

    @JsonProperty("sentiment")
    String sentiment;

    @JsonProperty("score")
    int score;

    @JsonProperty("summary")
    String summary;
}
```

**❷ AiChatController에 분석 메서드 추가**

프롬프트에 형식을 구체적으로 명시하지 않아도 `.entity()`가 내부적으로 스키마를 처리하지만, 명확한 가이드를 위해 텍스트로도 형식을 남기는 것이 좋습니다.

```java
  @GetMapping("/analyze")
  public ProductAnalysisResponse analyzeReview(@RequestParam(value = "review") String review) {

    String promptText = """
        다음 제품 리뷰를 분석해주세요:

        리뷰 내용: {review}

        요구사항:
        1. sentiment는 positive, neutral, negative 중 하나로 응답하세요.
        2. score는 1점에서 10점 사이의 정수로 응답하세요.
        3. summary는 분석 내용을 한 문장으로 요약하세요.
        """;

    return chatClient.prompt()
        .user(u -> u.text(promptText).param("review", review))
        .call()
        .entity(ProductAnalysisResponse.class); // 핵심: 객체로 자동 변환
  }
```

### **ChatOptions를 활용한 세부 설정**

모델의 창의성이나 답변 길이를 조절하고 싶을 때 **ChatOptions**를 사용합니다.

- **Temperature:** 0.0에 가까우면 일관되고 정확한 답변을, 1.0에 가까우면 창의적이고 다양한 답변을 생성합니다.
- **모델 교체:** 같은 코드 내에서도 특정 호출에만 다른 모델(예: Gemini 1.5 Pro vs Flash)을 지정할 수 있습니다.

| **옵션명** | **설명** | **설정 가이드** |
| --- | --- | --- |
| **Temperature** | 답변의 무작위성(창의성) 조절 | **낮음(0.1~0.3):** 데이터 분석, 요약, 번역<br>**높음(0.7~0.9):** 소설 쓰기, 아이디어 구상 |
| **MaxTokens** | 생성될 답변의 최대 토큰 수 | 비용 절감이나 간결한 응답이 필요할 때 제한을 둡니다. |

**실습 예제: 상황별 옵션 조정**

```java
  @GetMapping("/story")
  public String generateStory(@RequestParam(value = "topic") String topic) {
    return chatClient.prompt()
        .user("다음 주제로 창의적인 이야기를 작성해주세요: " + topic)
        .options(ChatOptions.builder()
            .temperature(0.9)  // 1.0에 가까울수록 창의적(랜덤성 증가)
            .maxTokens(500)    // 답변의 최대 길이 제한
            .build())
        .call()
        .content();
  }

  @GetMapping("/summary")
  public String generateSummary(@RequestParam(value = "text") String text) {
    return chatClient.prompt()
        .user("다음 텍스트를 핵심 위주로 요약해주세요: " + text)
        .options(ChatOptions.builder()
            .temperature(0.1)  // 0.0에 가까울수록 일관적이고 사실적
            .maxTokens(200)
            .build())
        .call()
        .content();
  }
```

---

## **5. AI 도입의 핵심: 비용 구조의 철저한 이해와 최적화**

엔지니어는 단순히 코드를 짜는 사람이 아니라, **회사의 자원을 관리하는 관리자**여야 합니다. AI를 도입하는 순간, 기존과는 완전히 다른 비용 패러다임이 시작됩니다.

### 인프라 비용 vs AI 토큰 비용

- **전통적 클라우드 (Log N형):** 클라우드 인프라 비용은 로그 함수(log N) 형태를 띱니다. 초기에는 서버, 네트워크, 스토리지 설정에 상당한 고정 비용이 들지만, 트래픽이 늘어나도 비용이 그에 비례해 급격히 오르지는 않습니다. 규모의 경제가 작동하기 때문입니다.
- **AI 서비스 (가파른 선형):** AI 토큰 비용은 선형, 혹은 그 이상으로 증가합니다. 사용자가 늘어날수록 토큰 소모량이 정직하게 늘어나고, 비용도 그만큼 따라옵니다. 캐싱이나 배치 처리로 일부 완화할 수 있지만, 근본적으로 AI를 서비스에 도입하는 순간 대규모 비용은 피할 수 없는 구조가 됩니다.

### 비즈니스 모델(B2B vs B2C)에 따른 전략

AI 기능을 하나 붙이기 전에, 비용 시뮬레이션을 먼저 해야 합니다. "사용자 1만 명이 하루 평균 몇 번 이 기능을 쓰고, 요청당 평균 토큰이 얼마인가"를 계산하면 월 비용 추정이 나옵니다. 이 숫자가 회사의 현재 수익 모델과 맞는지를 판단하는 것이 엔지니어의 역할입니다.

특히 **B2C와 B2B는 감당 가능한 비용 구조가 완전히 다릅니다.** B2C 서비스는 사용자 수가 많고 건당 결제 금액이 작기 때문에, AI 비용을 분산시키려면 거래량이 압도적으로 많아야 합니다. 대형 라이브 서비스처럼 수천만 건의 거래가 발생해야 건당 비용이 미미해집니다. 반면 B2B는 고객 수가 적은 대신 계약 단가가 크기 때문에, AI 비용을 계약 금액에 포함시키는 구조가 상대적으로 용이합니다. 자사가 어느 모델인지를 먼저 파악하고 전략을 세워야 합니다.

**LLM API 의존의 함정**

외부 LLM API(OpenAI, Anthropic 등)를 이용하면 빠르게 기능을 만들 수 있지만, **가격 결정권을 완전히 잃게 됩니다.** API 제공사가 가격을 올리면 속수무책으로 따라가야 합니다. 현재 AI 기업 중 흑자를 내는 곳은 극히 드물고, 지금의 저가 정책이 언제까지 유지될지는 아무도 모릅니다. 지금 당장 편하다는 이유로 계획 없이 의존도를 높이면, 나중에 비용 구조를 바꾸기가 매우 어려워집니다.

**엔지니어는 회사의 커리어 도구가 아니라, 회사의 기대 이익을 높이는 사람이어야 합니다.** 새로운 기술을 써보고 싶다는 개인적 동기보다, 이 기술이 회사에 실제로 수익을 가져오는지를 먼저 따지는 사고방식이 필요합니다.

### 기술 변화에 대응하기

MCP(Model Context Protocol), RAG, 에이전트 프레임워크 등 오늘날 화제가 되는 기술들은 내일 더 나은 방식으로 대체될 수 있습니다. 실제로 지난 2~3년 동안 수많은 AI 툴과 프로토콜이 등장했다가 사라지거나 바뀌었습니다. 특정 기술 스택에 과도하게 의존한 서비스는 그 기술이 바뀔 때마다 대규모 리팩토링을 강요받습니다.

<aside>
💡

**변하는 것 (Transient Technology)**

**도구와 프로토콜:** MCP(Model Context Protocol), 특정 라이브러리, 프레임워크 등은 기술적 수단일 뿐이며 언제든 더 효율적인 것으로 대체됩니다. 여기에만 매몰되면 기술의 변화에 휩쓸리게 됩니다.

</aside>

반면 AI의 본질은 변하지 않습니다. **"사람이 직접 로직을 짜지 않아도, 자연어로 원하는 작업을 수행할 수 있다"** — 이것이 AI가 가진 근본적인 가치입니다. 이 본질은 모델이 바뀌든, 프로토콜이 바뀌든, API 인터페이스가 달라지든 유효합니다.

<aside>
💡

**변하지 않는 것 (Immutable Essence)**

**컴퓨터 공학의 정점:** 인간이 복잡한 로직을 하나하나 코딩하지 않아도, AI가 스스로 판단하고 수행할 수 있다는 '지능의 자동화'라는 본질은 변하지 않습니다.

**문제 해결:** 기술이 무엇이든 결국 "사용자의 문제를 해결하고 가치를 창출한다"는 서비스의 목적은 불변합니다.

</aside>

따라서 서비스를 설계할 때는 특정 기술의 세부 구현에 단단히 묶이지 않도록 추상화 레이어를 두는 것이 좋습니다. "이 기능이 GPT-4로 돌아가는가, Claude로 돌아가는가"가 아니라, "사용자에게 어떤 가치를 제공하는가"를 중심으로 설계하면, 기술이 바뀌어도 서비스의 방향은 흔들리지 않습니다.

**실전 판단 기준**

기술을 선택할 때 이 질문을 해보세요.

- 이 기술이 1년 후에도 존재할 것인가?
- 이 기술이 바뀌면 우리 서비스의 핵심 가치도 바뀌는가?
- 우리가 해결하려는 문제의 본질은 무엇인가?

기술은 도구입니다. 도구는 바뀔 수 있고, 바뀌어야 할 때 쉽게 바꿀 수 있는 구조를 미리 만들어두는 것이 좋은 엔지니어링입니다.

---

## **6. 실습: Spring AI 기본 API 구현**

```java
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatRequest {

  String message;

}
```

```java
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatResponse {

  String message;

}
```

```java
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum DomainExceptionCode {

    // ... 기존 코드들 ...

    // AI 서비스 관련 에러 코드
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스를 현재 사용할 수 없습니다."),
    AI_RESPONSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 처리 중 오류가 발생했습니다."),
    AI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 호출 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요.");

    final HttpStatus status;
    final String message;
}
```

```java
import com.sparta.msa.lesson.domain.ai.dto.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatClient.Builder clientBuilder;

  public ChatResponse chat(String message) {
    try {
      String response = clientBuilder.build()
          .prompt()
          .user(message)
          .call()
          .content();

      return ChatResponse.builder().message(response).build();
    } catch (Exception e) {
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }

  public ChatResponse chatWithContext(String userMessage) {
    try {
      String response = clientBuilder.build()
          .prompt()
          .system("""
              당신은 '스파르타 몰'의 친절하고 전문적인 쇼핑 어시스턴트입니다.
              당신의 목표는 고객이 최적의 상품을 찾도록 돕고, 쇼핑 과정의 궁금증을 해결해 주는 것입니다.

              지침:
              1. 항상 밝고 친절한 말투를 사용하세요. (예: ~해드릴까요?, ~입니다!)
              2. 상품 추천 시에는 사용자의 니즈를 다시 한번 확인하고 제안하세요.
              3. 배송이나 결제 문의에는 신중하고 정확하게 답변하세요.
              4. 모든 답변은 한국어로 작성하세요.
              """)
          .user(userMessage)
          .call()
          .content();

      return ChatResponse.builder().message(response).build();
    } catch (Exception e) {
      throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
    }
  }
}

```

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-chat")
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
    return ApiResponse.ok(chatService.chat(request.getMessage()));
  }

  @PostMapping("/context")
  public ApiResponse<ChatResponse> context(@RequestBody ChatRequest request) {
    return ApiResponse.ok(chatService.chat(request.getMessage()));
  }

}
```

### **애플리케이션 실행 및 테스트**

```java
POST http://localhost:8080/api/ai-chat
Content-Type: application/json

{
		"message": "Spring Boot에 대해 간단히 설명해줘"
}
```

```java
POST http://localhost:8080/api/ai-chat/context
Content-Type: application/json

{
		"message": "아이패드 에어랑 프로 중에 대학생이 전공 서적 보기 더 좋은 건 뭐야?"
}
```