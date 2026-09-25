-- [Step 1] 원본 문서 관리 테이블
CREATE TABLE vector_documents
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(), -- 문서 고유 ID
    file_name    VARCHAR(255) NOT NULL,                           -- 파일명
    content      TEXT         NOT NULL,                           -- 문서 전체 내용
    content_type VARCHAR(50)  NOT NULL,                           -- 파일 확장자 (PDF, TXT 등)
    metadata     TEXT,                                            -- 추가 정보 (저자, 페이지 수 등)
    chunk_count  INTEGER      NOT NULL DEFAULT 0,                 -- 분할된 조각 개수
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [Step 2] 벡터 데이터 저장 테이블 (청크 단위)
CREATE TABLE vector_store
(
    id         UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    content    TEXT      NOT NULL,
    metadata   JSONB,
    embedding  vector(3072),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- [Step 3] 검색 성능 최적화 인덱스
CREATE INDEX idx_documents_filename ON vector_documents (file_name);

-- 메타데이터 필터링 가속화
CREATE INDEX idx_vector_store_metadata ON vector_store USING gin (metadata);