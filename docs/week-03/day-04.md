# 4일차. 로컬환경 LLM모델 연결

### **이번에 배울 것**

<aside>
❗

이번 학습에서는 비용과 보안 측면에서 유리한 로컬 환경 기반의 AI 모델 구동 방식을 집중적으로 다룹니다. 먼저 GPU 없이 CPU만으로도 원활한 실행이 가능한 Ollama의 설치 및 운영체제별 설정 방법을 익히고, 한국어 성능이 뛰어난 경량화 모델인 Alibaba Qwen3를 로컬에 직접 구축하여 다국어 처리 역량을 테스트합니다.

최종적으로는 Spring AI 프레임워크를 활용해 설정부터 컨트롤러 구현까지의 전 과정을 실습함으로써, 클라우드 API를 사용하는 것과 동일한 방식으로 Spring Boot 애플리케이션에 로컬 LLM을 완벽히 통합하는 기술을 습득하게 됩니다.

</aside>

## **1. 왜 로컬에서 AI 모델을 실행하는가?**

현대적인 AI 애플리케이션 개발에서 '클라우드 기반 API'와 '로컬 실행 모델'은 상호 보완적인 관계입니다. 프로젝트의 성격(예: 보안이 중요한 기업 내부용, 비용 절감이 절실한 개인 프로젝트 등)에 따라 적절한 방식을 선택해야 합니다.

### **클라우드 API 방식 (OpenAI, Claude 등)**

인터넷을 통해 거대 기업(Big Tech)의 강력한 모델을 빌려 쓰는 방식입니다.

**작동 원리:** **Spring Boot ↔ 인터넷(HTTPS) ↔ 외부 서버(OpenAI/Anthropic)**

**주요 특징:**

- **강력한 성능:** GPT-4, Claude 3.5와 같은 초거대 모델의 추론 능력을 그대로 활용합니다.
- **관리 편의성:** 서버 사양을 고민할 필요가 없으며, API 키만 있으면 즉시 연동됩니다.
- **잠재적 위험:** 사용량이 늘어날수록 토큰당 비용($)이 기하급수적으로 증가하며, 기업의 민감한 데이터가 외부 서버로 전송되는 **보안 이슈**가 존재합니다.

**문제점:**

- **💰 비용 부담:** 토큰당 과금 방식으로, 사용량이 늘어나면 비용이 급격히 증가합니다.
- **🌐 인터넷 의존:** 네트워크가 연결되지 않은 환경에서는 사용이 불가능합니다.
- **🔒 데이터 프라이버시:** 기업의 기밀이나 민감한 데이터가 외부 서버로 전송됩니다.
- **⏱️ 네트워크 지연:** API 호출 시 왕복 시간(RTT)으로 인해 응답이 늦어질 수 있습니다. (약 200~500ms)

```text
┌──────────────┐
│ Spring Boot  │
└──────┬───────┘
       │ API 호출 (인터넷 필요)
       │ 비용 발생 ($0.01 / 1K tokens)
       ↓
┌──────────────┐
│  OpenAI API  │
│  Claude API  │
└──────────────┘
```

### **로컬 모델 방식 (Ollama + Spring AI)**

내 컴퓨터의 자원을 활용하여 AI 모델을 직접 구동하는 방식입니다.

**작동 원리: Spring Boot ↔ 로컬 호스트(localhost:11434) ↔ Ollama Runtime**

**주요 특징:**

- **완벽한 데이터 통제:** 데이터가 외부로 유출되지 않아 **사내 기밀이나 개인정보** 처리에 최적입니다.
- **경제성:** 고가의 API 구독료 없이 하드웨어 자원만으로 무제한 추론이 가능합니다.
- **오프라인 환경:** 인터넷 연결이 불안정하거나 차단된 폐쇄망 환경에서도 AI 기능을 구현할 수 있습니다.

**✅ 장점 (Pros)**

- **비용 절감:** 모델 다운로드 및 초기 설치 후에는 완전 무료로 이용할 수 있습니다. (추가 과금 없이 전기세만 발생)
- **인터넷 불필요:** 오프라인이나 보안이 엄격한 폐쇄망 환경에서도 상시 사용이 가능합니다.
- **데이터 보안:** 입력하는 프롬프트와 결과값이 외부 서버로 전송되지 않고, 모든 데이터가 로컬에만 저장되어 안전합니다.
- **빠른 응답:** 외부 서버를 거치지 않으므로 네트워크 지연(Latency) 없이 즉각적인 반응 속도를 보여줍니다. (약 50~100ms)
- **무제한 사용:** API 호출 횟수 제한(Rate Limit)이 없어, 원하는 만큼 마음껏 테스트하고 서비스할 수 있습니다.

**❌ 단점 (Cons)**

- **하드웨어 요구사항:** AI 모델을 직접 돌리기 위해 고성능 GPU/CPU와 충분한 메모리가 필수적입니다. (최소 RAM 8GB 이상 권장)
- **모델 성능의 한계:** GPT-4나 Claude 같은 초거대 클라우드 모델에 비하면 복잡한 추론 지능이 상대적으로 낮을 수 있습니다.
- **초기 설정 필요:** Ollama 소프트웨어 설치부터 수 기가바이트(GB)에 달하는 모델 파일을 직접 다운로드해야 하는 번거로움이 있습니다.

```text
┌──────────────────────────┐
│     로컬 컴퓨터          │
│  ┌──────────────────┐    │
│  │  Spring Boot     │    │
│  └────────┬─────────┘    │
│           │ localhost    │
│           ↓              │
│  ┌──────────────────┐    │
│  │     OLLAMA       │    │
│  │  (AI 모델 실행)  │    │
│  └──────────────────┘    │
└──────────────────────────┘
```

