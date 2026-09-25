# 🤖 스파르타 MSA 과정 교안 예시 코드

> 스파르타 MSA 과정 **Part 02** 강의에서 사용하는 Spring Boot + Spring AI 기반 예시 프로젝트입니다.
> Part 01에서 만든 커머스 도메인 위에 LLM 연동 · RAG · AI Agent를 한 단계씩 얹어갑니다.
> 주차·일차별 브랜치로 나뉘어 있어, 강의 진도에 맞춰 코드를 따라가며 학습할 수 있습니다.

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.11-6DB33F?logo=springboot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.14.4-02303A?logo=gradle&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?logo=flyway&logoColor=white)

---

## 📚 목차

- [이 저장소에 대하여](#-이-저장소에-대하여)
- [브랜치 구성](#-브랜치-구성)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [시작하기](#️-시작하기)

---

## 📌 이 저장소에 대하여

Part 02는 **Part 01에서 만든 커머스 서비스에 AI를 붙이는 과정**입니다.
`main` 브랜치는 AI 코드가 없는 베이스이므로, 실습 코드는 아래 주차별 브랜치에서 확인하세요.

| 주차          | 다루는 것                                                                                 |
|-------------|---------------------------------------------------------------------------------------|
| **Week 03** | Spring AI 입문 — `ChatClient`, 프롬프트 설계, 대화 맥락과 토큰, 스트리밍, 대화 영속화, 이미지 분석, 로컬 LLM(Ollama) |
| **Week 04** | 임베딩과 벡터 스토어(pgvector), RAG, Function Calling과 커스텀 Advisor                             |

```bash
# 바로 최종 결과물부터 보고 싶다면
git checkout week-04/day-03
```

---

## 🌿 브랜치 구성

각 주차는 `original`(시작 코드)과 `day-XX`(일차별 완성 코드) 브랜치로 구성됩니다.
`main`은 AI 기능이 붙기 전의 베이스 코드(= `week-03/original`)이며, `week-04/original`은 `week-03/day-04`와 동일한 지점에서 출발합니다.

|                   주차                   |       시작 코드        |      Day 01      |      Day 02      |      Day 03      |      Day 04      |
|:--------------------------------------:|:------------------:|:----------------:|:----------------:|:----------------:|:----------------:|
|      **Week 03**<br/>Spring AI 입문      | `week-03/original` | `week-03/day-01` | `week-03/day-02` | `week-03/day-03` | `week-03/day-04` |
| **Week 04**<br/>RAG · Function Calling | `week-04/original` | `week-04/day-01` | `week-04/day-02` | `week-04/day-03` |        —         |

| 브랜치                | 한 줄 요약                                                            |
|--------------------|-------------------------------------------------------------------|
| `week-03/original` | Part 01에서 완성한 상품·주문·회원 도메인 (AI 기능 없음)                             |
| `week-03/day-01`   | `ChatClient` 첫 호출 — 프롬프트 템플릿, System Message, 구조화 출력, ChatOptions |
| `week-03/day-02`   | 대화 맥락 유지, 토큰 사용량 측정, SSE 스트리밍 응답                                  |
| `week-03/day-03`   | 대화 이력 DB 영속화, 이미지 분석(Vision)                                      |
| `week-03/day-04`   | 로컬 LLM(Ollama) 연동과 추론 파라미터 튜닝                                     |
| `week-04/original` | Week 04 시작 코드 (`week-03/day-04`와 동일)                              |
| `week-04/day-01`   | 임베딩과 pgvector 벡터 스토어, 문서 청킹·유사도 검색                                |
| `week-04/day-02`   | RAG(검색 증강 생성) — 근거 문서 기반 답변과 출처 표시                                |
| `week-04/day-03`   | Function Calling(`@Tool`)과 커스텀 Advisor 기반 대화 메모리                  |

```bash
# 원하는 강의 단계의 브랜치로 이동
git checkout week-03/day-01
```

---

## 🛠 기술 스택

| 분류                   | 사용 기술                                     |
|----------------------|-------------------------------------------|
| **Language / Build** | Java 21, Gradle 8.14.4                    |
| **Framework**        | Spring Boot 3.3.11, Spring Cloud 2023.0.2 |
| **Web Server**       | Undertow (Tomcat 대체)                      |
| **Persistence**      | Spring Data JPA, QueryDSL 5.0, Flyway     |
| **Database**         | PostgreSQL                                |
| **Communication**    | Spring Cloud OpenFeign, Spring Retry      |
| **Validation**       | Spring Validation (Hibernate Validator)   |
| **Mapping**          | MapStruct 1.5, Lombok                     |
| **API Docs**         | springdoc-openapi (Swagger UI)            |
| **Monitoring**       | Spring Boot Actuator                      |
| **Test**             | JUnit 5, Spring Boot Test                 |

---

## 📁 프로젝트 구조

```
sparta-msa-lesson-part-02
├── build.gradle
├── settings.gradle
├── gradle/wrapper
└── src
    ├── main
    │   ├── java/com/sparta/msa/lesson
    │   │   ├── domain
    │   │   │   ├── category        # 카테고리 엔티티 · 리포지토리
    │   │   │   ├── order           # 주문 · 주문상품 (엔티티, 매퍼, 서비스)
    │   │   │   ├── product         # 상품 CRUD + QueryDSL 조회
    │   │   │   └── user            # 회원가입
    │   │   ├── global
    │   │   │   ├── config          # QueryDslConfig, SecurityConfig, SwaggerConfig
    │   │   │   ├── constants       # Constants, OrderStatus
    │   │   │   ├── exception       # DomainException, DomainExceptionCode, GlobalExceptionHandler
    │   │   │   └── response        # ApiResponse
    │   │   └── LessonApplication.java
    │   └── resources
    │       ├── db/migration
    │       │   ├── V1__init_table.sql
    │       │   ├── V2__create_users_table.sql
    │       │   └── V3__create_product_table.sql
    │       └── application.yml
    └── test/java/com/sparta/msa/lesson
        └── LessonApplicationTests.java
```

---

## ▶️ 시작하기

### 1. 사전 준비

- **JDK 21**
- **PostgreSQL** — `localhost:5432`에 `sparta` 데이터베이스 생성

| 항목       | 값                                         |
|----------|-------------------------------------------|
| URL      | `jdbc:postgresql://localhost:5432/sparta` |
| Username | `postgres`                                |
| Password | `postgres`                                |

```bash
# Docker로 PostgreSQL 실행 (선택)
docker run -d --name sparta-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=sparta \
  -p 5432:5432 postgres
```

### 2. 클론 및 빌드

```bash
git clone https://github.com/KDT-Java-5/sparta-msa-lesson-part-02.git
cd sparta-msa-lesson-part-02
git checkout main

./gradlew build
```

### 3. 실행

```bash
./gradlew bootRun
```

| 주소                                          | 설명         |
|---------------------------------------------|------------|
| http://localhost:8080                       | 애플리케이션     |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |

### 🔗 API 목록

|  Method  | URL                  | 설명                     |
|:--------:|----------------------|------------------------|
|  `GET`   | `/api/products`      | 상품 목록 조회               |
|  `GET`   | `/api/products/{id}` | 상품 단건 조회               |
|  `POST`  | `/api/products`      | 상품 등록 (201 Created)    |
|  `PUT`   | `/api/products/{id}` | 상품 수정                  |
| `DELETE` | `/api/products/{id}` | 상품 삭제 (204 No Content) |
|  `POST`  | `/api/users`         | 회원가입 (201 Created)     |

### 4. 테스트

```bash
./gradlew test
```

---

<div align="center">

**Happy Coding! 🎉**

</div>
