-- CMS 模块 PostgreSQL 初始化脚本（与 docs/sql/cms_pg.sql 保持一致）

CREATE TABLE IF NOT EXISTS cms_category (
    id VARCHAR(64) PRIMARY KEY,
    parent_id VARCHAR(64),
    category_name VARCHAR(200) NOT NULL,
    category_code VARCHAR(100),
    description TEXT,
    cover_file_id VARCHAR(64),
    sort_order INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    CONSTRAINT fk_cms_category_parent FOREIGN KEY (parent_id) REFERENCES cms_category(id) ON DELETE CASCADE,
    CONSTRAINT fk_cms_category_cover FOREIGN KEY (cover_file_id) REFERENCES sys_file(id) ON DELETE SET NULL
);

COMMENT ON TABLE cms_category IS 'CMS 栏目表';
COMMENT ON COLUMN cms_category.id IS '主键ID';
COMMENT ON COLUMN cms_category.parent_id IS '父栏目ID，NULL表示根栏目';
COMMENT ON COLUMN cms_category.category_name IS '栏目名称';
COMMENT ON COLUMN cms_category.category_code IS '栏目编码';
COMMENT ON COLUMN cms_category.description IS '栏目描述';
COMMENT ON COLUMN cms_category.cover_file_id IS '栏目封面文件ID，关联sys_file.id';
COMMENT ON COLUMN cms_category.sort_order IS '排序号';
COMMENT ON COLUMN cms_category.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN cms_category.version IS '版本号（乐观锁预留）';
COMMENT ON COLUMN cms_category.del_flag IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN cms_category.create_time IS '创建时间';
COMMENT ON COLUMN cms_category.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN cms_category.create_by IS '创建人';
COMMENT ON COLUMN cms_category.last_modify_by IS '最后修改人';
COMMENT ON COLUMN cms_category.remark IS '备注';

CREATE INDEX IF NOT EXISTS idx_cms_category_parent_id ON cms_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_cms_category_category_code ON cms_category(category_code);
CREATE INDEX IF NOT EXISTS idx_cms_category_status ON cms_category(status);


CREATE TABLE IF NOT EXISTS cms_article (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    category_id VARCHAR(64),
    author VARCHAR(100),
    cover_file_id VARCHAR(64),
    summary TEXT,
    content TEXT,
    view_count INTEGER DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    publish_time TIMESTAMP,
    reviewer_id VARCHAR(64),
    review_time TIMESTAMP,
    review_comment TEXT,
    sort_order INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    CONSTRAINT fk_cms_article_category FOREIGN KEY (category_id) REFERENCES cms_category(id) ON DELETE SET NULL,
    CONSTRAINT fk_cms_article_cover FOREIGN KEY (cover_file_id) REFERENCES sys_file(id) ON DELETE SET NULL,
    CONSTRAINT fk_cms_article_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id) ON DELETE SET NULL
);

COMMENT ON TABLE cms_article IS 'CMS 文章表';
COMMENT ON COLUMN cms_article.id IS '主键ID';
COMMENT ON COLUMN cms_article.title IS '文章标题';
COMMENT ON COLUMN cms_article.category_id IS '栏目ID，关联cms_category.id';
COMMENT ON COLUMN cms_article.author IS '作者';
COMMENT ON COLUMN cms_article.cover_file_id IS '封面文件ID，关联sys_file.id';
COMMENT ON COLUMN cms_article.summary IS '摘要';
COMMENT ON COLUMN cms_article.content IS '文章正文内容（TEXT）';
COMMENT ON COLUMN cms_article.view_count IS '浏览次数';
COMMENT ON COLUMN cms_article.like_count IS '点赞次数';
COMMENT ON COLUMN cms_article.status IS '文章状态：DRAFT/PENDING/PUBLISHED/REJECTED';
COMMENT ON COLUMN cms_article.publish_time IS '发布时间';
COMMENT ON COLUMN cms_article.reviewer_id IS '审核人ID，关联sys_user.id';
COMMENT ON COLUMN cms_article.review_time IS '审核时间';
COMMENT ON COLUMN cms_article.review_comment IS '审核意见';
COMMENT ON COLUMN cms_article.sort_order IS '排序号';
COMMENT ON COLUMN cms_article.version IS '版本号（乐观锁预留）';
COMMENT ON COLUMN cms_article.del_flag IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN cms_article.create_time IS '创建时间';
COMMENT ON COLUMN cms_article.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN cms_article.create_by IS '创建人';
COMMENT ON COLUMN cms_article.last_modify_by IS '最后修改人';
COMMENT ON COLUMN cms_article.remark IS '备注';