| **비교 항목**   | **클라우드 API (SaaS)**           | **로컬 모델 (Ollama)**          |
|-------------|-------------------------------|-----------------------------|
| **운영 비용**   | **종량제 (Pay-as-you-go)** / 고비용 | **무료 (초기 하드웨어 비용)**         |
| **데이터 보안**  | 외부 서버 전송 (보안 검토 필요)           | **로컬 저장 (보안 우수)**           |
| **인터넷 연결**  | **필수** (연결 끊기면 중단)            | **불필요** (오프라인 가능)           |
| **응답 속도**   | 네트워크 지연 발생 (평균 300ms+)        | 로컬 통신 (평균 50ms~100ms)       |
| **모델 제어**   | 제공자가 정한 모델만 사용 가능             | **다양한 모델 교체 및 미세조정 가능**     |
| **하드웨어 사양** | 저사양 PC에서도 가능                  | **중상급 사양 권장 (RAM 16GB 이상)** |

### **로컬 모델 vs 클라우드 API: 언제 무엇을 선택할까?**

AI 서비스를 기획하거나 개발할 때, 무조건 최신 모델을 쓰기보다 **비용 효율성**과 **데이터 보안**을 고려한 전략적 선택이 필요합니다.

**✅ 로컬 모델(Ollama) 사용을 권장하는 상황**

- **비용 걱정 없는 프로토타입 개발**
    - 아이디어를 검증하는 단계에서 수천 번의 프롬프트 테스트가 필요할 때, API 과금 걱정 없이 무제한으로 실험할 수 있습니다.
- **민감한 데이터 처리 (Security First)**
    - **의료 기록, 금융 거래 내역, 사내 기밀 문서** 등 외부 서버로 유출되어서는 안 되는 민감한 정보를 다룰 때 최적의 선택입니다.
- **오프라인 및 폐쇄망 환경**
    - 인터넷 연결이 불안정한 현장, 선박, 혹은 보안상의 이유로 외부 망이 차단된 공공기관/군부대 환경에서 AI 기능을 구현해야 할 때 필수적입니다.
- **고빈도·고트래픽 서비스**
    - 매일 수백만 건의 단순 요청(텍스트 분류, 오타 수정 등)이 발생하는 서비스라면, 클라우드 API 비용보다 로컬 서버 운영비가 훨씬 저렴해집니다.
- **AI 학습 및 내부 동작 원리 파악**
    - 모델의 파라미터나 시스템 프롬프트가 실제 로컬 자원(CPU/GPU)을 어떻게 사용하는지 직접 확인하며 학습하고 싶을 때 유용합니다.

**☁️ 클라우드 API(OpenAI, Claude) 사용을 권장하는 상황**

- **최고 수준의 지능과 성능이 필요한 경우**
    - **GPT-4o, Claude 3.5 Sonnet** 수준의 고도화된 논리 추론, 복잡한 코딩 지원, 창의적 글쓰기가 핵심인 서비스에 적합합니다.
- **하드웨어 자원이 부족한 환경**
    - 로컬 모델을 돌릴 만한 고성능 GPU나 충분한 RAM(8GB 미만)이 없는 환경에서는 클라우드 인프라를 빌려 쓰는 것이 효율적입니다.
- **빠른 시장 검증과 배포 (Time-to-Market)**
    - 인프라 설정이나 모델 최적화에 시간을 쏟기보다, API 키 하나로 즉시 서비스를 출시하여 사용자 반응을 보고 싶을 때 유리합니다.
- **트래픽이 적어 기본 요금 수준일 때**
    - 사용량이 많지 않아 월 비용이 $10 내외로 발생하는 초기 서비스라면, 직접 서버를 관리하는 리소스보다 API 비용이 더 경제적입니다.

---

## 2. Ollama란 무엇인가?

**Ollama**는 복잡한 설정 없이 내 컴퓨터에서 **대규모 언어 모델(LLM)을 즉시 실행**할 수 있게 해주는 오픈소스 플랫폼입니다.

- **한 줄 정의:** "AI 모델계의 **Docker**" (모델을 패키징하여 어디서나 동일하게 실행)
- **핵심 역할:** 오픈소스 LLM(Llama 3, Mistral, Qwen 등)을 로컬 환경에 맞게 최적화하여 구동합니다.
- **주요 특징:**
    - **초간편 설치:** 복잡한 파이썬 라이브러리 설치 없이 실행 파일 하나로 준비 끝.
    - **다양한 모델 라이브러리:** Alibaba의 Qwen3, Meta의 Llama 3 등 최신 모델을 지원합니다.
    - **표준 API 제공:** OpenAI와 호환되는 **REST API**를 제공하여 기존 애플리케이션에 연결하기 매우 쉽습니다.
    - **크로스 플랫폼:** Windows, macOS, Linux를 모두 지원합니다.

!스크린샷 2025-11-10 오후 4.52.29.png

### **OLLAMA 설치 및 환경 구성**

#### **시스템 요구사항 (System Requirements)**

로컬에서 AI를 구동하기 위해서는 최소한의 하드웨어 사양 체크가 필요합니다.

| **구분**   | **⚙️ 최소 사양 (구동 가능)**         | **🚀 권장 사양 (쾌적함)**                 |
|----------|------------------------------|------------------------------------|
| **CPU**  | Intel i5 이상 / Apple Silicon  | 최신 멀티코어 프로세서                       |
| **RAM**  | **8GB 이상**                   | **16GB 이상** (다중 작업 시 필수)           |
| **GPU**  | CPU만으로도 가능                   | **NVIDIA RTX 3060 이상** (속도 10배 향상) |
| **Disk** | 10GB 이상의 여유 공간               | SSD (NVMe 권장, 모델 로딩 속도 향상)         |
| **OS**   | Win 10/11, macOS 11+, Ubuntu | 위와 동일                              |

