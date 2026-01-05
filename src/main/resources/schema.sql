-- SysUser Table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    avatar VARCHAR(500),
    email VARCHAR(255),
    phone VARCHAR(50),
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by VARCHAR(255),
    last_modify_by VARCHAR(255),
    remark VARCHAR(500)
);

-- SysRole Table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    role_value VARCHAR(100) NOT NULL UNIQUE,
    status INTEGER DEFAULT 1,
    description VARCHAR(500),
    order_no INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by VARCHAR(255),
    last_modify_by VARCHAR(255),
    remark VARCHAR(500)
);

-- SysMenu Table
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(255),
    name VARCHAR(100),
    component VARCHAR(255),
    redirect VARCHAR(255),
    title VARCHAR(100),
    icon VARCHAR(100),
    order_no INTEGER DEFAULT 0,
    hide_menu BOOLEAN DEFAULT FALSE,
    keep_alive BOOLEAN DEFAULT TRUE,
    active_icon VARCHAR(100),
    badge VARCHAR(100),
    badge_type VARCHAR(50),
    badge_variants VARCHAR(50),
    link VARCHAR(255),
    iframe_src VARCHAR(255),
    affix_tab BOOLEAN DEFAULT FALSE,
    hide_children_in_menu BOOLEAN DEFAULT FALSE,
    hide_breadcrumb BOOLEAN DEFAULT FALSE,
    hide_tab BOOLEAN DEFAULT FALSE,
    menu_visible_with_forbidden BOOLEAN DEFAULT FALSE,
    authority VARCHAR(255),
    permission VARCHAR(100),
    type INTEGER,
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by VARCHAR(255),
    last_modify_by VARCHAR(255),
    remark VARCHAR(500)
);

-- SysUserRole Table
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by VARCHAR(255),
    last_modify_by VARCHAR(255),
    remark VARCHAR(500)
);

-- SysRoleMenu Table
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by VARCHAR(255),
    last_modify_by VARCHAR(255),
    remark VARCHAR(500)
);
