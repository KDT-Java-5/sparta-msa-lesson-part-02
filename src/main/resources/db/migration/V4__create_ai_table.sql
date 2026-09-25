CREATE TABLE chat_conversations
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    title      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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