#### **OLLAMA 설치하기 (Docker 방식)**

로컬 환경을 깔끔하게 유지하고 관리하기 위해 **Docker**를 이용한 설치를 권장합니다. 터미널(CMD/Terminal)에서 아래 명령어를 입력하세요.

**📌 Docker 설치 명령어**

```bash
# Ollama 컨테이너 실행 (데이터 보존을 위한 볼륨 설정 및 포트 매핑)
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
```

- `v ollama:/root/.ollama`: 다운로드한 모델 파일을 컨테이너가 삭제되어도 유지합니다.
- `p 11434:11434`: 외부(Spring Boot 등)에서 Ollama 서비스에 접속할 수 있도록 포트를 엽니다.

#### **OLLAMA 서비스 시작 및 모델 실행**

설치가 완료되었다면, 이번 학습의 핵심인 **Qwen2.5 모델**을 직접 내려받고 실행해 보겠습니다.

**1️⃣ Ollama 모델 실행**
Docker 컨테이너 내부에서 Ollama 명령어를 실행하여 모델을 구동합니다.

```bash
# qwen3:4b 모델 다운로드 및 대화형 인터페이스 실행
docker exec -it ollama ollama run qwen2.5:3b

# 다운로드 진행 상황 표시:
# pulling manifest
# pulling 8e7ca4ddb09c... 100% ▕████████████████▏ 1.9 GB
# pulling f8ac8c4bdb71... 100% ▕████████████████▏  485 B
# pulling cf59c5c5eacf... 100% ▕████████████████▏  11 KB
# pulling 1f16d5c77cbe... 100% ▕████████████████▏   68 B
# pulling e3038111a6ee... 100% ▕████████████████▏  561 B
# verifying sha256 digest
# writing manifest
# success
```

**2️⃣ 정상 동작 확인**
명령어를 입력하면 모델 다운로드가 시작되며, 완료 후 `>>>` 프롬프트가 나타납니다.

- "안녕? 너는 누구니?"라고 질문하여 한국어 응답이 정상적으로 오는지 확인하세요.
- 종료하고 싶을 때는 `/bye`를 입력하거나 `Ctrl + D`를 누르면 됩니다.

  **서버 상태 확인 :**

    ```bash
    # 콘솔 & 브라우저 실행
    curl http://localhost:11434
    
    # 출력:
    # Ollama is running
    ```

<aside>
💡

`4b`는 파라미터 규모를 의미하며, 일반적인 노트북 환경에서 가장 빠르고 효율적으로 돌아가는 크기입니다. 이제 로컬 엔진이 준비되었으니, 다음 단계인 **Spring AI 연동**으로 넘어갈 준비가 되었습니다!

</aside>

---

## 3. Qwen3 모델 시작하기

**Qwen3**는 Alibaba Cloud에서 선보이는 차세대 오픈소스 LLM으로, 이전 세대보다 더욱 정교해진 한국어 이해도와 추론 능력을 갖추고 있습니다. 특히 **4b(40억 파라미터)** 모델은 일반적인 개발용 노트북에서도 끊김 없이 돌아가는 '가성비 끝판왕' 모델입니다.

- **제작사:** Alibaba Cloud
- **주요 특징:**
    - **🇰🇷 한국어 특화:** 다국어 데이터셋 중 한국어 비중이 높아 자연스러운 문체 구현.
    - **⚡ 최적화된 속도:** 4b 모델은 CPU 환경에서도 초당 수십 단어를 생성할 정도로 가볍습니다.
    - **🆓 완전 무료:** 상업적 프로젝트에 제약 없이 적용 가능한 라이선스.
    - **🛠️ 코딩 특화:** Java, Python 등 주요 프로그래밍 언어의 코드 생성 능력이 대폭 향상되었습니다.

**모델 크기 및 사양 가이드 (4b 중심)**

Ollama에서 제공하는 Qwen2.5의 체급별 요구사항입니다. 현재 실습의 메인 모델인 **4b**를 확인해 보세요.

| **모델 태그**          | **파라미터**  | **RAM 요구량** | **속도 (추론)** | **품질**   | **추천 용도**                |
|--------------------|-----------|-------------|-------------|----------|--------------------------|
| **`qwen2.5:0.5b`** | 5억 개      | 2GB 이하      | ⚡⚡⚡⚡        | ⭐⭐       | 초경량 테스트, 단순 분류/치환        |
| **`qwen2.5:1.5b`** | 15억 개     | 4GB 이상      | ⚡⚡⚡         | ⭐⭐⭐      | 가벼운 챗봇, 모바일 환경 실습        |
| **`qwen2.5:3b`**   | **30억 개** | **8GB 이상**  | **⚡⚡⚡**     | **⭐⭐⭐⭐** | **[강력 추천] 균형 잡힌 속도와 지능** |
| **`qwen2.5:7b`**   | 70억 개     | 16GB 권장     | ⚡⚡          | ⭐⭐⭐⭐⭐    | 복잡한 로직, RAG(문서 기반 답변)    |
| **`qwen2.5:14b`**  | 140억 개    | 32GB 권장     | ⚡           | ⭐⭐⭐⭐⭐    | 전문 지식 분석, 고난도 코딩 지원      |

### **Qwen3 모델 실행 실습**

#### **1️⃣ 설치된 모델 목록 확인**

먼저 내 컴퓨터에 `qwen2.5:3b` 모델이 잘 설치되었는지 확인합니다.

