-- SysUser Table
CREATE TABLE IF NOT EXISTS sys_user
(
    id               VARCHAR(32)  NOT NULL UNIQUE,
    username         VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    real_name        VARCHAR(100),
    avatar           VARCHAR(500),
    email            VARCHAR(255),
    phone            VARCHAR(50),
    dept_id          VARCHAR(32),
    status           INTEGER   DEFAULT 1,
    version          BIGINT    DEFAULT 0,
    del_flag         INTEGER   DEFAULT 0,
    create_time      TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by        VARCHAR(255),
    last_modify_by   VARCHAR(255),
    remark           VARCHAR(500)
);

-- SysRole Table
CREATE TABLE IF NOT EXISTS sys_role
(
    id               VARCHAR(32)  NOT NULL UNIQUE,
    role_name        VARCHAR(100) NOT NULL,
    role_value       VARCHAR(100) NOT NULL UNIQUE,
    status           INTEGER   DEFAULT 1,
    description      VARCHAR(500),
    order_no         INTEGER   DEFAULT 0,
    version          BIGINT    DEFAULT 0,
    del_flag         INTEGER   DEFAULT 0,
    create_time      TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by        VARCHAR(255),
    last_modify_by   VARCHAR(255),
    remark           VARCHAR(500)
);

-- SysMenu Table
CREATE TABLE IF NOT EXISTS sys_menu
(
    id                          VARCHAR(32) NOT NULL UNIQUE,
    parent_id                   VARCHAR(32)  DEFAULT NULL,
    path                        VARCHAR(255),
    name                        VARCHAR(100),
    component                   VARCHAR(255),
    redirect                    VARCHAR(255),
    title                       VARCHAR(100),
    icon                        VARCHAR(100),
    order_no                    INTEGER              DEFAULT 0,
    hide_menu                   BOOLEAN              DEFAULT FALSE,
    keep_alive                  BOOLEAN              DEFAULT TRUE,
    active_icon                 VARCHAR(100),
    badge                       VARCHAR(100),
    badge_type                  VARCHAR(50),
    badge_variants              VARCHAR(50),
    link                        VARCHAR(255),
    iframe_src                  VARCHAR(255),
    affix_tab                   BOOLEAN              DEFAULT FALSE,
    hide_children_in_menu       BOOLEAN              DEFAULT FALSE,
    hide_breadcrumb             BOOLEAN              DEFAULT FALSE,
    hide_tab                    BOOLEAN              DEFAULT FALSE,
    menu_visible_with_forbidden BOOLEAN              DEFAULT FALSE,
    authority                   VARCHAR(255),
    permission                  VARCHAR(100),
    type                        INTEGER,
    status                      INTEGER              DEFAULT 1,
    version                     BIGINT               DEFAULT 0,
    del_flag                    INTEGER              DEFAULT 0,
    create_time                 TIMESTAMP            DEFAULT NOW(),
    last_modify_time            TIMESTAMP            DEFAULT NOW(),
    create_by                   VARCHAR(255),
    last_modify_by              VARCHAR(255),
    remark                      VARCHAR(500)
);

-- SysDept Table
CREATE TABLE IF NOT EXISTS sys_dept
(
    id               VARCHAR(32)  NOT NULL UNIQUE,
    parent_id        VARCHAR(32)  DEFAULT NULL,
    dept_name        VARCHAR(100) NOT NULL,
    order_no         INTEGER               DEFAULT 0,
    status           INTEGER               DEFAULT 1,
    version          BIGINT                DEFAULT 0,
    del_flag         INTEGER               DEFAULT 0,
    create_time      TIMESTAMP             DEFAULT NOW(),
    last_modify_time TIMESTAMP             DEFAULT NOW(),
    create_by        VARCHAR(255),
    last_modify_by   VARCHAR(255),
    remark           VARCHAR(500)
);

-- SysUserRole Table
CREATE TABLE IF NOT EXISTS sys_user_role
(
    id               VARCHAR(32) NOT NULL UNIQUE,
    user_id          VARCHAR(32) NOT NULL,
    role_id          VARCHAR(32) NOT NULL,
    version          BIGINT    DEFAULT 0,
    del_flag         INTEGER   DEFAULT 0,
    create_time      TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by        VARCHAR(255),
    last_modify_by   VARCHAR(255),
    remark           VARCHAR(500)
);

-- SysRoleMenu Table
CREATE TABLE IF NOT EXISTS sys_role_menu
(
    id               VARCHAR(32) NOT NULL UNIQUE,
    role_id          VARCHAR(32) NOT NULL,
    menu_id          VARCHAR(32) NOT NULL,
    version          BIGINT    DEFAULT 0,
    del_flag         INTEGER   DEFAULT 0,
    create_time      TIMESTAMP DEFAULT NOW(),
    last_modify_time TIMESTAMP DEFAULT NOW(),
    create_by        VARCHAR(255),
    last_modify_by   VARCHAR(255),
    remark           VARCHAR(500)
);

DROP TABLE IF EXISTS "public"."sys_dict_item";
CREATE TABLE "public"."sys_dict_item"
(
    "id"               varchar(32) COLLATE "pg_catalog"."default"  NOT NULL UNIQUE,
    "type_code"        varchar(64) COLLATE "pg_catalog"."default"  NOT NULL,
    "item_code"        varchar(64) COLLATE "pg_catalog"."default"  NOT NULL,
    "item_text"        varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "item_value"       varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "status"           int2                                        NOT NULL DEFAULT 1,
    "order_no"         int4                                        NOT NULL DEFAULT 0,
    "parent_code"      varchar(64) COLLATE "pg_catalog"."default",
    "version"          int8                                                 DEFAULT 0,
    "del_flag"         int4                                                 DEFAULT 0,
    "create_time"      timestamp(6)                                         DEFAULT now(),
    "last_modify_time" timestamp(6)                                         DEFAULT now(),
    "create_by"        varchar(255) COLLATE "pg_catalog"."default",
    "last_modify_by"   varchar(255) COLLATE "pg_catalog"."default",
    "remark"           varchar(500) COLLATE "pg_catalog"."default"
);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_type";
CREATE TABLE "public"."sys_dict_type"
(
    "id"               varchar(32) COLLATE "pg_catalog"."default"  NOT NULL UNIQUE,
    "code"             varchar(64) COLLATE "pg_catalog"."default"  NOT NULL,
    "name"             varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
    "status"           int2                                        NOT NULL DEFAULT 1,
    "order_no"         int4                                        NOT NULL DEFAULT 0,
    "version"          int8                                                 DEFAULT 0,
    "del_flag"         int4                                                 DEFAULT 0,
    "create_time"      timestamp(6)                                         DEFAULT now(),
    "last_modify_time" timestamp(6)                                         DEFAULT now(),
    "create_by"        varchar(255) COLLATE "pg_catalog"."default",
    "last_modify_by"   varchar(255) COLLATE "pg_catalog"."default",
    "remark"           varchar(500) COLLATE "pg_catalog"."default"
);