CREATE INDEX IF NOT EXISTS idx_cms_article_category_id ON cms_article(category_id);
CREATE INDEX IF NOT EXISTS idx_cms_article_status ON cms_article(status);
CREATE INDEX IF NOT EXISTS idx_cms_article_publish_time ON cms_article(publish_time);
CREATE INDEX IF NOT EXISTS idx_cms_article_create_by ON cms_article(create_by);


CREATE TABLE IF NOT EXISTS cms_tag (
    id VARCHAR(64) PRIMARY KEY,
    tag_name VARCHAR(100) NOT NULL UNIQUE,
    tag_color VARCHAR(20),
    use_count INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT
);

COMMENT ON TABLE cms_tag IS 'CMS 标签表';
COMMENT ON COLUMN cms_tag.id IS '主键ID';
COMMENT ON COLUMN cms_tag.tag_name IS '标签名称（唯一）';
COMMENT ON COLUMN cms_tag.tag_color IS '标签颜色';
COMMENT ON COLUMN cms_tag.use_count IS '使用次数';
COMMENT ON COLUMN cms_tag.version IS '版本号（乐观锁预留）';
COMMENT ON COLUMN cms_tag.del_flag IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN cms_tag.create_time IS '创建时间';
COMMENT ON COLUMN cms_tag.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN cms_tag.create_by IS '创建人';
COMMENT ON COLUMN cms_tag.last_modify_by IS '最后修改人';
COMMENT ON COLUMN cms_tag.remark IS '备注';

CREATE INDEX IF NOT EXISTS idx_cms_tag_tag_name ON cms_tag(tag_name);


CREATE TABLE IF NOT EXISTS cms_article_file (
    id VARCHAR(64) PRIMARY KEY,
    article_id VARCHAR(64) NOT NULL,
    file_id VARCHAR(64) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    CONSTRAINT fk_cms_article_file_article FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_cms_article_file_file FOREIGN KEY (file_id) REFERENCES sys_file(id) ON DELETE CASCADE
);

COMMENT ON TABLE cms_article_file IS 'CMS 文章附件关联表';
COMMENT ON COLUMN cms_article_file.id IS '主键ID';
COMMENT ON COLUMN cms_article_file.article_id IS '文章ID，关联cms_article.id';
COMMENT ON COLUMN cms_article_file.file_id IS '文件ID，关联sys_file.id';
COMMENT ON COLUMN cms_article_file.sort_order IS '附件排序号';
COMMENT ON COLUMN cms_article_file.version IS '版本号（乐观锁预留）';
COMMENT ON COLUMN cms_article_file.del_flag IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN cms_article_file.create_time IS '创建时间';
COMMENT ON COLUMN cms_article_file.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN cms_article_file.create_by IS '创建人';
COMMENT ON COLUMN cms_article_file.last_modify_by IS '最后修改人';
COMMENT ON COLUMN cms_article_file.remark IS '备注';

CREATE INDEX IF NOT EXISTS idx_cms_article_file_article_id ON cms_article_file(article_id);
CREATE INDEX IF NOT EXISTS idx_cms_article_file_file_id ON cms_article_file(file_id);


CREATE TABLE IF NOT EXISTS cms_article_tag (
    id VARCHAR(64) PRIMARY KEY,
    article_id VARCHAR(64) NOT NULL,
    tag_id VARCHAR(64) NOT NULL,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    CONSTRAINT fk_cms_article_tag_article FOREIGN KEY (article_id) REFERENCES cms_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_cms_article_tag_tag FOREIGN KEY (tag_id) REFERENCES cms_tag(id) ON DELETE CASCADE,
    CONSTRAINT uk_cms_article_tag_article_tag UNIQUE (article_id, tag_id)
);

COMMENT ON TABLE cms_article_tag IS 'CMS 文章标签关联表';
COMMENT ON COLUMN cms_article_tag.id IS '主键ID';
COMMENT ON COLUMN cms_article_tag.article_id IS '文章ID，关联cms_article.id';
COMMENT ON COLUMN cms_article_tag.tag_id IS '标签ID，关联cms_tag.id';
COMMENT ON COLUMN cms_article_tag.version IS '版本号（乐观锁预留）';
COMMENT ON COLUMN cms_article_tag.del_flag IS '逻辑删除标记：0-未删除，1-已删除';
COMMENT ON COLUMN cms_article_tag.create_time IS '创建时间';
COMMENT ON COLUMN cms_article_tag.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN cms_article_tag.create_by IS '创建人';
COMMENT ON COLUMN cms_article_tag.last_modify_by IS '最后修改人';
COMMENT ON COLUMN cms_article_tag.remark IS '备注';

CREATE INDEX IF NOT EXISTS idx_cms_article_tag_article_id ON cms_article_tag(article_id);
CREATE INDEX IF NOT EXISTS idx_cms_article_tag_tag_id ON cms_article_tag(tag_id);