```bash
docker exec -it ollama ollama list

# 출력 예시
# NAME              ID              SIZE      MODIFIED
# qwen2.5:3b        f6a123bc456d    2.3 GB    1 minute ago
```

<aside>
💡

`docker exec -it ollama`는 "ollama라는 이름의 컨테이너 안에서 다음 명령어를 실행하라"는 뜻입니다.

</aside>

#### **2️⃣ 대화형 모드(CLI) 실행 및 테스트**

이제 직접 모델을 구동하여 성능을 확인해 봅시다.

```bash
docker exec -it ollama ollama run qwen2.5:3b
```

**1. 테스트 시나리오: 한국어 및 개발 지식**

**사용자:** "Spring AI와 Ollama를 연결하면 어떤 장점이 있어?"

**Qwen3:** "가장 큰 장점은 **데이터 보안**과 **비용 절감**입니다. 모든 데이터가 로컬에서 처리되어 외부 유출 걱정이 없으며, API 호출 비용 없이 무제한으로 테스트할 수 있습니다. 또한 Spring AI의 추상화된 인터페이스 덕분에 코드 수정 없이 모델을 쉽게 교체할 수 있습니다."

**2. 테스트 시나리오: Java 코드 생성**

**사용자:** "Java로 현재 날짜를 출력하는 간단한 Spring Boot 컨트롤러를 짜줘."

**Qwen3:** "네, 다음과 같이 `RestController`를 작성할 수 있습니다."

```bash
@RestController
public class DateController {
    @GetMapping("/today")
    public String getToday() {
        return "오늘의 날짜: " + LocalDate.now();
    }
}
```

### **유용한 관리 명령어**

실습 중 모델을 교체하거나 정보를 확인할 때 사용하세요.

- **멀티라인 입력:** 여러 줄의 프롬프트를 보낼 때 `"""`로 감쌉니다.

    ```bash
    >>> """
    다음 문장을 영어로 번역하고, 핵심 단어 3개를 뽑아줘:
    "로컬 AI 모델을 사용하면 데이터 보안을 강화할 수 있습니다."
    """
    ```

- **모델 정보 보기:** 모델의 아키텍처 정보를 출력합니다.

    ```bash
    /show info
    ```

- **종료하기:** 세션을 안전하게 닫습니다.

    ```bash
    /bye
    ```

### OLLAMA API 테스트 및 활용

Ollama는 기본적으로 `11434` 포트에서 HTTP 서버를 운영합니다. 이를 통해 우리는 브라우저나 터미널, 혹은 자바 코드로 AI에게 질문을 던질 수 있습니다.

#### **1️⃣ 대화형 응답 테스트 (Chat API)**

이전 대화 맥락을 포함하거나, AI에게 특정 역할을 부여(System Role)할 때 사용하는 방식입니다. Spring AI에서 주로 사용하는 방식이기도 합니다.

```bash
POST http://localhost:11434/api/chat 

-d '{
  "model": "qwen2.5:3b",
  "messages": [
    {
      "role": "system",
      "content": "당신은 친절하고 전문적인 자바 개발자입니다."
    },
    {
      "role": "user",
      "content": "로컬 LLM을 사용하는 이유가 뭐야?"
    }
  ],
  "stream": false
}'
```

#### **2️⃣ API 응답 결과 분석**

요청이 성공하면 다음과 같은 JSON 형식의 응답을 받게 됩니다.

