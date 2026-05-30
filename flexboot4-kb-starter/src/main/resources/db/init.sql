-- ============================================================
-- FlexBoot4 KB Starter initialization (PostgreSQL)
-- ============================================================

CREATE TABLE IF NOT EXISTS knowledge_base (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(32) DEFAULT 'private',
    owner_id VARCHAR(64),
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE knowledge_base IS '知识库表';
COMMENT ON COLUMN knowledge_base.type IS '知识库类型：public/team/private';
COMMENT ON COLUMN knowledge_base.owner_id IS '创建者用户ID';

CREATE INDEX IF NOT EXISTS idx_knowledge_base_owner ON knowledge_base(owner_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_type_status ON knowledge_base(type, status);


CREATE TABLE IF NOT EXISTS kb_member (
    id VARCHAR(64) PRIMARY KEY,
    kb_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE kb_member IS '知识库成员表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_member_kb_user_alive
    ON kb_member(kb_id, user_id)
    WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_kb_member_user ON kb_member(user_id);


CREATE TABLE IF NOT EXISTS kb_file_tree (
    id VARCHAR(64) PRIMARY KEY,
    kb_id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    file_id VARCHAR(64),
    sort_order INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE kb_file_tree IS '知识库文件树';
COMMENT ON COLUMN kb_file_tree.type IS '节点类型：FOLDER/FILE';

CREATE INDEX IF NOT EXISTS idx_kb_file_tree_kb_parent ON kb_file_tree(kb_id, parent_id);
CREATE INDEX IF NOT EXISTS idx_kb_file_tree_file ON kb_file_tree(file_id);


CREATE TABLE IF NOT EXISTS sys_file_parsed (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    full_text TEXT,
    page_count INTEGER DEFAULT 0,
    metadata JSONB,
    blocks JSONB,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_file_parsed IS '文件解析结果表';
COMMENT ON COLUMN sys_file_parsed.full_text IS '解析全文';
COMMENT ON COLUMN sys_file_parsed.metadata IS '解析元数据';
COMMENT ON COLUMN sys_file_parsed.blocks IS '结构化解析块';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_parsed_file_alive
    ON sys_file_parsed(file_id)
    WHERE del_flag = 0;


CREATE TABLE IF NOT EXISTS sys_file_chunk (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT,
    content_hash VARCHAR(128),
    embedding_id VARCHAR(160),
    embedding_model VARCHAR(120),
    token_count INTEGER DEFAULT 0,
    embed_status VARCHAR(32) DEFAULT 'PENDING',
    page_number INTEGER,
    section_title VARCHAR(500),
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_file_chunk IS '文件内容分片表';
COMMENT ON COLUMN sys_file_chunk.content_hash IS '分片内容哈希';
COMMENT ON COLUMN sys_file_chunk.embedding_id IS '向量存储主键';
COMMENT ON COLUMN sys_file_chunk.embed_status IS '向量化状态';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_chunk_file_index_alive
    ON sys_file_chunk(file_id, chunk_index)
    WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_sys_file_chunk_file ON sys_file_chunk(file_id);
CREATE INDEX IF NOT EXISTS idx_sys_file_chunk_embed_status ON sys_file_chunk(embed_status);
