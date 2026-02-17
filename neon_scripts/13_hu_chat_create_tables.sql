-- HU-CHAT: Crear tablas para mensajería interna
-- Ejecutar en el schema sigma

CREATE TABLE IF NOT EXISTS sigma.chat_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(120) NOT NULL,
    sender_id VARCHAR(40) NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    receiver_id VARCHAR(40) NOT NULL,
    activity_id INTEGER,
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sigma.chat_attachment (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    size_bytes BIGINT,
    storage_path VARCHAR(500) NOT NULL,
    CONSTRAINT fk_chat_attachment_message
        FOREIGN KEY (message_id)
        REFERENCES sigma.chat_message(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_message_conversation_created
    ON sigma.chat_message (conversation_id, created_at);

CREATE INDEX IF NOT EXISTS idx_chat_attachment_message
    ON sigma.chat_attachment (message_id);