```bash
{
    "model": "qwen2.5:3b",
    "created_at": "2026-03-25T15:48:15.51317551Z",
    "message": {
        "role": "assistant",
        "content": "안녕하세요! 로컬 LLM을 사용하는 주요 이유는 다음과 같습니다:\n\n1. **개인정보 보안**: 클라우드 서비스에 데이터를 전송하지 않아 민감한 정보 유출 위험을 최소화할 수 있습니다. 특히 의료, 금융 등 특정 산업에서 필수적입니다.\n2. **비용 절감**: 고주파 요청 시 클라우드 API 비용 대비 로컬 실행이 경제적입니다. 예를 들어, 내부 애플리케이션에서 실시간 처리를 할 때 네트워크 비용이 크게 절감됩니다.\n3. **실시간 성능**: 네트워크 지연 없이 즉시 응답이 가능해 실시간 챗봇이나 IoT 시스템에 적합합니다.\n4. **맞춤형 모델 튜닝**: 로컬에서 모델을 직접 훈련해 특정 용도(예: 한국어 처리)에 최적화할 수 있습니다.\n5. **법적 규제 준수**: 데이터 주거 요구사항을 충족하기 위해 로컬 환경에서 실행하는 경우가 많습니다.\n\n예를 들어, **Llama 3**, **Mistral** 등 소형 LLM 모델은 로컬 머신에 설치해 사용할 수 있습니다. Java 개발자로서는 Hugging Face Java SDK를 활용해 로컬 LLM을 효율적으로 통합할 수 있습니다.\n\n추가로 궁금한 점이 있으면 알려주세요! 😊",
        "thinking": "Okay, the user is asking why we use a local LLM. Let me think about this.\n\nFirst, I should recall what a local LLM is. Local LLM refers to running a large language model on the user's own machine instead of using a cloud-based service. So the main reasons would be privacy, cost, performance, maybe security.\n\nPrivacy is a big one. If you're using a cloud-based LLM, your data gets sent to the server, which might be a privacy concern. By using local, you keep the data on your machine.\n\nCost could be another point. Cloud services like OpenAI or Anthropic have costs per request. If you're using it locally, you don't have to pay for each query, but you need to set up the hardware. Maybe for some use cases, it's cheaper.\n\nPerformance: Local models might be faster since there's no network latency. Especially if you're doing real-time applications, local processing could be better.\n\nSecurity: Keeping the model on your own machine reduces the risk of data breaches or unauthorized access. Also, you control the model's access.\n\nAnother point is customization. Maybe you can fine-tune the local model to your specific needs without sharing data with the provider.\n\nWait, but some LLMs are too big to run locally. Like, GPT-3 is huge. But there are smaller models like Llama 2, Mistral, etc., that can be run locally with enough hardware.\n\nAlso, maybe the user is thinking about edge computing scenarios where you need to process data without internet.\n\nI should structure the answer clearly. Start with the main reasons: privacy, cost, performance, security, customization. Maybe mention specific examples of models that can be run locally.\n\nWait, the user is asking in Korean. The question is \"로컬 LLM을 사용하는 이유가 뭐야?\" which translates to \"Why do we use a local L:LM?\" So I need to answer in Korean.\n\nWait, the user wrote the question in Korean, but the instructions say to respond as a friendly and professional Java developer. Hmm, the user is asking in Korean, so I should respond in Korean.\n\nWait, the user's message is in Korean: \"로컬 LLM을 사용하는 이유가 뭐야?\" which means \"What's the reason for using a local LLM?\"\n\nBut the assistant's instructions say to be a friendly and professional Java developer. So the response should be in Korean, since the user is asking in Korean.\n\nWait, but maybe the user expects the answer in English? Let me check the initial instructions.\n\nThe user says: \"로컬 LLM을 사용하는 이유가 뭐야?\" which is Korean. The assistant's role is to be a friendly and professional Java developer. The user might be Korean, so the answer should be in Korean.\n\nBut the problem says: \"당신은 친절하고 전문적인 자바 개발자입니다.\" So the assistant should respond in the same language as the user. Since the user wrote in Korean, the answer should be in Korean.\n\nWait, but the user might have intended to ask in English but wrote in Korean. Hmm, but the question is in Korean. Let me confirm.\n\nThe user's message is in Korean: \"로컬 LLM을 사용하는 이유가 뭐야?\" So the answer should be in Korean.\n\nBut the initial problem says: \"You are a friendly and professional Java developer.\" So the answer should be in Korean.\n\nWait, but in the problem statement, the user wrote the question in Korean. So I need to respond in Korean.\n\nLet me think about the key points in Korean.\n\nFirst, list the reasons:\n\n1. 개인정보 보안: 로컬에서 실행하면 데이터가 클라우드에 전송되지 않아 개인정보 유출 위험 감소\n2. 비용 절감: 클라우드 서비스 사용 비용 대비 로컬 실행 시 비용 절감 가능 (특히 고주파 요청 시)\n3. 실시간 처리: 네트워크 지연 없이 즉시 응답 가능\n4. 정책 및 법적 규제: 특정 산업에서 데이터 주거 요구사항 충족\n5. 맞춤형 모델 튜닝: 로컬에서 모델을 직접 훈련해 특정 작업에 최적화 가능\n\nAlso, mention examples like Llama 3, Mistral, etc. that can be run locally.\n\nWait, but the user might not know about specific models. Maybe keep it general.\n\nAlso, as a Java developer, I can mention that Java has libraries like Hugging Face Java SDK for local LLMs, but maybe that's too specific.\n\nWait, the user is asking for the reasons, not how to implement. So focus on the reasons.\n\nLet me draft the answer in Korean.\n\n\"안녕하세요! 로컬 LLM을 사용하는 주요 이유는 다음과 같습니다:\n\n1. **개인정보 보안**: 클라우드 서비스에 데이터를 전송하지 않아 개인정보 유출 위험을 최소화할 수 있습니다. 특히 민감한 데이터 처리 시 매우 중요합니다.\n2. **비용 절감**: 클라우드 API 호출 비용 대비 로컬에서 실행하는 경우, 고주파 요청 시 비용이 크게 절감됩니다.\n3. **실시간 처리 성능**: 네트워크 지연 없이 즉시 응답을 가능하게 하여 실시간 애플리케이션에 적합합니다.\n4. **맞춤형 튜닝**: 로컬에서 모델을 직접 훈련하거나 조정해 특정 용도에 최적화할 수 있습니다.\n5. **법적 규제 준수**: 특정 산업 분야에서 데이터 주거 요구사항을 충족하기 위해 로컬 실행이 필요할 수 있습니다.\n\n예를 들어, Llama 3, Mistral 등 소형 LLM 모델은 로컬 머신에 설치해 사용할 수 있습니다. Java 개발자로서는 Hugging Face Java SDK와 같은 도구를 활용해 로컬 LLM을 효율적으로 통합할 수 있습니다.\n\n추가로 궁금한 점이 있으면 알려주세요! 😊\"\n\nWait, but I should check if the terms are correctly translated. For example, \"개인정보 보안\" is correct. \"비용 절감\" is cost reduction. \"실시간 처리 성능\" is real-time processing performance. \"맞춤형 튜닝\" is custom tuning.\n\nAlso, the example models: Llama 3 is the latest, but maybe the user is familiar with Llama 2. But it's okay.\n\nAlso, the Java part: Hugging Face has a Java SDK, so that's a good point.\n\nThe answer seems good. Let me make sure it's friendly and professional.\n\nYes, the tone is friendly with emojis and the closing. The points are clear.\n\nI think that's a good answer.\n"
    },
    "done": true,
    "done_reason": "stop",
    "total_duration": 132036650359, // 전체 처리 시간 (나노초 단위)
    "load_duration": 61223537,      // 모델 로딩 시간
    "prompt_eval_count": 45,        // 질문에 사용된 토큰 수
    "prompt_eval_duration": 710243481,
    "eval_count": 1809,             // 답변에 생성된 토큰 수
    "eval_duration": 130615466730
}
```

- **total_duration:** 내 컴퓨터가 이 답변을 만드는 데 얼마나 걸렸는지 확인할 수 있는 지표입니다.
- **eval_count:** AI가 얼마나 긴 답변을 생성했는지(토큰 수)를 나타냅니다.

<aside>
💡

**도커 환경 주의사항**

사용자님의 환경처럼 **Docker**로 실행 중인 경우, 외부(호스트 PC)에서 `localhost:11434`로 접근하기 위해서는 컨테이너 실행 시 반드시 `-p 11434:11434` 옵션이 설정되어 있어야 합니다.

**작동 확인 팁:** 웹 브라우저 주소창에 `http://localhost:11434`를 입력했을 때 "Ollama is running"이라는 문구가 뜨면 API 호출 준비가 완벽히 끝난 상태입니다.

</aside>

---

## **4. Spring AI에 OLLAMA 통합**

이제 로컬에서 구동 중인 Ollama 엔진을 Spring Boot 애플리케이션에 연결할 차례입니다.

### Dependencies 추가

`build.gradle` 파일에 Ollama 연동을 위한 전용 스타터를 추가합니다. (기존 OpenAI 스타터가 있어도 무방합니다.)

```groovy
dependencies {
    // 🦙 Ollama 연동을 위한 스타터
    implementation 'org.springframework.ai:spring-ai-ollama-spring-boot-starter'

    // 기존 Gemini/OpenAI 호환 스타터가 있다면 주석 처리
    // implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
}
```

### **application.yml 설정**

두 모델의 설정을 계층적으로 구성합니다. 이렇게 하면 코드에서 모델의 이름을 명시하여 선택적으로 호출할 수 있습니다

```bash
spring:
  application:
    name: lesson

  ai:
    openai: # 유지
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

        # 4. 엔드포인트 경로: 대화형 API를 호출하기 위한 표준 경로입니다.
        completions-path: "/chat/completions"

    ollama: # 추가
      # Ollama 서버 주소 (도커 포트 포워딩 11434 확인)
      base-url: http://localhost:11434
      chat:
        options:
          # 우리가 설치하고 테스트한 모델명
          model: qwen2.5:3b
          # 창의성 조절 (0.7은 일반적인 대화에 적합)
          temperature: 0.7
          
          # 3. 샘플링 제어 (Top-K, Top-P) - 방금 학습한 내용!
          # 확률 상위 40개 후보군으로 제한
          top-k: 40
          # 누적 확률 90% 이내의 단어만 선택
          top-p: 0.9
          
          # 4. 답변 길이 및 품질 제어
          # 답변의 최대 토큰(단어 조각) 수를 제한 (너무 짧으면 답변이 끊김)
          num-predict: 10000
          # 동일한 문구 반복을 방지하는 정도 (1.1 이상 권장)
          repeat-penalty: 1.1

```

LLM이 다음 단어를 결정할 때, 수만 개의 단어 후보 중 어떤 것을 선택할지 제어하는 핵심 매개변수입니다. 이를 통해 답변의 **정확도**와 **창의성**을 조절할 수 있습니다.

**Top-K (상위 K개 추출)**

다음 토큰 후보를 **확률이 높은 순서대로 K개만큼만** 남기는 방식입니다.

- **작동 원리:** `topK=40` 설정 시, 확률 상위 40개 토큰 외에는 모두 후보에서 탈락시킵니다.
- **특징:** 후보군을 고정된 개수로 제한하여 터무니없는 답변이 나올 확률을 줄입니다.
- **예시:** * 전체 어휘: 50,000개
    - `topK=40` 설정 → "사과"(15%), "포도"(12%) ... 등 **상위 40개**만 후보로 인정하고 나머지는 버림.

**Top-P (누적 확률 P%까지 추출)**

누적 확률이 **설정한 P값(0.0 ~ 1.0)에 도달할 때까지**의 토큰들만 후보로 포함하는 방식입니다. 'Nucleus Sampling'이라고도 불립니다.

- **작동 원리:** `topP=0.9` 설정 시, 확률이 높은 순서대로 더해서 합계가 90%가 되는 지점까지만 후보로 삼습니다.
- **특징:** 문맥에 따라 후보군의 개수가 **동적으로 변합니다.** (확신이 높은 문맥에선 적은 후보, 모호한 문맥에선 많은 후보 선택)
- **예시:**
    - 확률 분포: "안녕"(50%), "좋은"(25%), "반갑"(15%), "훌륭"(5%) ...
    - `topP=0.9` 설정 → 50%+25%+15% = 90%이므로, 앞의 **3개 토큰만** 후보로 선택.

| **목적**       | **Top-K** | **Top-P** | **기대 효과**            | **추천 분야**          |
|--------------|-----------|-----------|----------------------|--------------------|
| **정확성 우선**   | 10~20     | 0.7~0.8   | 일관되고 예측 가능한 답변 생성    | 기술 문서 요약, 코드 생성    |
| **균형 잡힌 답변** | **40~50** | **0.9**   | **정확성과 창의성의 적절한 조화** | **일반적인 챗봇 대화**     |
| **창의성 우선**   | 80~100    | 0.95      | 다양하고 의외성 있는 답변 생성    | 소설 쓰기, 아이디어 브레인스토밍 |

### 실습 : Ollama 기반 AI API 응답 확인

로컬 모델로 변경 후, 다음 3가지 포인트를 중점적으로 체크해 보세요.

| **테스트 항목**          | **체크포인트**                           | **기대 결과**                |
|---------------------|-------------------------------------|--------------------------|
| **응답 속도 (Latency)** | 네트워크 구간이 없으므로 응답 시작 속도가 빨라졌나요?      | API 호출 후 100ms 이내에 반응 시작 |
| **한국어 품질**          | Qwen3 특유의 매끄러운 한국어 문체가 나오나요?        | 어색한 번역투가 아닌 자연스러운 문장     |
| **자원 사용량**          | AI 응답 중에 내 컴퓨터의 CPU/GPU 점유율이 올라가나요? | 팬 소음이나 리소스 모니터의 변화 확인    |

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
        return ApiResponse.ok(chatService.chatWithContext(request.getMessage()));
    }

}
```

```java
@Slf4j
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
            log.info(e.getMessage(), e);
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
            log.info(e.getMessage(), e);
            throw new DomainException(DomainExceptionCode.AI_RESPONSE_ERROR);
        }
    }
}

```

#### **트러블슈팅: 응답이 너무 느리거나 끊긴다면?**

만약 로컬 모델의 성능이 만족스럽지 않다면 `application.yml`에서 다음 설정을 조정해 보세요.

1. **답변이 중간에 잘림:** `num-predict` 값을 `2000` 이상으로 높이세요.
2. **답변 속도가 너무 느림:** 모델을 더 가벼운 버전인 `qwen2.5:1.5b` 또는 `qwen2.5:0.5b`로 변경해 보세요.
3. **반복되는 말만 함:** `repeat-penalty` 옵션을 `1.2` 정도로 높여보세요.

---

## 5. 실습: Ollama 기반 AI Chat API 구현 및 테스트

Spring Boot 애플리케이션에서 비즈니스 로직을 `Service` 계층으로 분리하여 구조화하고, 다양한 시나리오별 AI 응답을 확인해 봅니다.

### Service 계층 구현 (AiService)

```java
@Service
@RequiredArgsConstructor
public class OllamaService {

    private final ChatClient chatClient;

    // 1. 단순 채팅
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    // 2. 역할 기반 및 옵션 조정 채팅 (System Prompt & Temperature)
    public String chatWithOptions(String systemPrompt, String message, Double temperature) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .options(OllamaOptions.builder()
                        .temperature(temperature)
                        .build())
                .call()
                .content();
    }
}
```

### Controller 계층 구현 (AiChatController)

```java
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OllamaChatRequest {

    String systemPrompt; // AI 역할 설정 (페르소나)

    String message;      // 사용자 질문 내용

    Double temperature;  // 답변의 창의성 정도 (0.0 ~ 1.0)

}
```

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/ollama")
public class OllamaController {

    private final OllamaService ollamaService;

    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody OllamaChatRequest request) {
        return ApiResponse.ok(ollamaService.chat(request.getMessage()));
    }

    @PostMapping("/chat/options")
    public ApiResponse<String> chatWithOptions(@RequestBody OllamaChatRequest request) {
        return ApiResponse.ok(
                ollamaService.chatWithOptions(request.getSystemPrompt(), request.getMessage(),
                        request.getTemperature()));
    }

}
```

### API 테스트 가이드

**1) 기본 채팅 테스트**

```bash
curl -X POST http://localhost:8080/api/ai/ollama/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Spring AI에 대해 3문장으로 설명해줘"}'
```

**2) 역할 기반 채팅 (Temperature 조정 비교 테스트)**

동일한 질문이라도 온도를 다르게 설정하여 로컬 모델(Qwen 2.5)의 반응을 비교해 보세요.

- **낮은 Temperature (정확성/일관성 우선)**

    ```java
    curl -X POST http://localhost:8080/api/ai/ollama/options \
      -H "Content-Type: application/json" \
      -d '{
      "systemPrompt": "당신은 초등학생 선생님입니다. 학생들에게 설명해주세요",
      "message": "미래의 AI에 대해 창의적으로 상상해봐",
      "temperature": 0.1
    }'
    ```

- **높은 Temperature (창의성/다양성 우선)**

    ```java
    curl -X POST http://localhost:8080/api/ai/ollama/options \
      -H "Content-Type: application/json" \
      -d '{
      "systemPrompt": "당신은 초등학생 선생님입니다. 학생들에게 설명해주세요",
      "message": "미래의 AI에 대해 창의적으로 상상해봐",
      "temperature": 0.9
    }'
    ```

---

## 6. 실습 과제: 로컬 LLM 환경에서의 `PersistentChatService`기능 검증

기존에 구현된 **히스토리 기반 대화(Context Management)** 및 **자동 요약(Summary)** 로직이 로컬에서 구동 중인 Ollama 모델에서도 논리적으로 올바르게 수행되는지 확인하고 평가합니다.

#### **1) 히스토리 기반 대화 테스트 (Context Retention)**

- **검증 내용:** 로컬 모델이 이전 대화의 맥락(`MAX_HISTORY_MESSAGES`)을 유지하며 답변하는지 확인합니다.
- **테스트 시나리오:**
    1. `POST /api/ai/chat` (ID 없이 호출): "내 이름은 '제미니'야. 기억해줘."
    2. 반환된 `conversationId`를 사용하여 재호출: "내 이름이 뭐라고 했지?"
- **평가 포인트:** 로컬 모델(`qwen2.5:3b`)이 DB에 저장된 이전 메시지를 `recentMessages` 리스트를 통해 정상적으로 전달받아 인지하는가?

#### **2) 자동 요약 기능 검증 (Long-term Memory)**

- **검증 내용:** 대화가 5개(`MAX_HISTORY_MESSAGES`)를 초과했을 때 실행되는 `generateSummary()`가 로컬에서 정상 작동하는지 확인합니다.
- **테스트 시나리오:**
    1. 연속적으로 6번 이상의 질문을 던져 요약 로직을 트리거합니다.
    2. `chat_message` 테이블에서 이전 메시지들이 `INACTIVE`로 변하고, `SUMMARY` 타입의 메시지가 새롭게 생성되었는지 확인합니다.
- **평가 포인트:** 로컬 모델이 긴 대화 내용을 요약할 때 핵심 키워드를 놓치지 않는가?

| **항목**        | **평가 및 분석**                                                                                                        |
|---------------|--------------------------------------------------------------------------------------------------------------------|
| **추상화 수준**    | `org.springframework.ai.chat.messages.Message` 인터페이스를 사용하여 **합격**. 모델이 바뀌어도 `mapToSpringAiMessage` 로직을 수정할 필요가 없음. |
| **요약 로직**     | `generateSummary` 메서드에서 `chatClient`를 재귀적으로 호출하는 구조. 로컬 모델 사양이 낮을 경우 요약 시간이 길어질 수 있으므로 **성능 모니터링 필요**.             |
| **토큰 사용량 기록** | Ollama API도 `Usage` 정보를 제공하므로, 기존 `usage.getPromptTokens()` 등의 로직이 에러 없이 DB에 저장됨.                                  |
| **안정성**       | 로컬 서버(Docker)가 꺼져있을 경우 `AI_RESPONSE_ERROR`가 발생함. 예외 처리 로직이 이미 구현되어 있어 **안전함**.                                     |

---

## **7. 부록: Ollama 실무 활용 및 트러블슈팅**

#### **용도별 추천 모델 리스트**

로컬 환경의 자원(RAM) 상태와 목적에 따라 적절한 모델을 선택하세요.

| **카테고리**       | **추천 모델 (Ollama Pull)**     | **특징 및 추천 이유**                  |
|----------------|-----------------------------|---------------------------------|
| **다국어/한국어 우수** | `qwen2.5:3b`, `qwen2.5:7b`  | **현재 실습 메인 모델.** 한국어 이해도가 가장 높음 |
| **코드 생성 특화**   | `deepseek-coder:6.7b`       | 코딩 테스트 및 자바 로직 생성 능력이 매우 뛰어남    |
| **경량 고속 모델**   | `phi3:mini`, `gemma2:2b`    | 저사양 PC에서도 매우 빠른 응답 속도를 보여줌      |
| **글로벌 범용**     | `llama3.2:3b`, `mistral:7b` | 메타와 미스트랄의 대표 모델로 안정적인 성능 제공     |

#### **Ollama 핵심 명령어 정리**

Docker 환경에서 실행할 경우 명령어 앞에 `docker exec -it ollama`를 붙여야 함을 기억하세요.

### **📦 모델 관리**

- `ollama list` : 현재 내 로컬에 설치된 모델 목록 확인
- `ollama pull <model>` : 새로운 모델 다운로드 (예: `ollama pull qwen2.5:3b`)
- `ollama rm <model>` : 불필요한 모델 삭제 (용량 확보)
- `ollama run <model>` : 터미널에서 즉시 대화형 모드로 모델 실행

### **⚙️ 시스템 및 서버 관리**

- `ollama ps` : 현재 메모리(RAM)에 올라가 실행 중인 모델 확인
- `ollama serve` : (로컬 설치 시) Ollama 서버 엔진 시작
- `curl http://localhost:11434` : 서버가 정상 응답하는지 상태 체크

#### **트러블슈팅 (Q&A)**

**❓ Q1: "Connection Refused" 오류가 발생합니다.**

- **원인:** Ollama 서버가 꺼져 있거나 포트가 충돌한 경우입니다.
- **해결:**
    1. `curl http://localhost:11434`로 서버 응답 확인.
    2. Docker 컨테이너가 `Up` 상태인지 확인 (`docker ps`).
    3. Windows 방화벽에서 `11434` 포트가 허용되어 있는지 체크하세요.

**❓ Q2: 모델 응답이 너무 느립니다 (버벅임).**

- **원인:** 하드웨어(RAM/GPU) 자원 부족 또는 모델이 너무 무거운 경우입니다.
- **해결:**
    1. **경량 모델로 교체:** `qwen2.5:0.5b`나 `phi3:mini` 같이 파라미터가 적은 모델을 사용하세요.
    2. **GPU 가속:** NVIDIA 그래픽카드가 있다면 Docker 설정에서 GPU 할당을 확인하세요.
    3. **옵션 조정:** `num-predict` 값을 줄여 한 번에 생성하는 답변 길이를 제한하세요.

**❓ Q3: "Model not found" 오류가 발생합니다.**

- **원인:** 모델명이 정확하지 않거나 다운로드가 되지 않은 상태입니다.
- **해결:**
    1. `ollama list`로 설치된 이름을 정확히 확인하세요. (예: `qwen2.5:3b`와 `qwen2.5`는 다릅니다.)
    2. 반드시 소문자로 입력해야 합니다. (`Qwen` ❌ -> `qwen` ✅)

**❓ Q4: Spring Boot 연동 시 Timeout이 발생합니다.**

- **원인:** 모델이 답변을 생성하는 시간이 Spring의 기본 타임아웃 설정을 초과한 경우입니다.
- **해결:** `application.yml`에서 타임아웃 시간을 넉넉하게 늘려주세요.

    ```java
    spring:
      ai:
        ollama:
          base-url: http://localhost:11434
          chat:
            options:
              timeout: 120s  # 2분으로 확장
    ```