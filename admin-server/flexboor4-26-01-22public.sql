/*
 Navicat Premium Dump SQL

 Source Server         : 192.168.11.104
 Source Server Type    : PostgreSQL
 Source Server Version : 150015 (150015)
 Source Host           : 192.168.11.104:5433
 Source Catalog        : flexboot4
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 150015 (150015)
 File Encoding         : 65001

 Date: 22/01/2026 13:25:35
*/


-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dept";
CREATE TABLE "public"."sys_dept" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_name" varchar(50) COLLATE "pg_catalog"."default",
  "order_no" int2 DEFAULT 0,
  "status" int4 DEFAULT 1,
  "parent_id" varchar(32) COLLATE "pg_catalog"."default",
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(32) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(32) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."sys_dept"."dept_name" IS '部门名称';
COMMENT ON COLUMN "public"."sys_dept"."order_no" IS '排序';
COMMENT ON COLUMN "public"."sys_dept"."status" IS '状态 1-启用 0-禁用';
COMMENT ON COLUMN "public"."sys_dept"."parent_id" IS '父级ID';
COMMENT ON TABLE "public"."sys_dept" IS '部门表';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO "public"."sys_dept" VALUES ('2', 'R&D Department', 2, 1, '1', 0, 0, '2026-01-07 15:00:32.751024', '2026-01-07 15:00:32.751024', NULL, NULL, '这是二级节点');
INSERT INTO "public"."sys_dept" VALUES ('4', 'Backend Group', 4, 1, '3', 0, 0, '2026-01-07 15:00:32.752715', '2026-01-07 15:00:32.752715', NULL, NULL, '这是四级节点');
INSERT INTO "public"."sys_dept" VALUES ('3', 'Development Team 1', 3, 1, '2', 0, 0, '2026-01-07 15:00:32.75191', '2026-01-07 15:00:32.75191', NULL, NULL, '这是三级节点');
INSERT INTO "public"."sys_dept" VALUES ('5', 'Node1', 2, 1, NULL, 0, 0, '2026-01-08 02:23:10.477523', '2026-01-08 02:23:10.477523', NULL, NULL, '这也是一级节点');
INSERT INTO "public"."sys_dept" VALUES ('1', 'Headquarters', 1, 1, NULL, 0, 0, '2026-01-07 15:00:32.749732', '2026-01-07 15:00:32.749732', NULL, NULL, '这是一级节点');
INSERT INTO "public"."sys_dept" VALUES ('371095977450438656', 'Node-2', 3, 0, '5', 0, 0, '2026-01-20 13:43:55.572229', '2026-01-20 13:44:28.665808', 'super', 'super', '这是node-1的二级节点');

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_item";
CREATE TABLE "public"."sys_dict_item" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "type_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "item_code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "item_text" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "item_value" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL DEFAULT 1,
  "order_no" int4 NOT NULL DEFAULT 0,
  "parent_code" varchar(64) COLLATE "pg_catalog"."default",
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(255) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(255) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."sys_dict_item"."type_id" IS 'pk（sys_dict_type）';

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO "public"."sys_dict_item" VALUES ('371406372480450560', '371404416743604224', 'bt_logout', '登出', '9', 1, 9, NULL, 0, 0, '2026-01-21 10:17:19.38866', '2026-01-21 10:17:19.38866', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371405897211281408', '371404416743604224', 'op_delete', '删除', '3', 1, 3, NULL, 0, 0, '2026-01-21 10:15:26.068986', '2026-01-21 10:15:26.068986', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371406200660787200', '371404416743604224', 'bt_query', '查询', '7', 1, 7, NULL, 0, 0, '2026-01-21 10:16:38.420602', '2026-01-21 10:16:38.420602', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371404581596528640', '371404416743604224', 'bt_other', '其它', '0', 1, 0, NULL, 0, 0, '2026-01-21 10:10:12.38522', '2026-01-21 10:10:12.38522', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371406111678627840', '371404416743604224', 'bt_import', '导入', '6', 1, 6, NULL, 0, 0, '2026-01-21 10:16:17.205046', '2026-01-21 10:16:17.205046', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371406050848636928', '371404416743604224', 'bt_export', '导出', '5', 1, 5, NULL, 0, 0, '2026-01-21 10:16:02.701046', '2026-01-21 10:16:02.701046', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371405835106222080', '371404416743604224', 'bt_update', '修改', '2', 1, 2, NULL, 0, 0, '2026-01-21 10:15:11.261615', '2026-01-21 10:15:11.261615', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371405611046502400', '371404416743604224', 'bt_insert', '新增', '1', 1, 1, NULL, 0, 0, '2026-01-21 10:14:17.838499', '2026-01-21 10:14:17.838499', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371406295074570240', '371404416743604224', 'bt_login', '登录', '8', 1, 8, NULL, 0, 0, '2026-01-21 10:17:00.932154', '2026-01-21 10:17:00.932154', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371405962134913024', '371404416743604224', 'bt_api', '调用api', '4', 1, 4, NULL, 0, 0, '2026-01-21 10:15:41.548792', '2026-01-21 10:15:41.548792', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371413266137841664', '371413160227471360', 'op_other', '其它', '0', 1, 0, NULL, 0, 0, '2026-01-21 10:44:43.034962', '2026-01-21 10:44:43.034962', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371413403266416640', '371413160227471360', 'op_manage', '后台用户', '1', 1, 1, NULL, 0, 0, '2026-01-21 10:45:15.730276', '2026-01-21 10:45:15.730276', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371413475207118848', '371413160227471360', 'op_mobile', '移动端用户', '2', 1, 2, NULL, 0, 0, '2026-01-21 10:45:32.882753', '2026-01-21 10:45:32.882753', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371414133826093056', '371413834390536192', 'disabled', '禁用', '0', 1, 0, NULL, 0, 0, '2026-01-21 10:48:09.913459', '2026-01-21 10:48:09.913459', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371414303238225920', '371413834390536192', 'enable', '启用', '1', 1, 1, NULL, 0, 0, '2026-01-21 10:48:50.305502', '2026-01-21 10:48:50.305502', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371414364802220032', '371413834390536192', 'locked', '锁定', '2', 1, 2, NULL, 0, 0, '2026-01-21 10:49:04.983651', '2026-01-21 10:49:04.983651', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371414843372306432', '371414504548040704', 'male', '男', 'male', 1, 0, NULL, 0, 0, '2026-01-21 10:50:59.087185', '2026-01-21 10:50:59.087185', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371414896426057728', '371414504548040704', 'female', '女', 'female', 1, 1, NULL, 0, 0, '2026-01-21 10:51:11.7372', '2026-01-21 10:51:11.7372', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('371414992551116800', '371414504548040704', 'pther', '其他', 'other', 1, 2, NULL, 0, 0, '2026-01-21 10:51:34.655321', '2026-01-21 10:51:34.655321', '1', NULL, NULL);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_dict_type";
CREATE TABLE "public"."sys_dict_type" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL DEFAULT 1,
  "order_no" int4 NOT NULL DEFAULT 0,
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(255) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(255) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO "public"."sys_dict_type" VALUES ('371404416743604224', 'business_type', '业务类型', 1, 1, 0, 0, '2026-01-21 10:09:33.080565', '2026-01-21 10:12:50.861643', '1', '1', NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('371413160227471360', 'operator_type', '操作类别', 1, 2, 0, 0, '2026-01-21 10:44:17.783047', '2026-01-21 10:44:17.783047', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('371413834390536192', 'status', '状态值', 1, 3, 0, 0, '2026-01-21 10:46:58.520272', '2026-01-21 10:46:58.520272', '1', NULL, NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('371414504548040704', 'gender', '性别', 1, 4, 0, 0, '2026-01-21 10:49:38.302481', '2026-01-21 10:49:38.302481', '1', NULL, NULL);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_menu";
CREATE TABLE "public"."sys_menu" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" varchar(64) COLLATE "pg_catalog"."default",
  "path" varchar(255) COLLATE "pg_catalog"."default",
  "name" varchar(100) COLLATE "pg_catalog"."default",
  "component" varchar(255) COLLATE "pg_catalog"."default",
  "redirect" varchar(255) COLLATE "pg_catalog"."default",
  "title" varchar(100) COLLATE "pg_catalog"."default",
  "icon" varchar(100) COLLATE "pg_catalog"."default",
  "order_no" int4 DEFAULT 0,
  "hide_menu" bool DEFAULT false,
  "keep_alive" bool DEFAULT true,
  "active_icon" varchar(100) COLLATE "pg_catalog"."default",
  "badge" varchar(100) COLLATE "pg_catalog"."default",
  "badge_type" varchar(50) COLLATE "pg_catalog"."default",
  "badge_variants" varchar(50) COLLATE "pg_catalog"."default",
  "link" varchar(255) COLLATE "pg_catalog"."default",
  "iframe_src" varchar(255) COLLATE "pg_catalog"."default",
  "affix_tab" bool DEFAULT false,
  "hide_children_in_menu" bool DEFAULT false,
  "hide_breadcrumb" bool DEFAULT false,
  "hide_tab" bool DEFAULT false,
  "menu_visible_with_forbidden" bool DEFAULT false,
  "authority" varchar(255) COLLATE "pg_catalog"."default",
  "auth_code" varchar(100) COLLATE "pg_catalog"."default",
  "type" varchar(10) COLLATE "pg_catalog"."default",
  "status" int4 DEFAULT 1,
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(32) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(32) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."sys_menu"."parent_id" IS '父级id';
COMMENT ON COLUMN "public"."sys_menu"."auth_code" IS '权限标识码';
COMMENT ON COLUMN "public"."sys_menu"."type" IS '0-catalog 1-menu 2-button 3-embedded 4-link';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO "public"."sys_menu" VALUES ('132', '130', '', 'SysDictTypeDelete', NULL, NULL, 'common.delete', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dict:type:delete', 'button', 1, 0, 0, '2026-01-15 16:51:54.707078', '2026-01-15 16:51:54.707078', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('151', '150', '', 'SystemRoleCreate', NULL, NULL, 'common.create', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:role:create', 'button', 1, 0, 0, '2026-01-15 16:28:39.278494', '2026-01-15 16:28:39.278494', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('152', '150', '', 'SystemRoleUpdate', NULL, NULL, 'common.edit', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:role:edit', 'button', 1, 0, 0, '2026-01-15 16:30:25.712212', '2026-01-15 16:30:49.364411', 'super', 'super', NULL);
INSERT INTO "public"."sys_menu" VALUES ('131', '130', '', 'SysDictTypeUpdate', NULL, NULL, 'common.edit', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dict:type:edit', 'button', 1, 0, 0, '2026-01-15 16:51:15.271361', '2026-01-15 16:51:15.271361', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('112', '110', NULL, 'SystemMenuEdit', NULL, NULL, 'common.edit', NULL, 0, 'f', 'f', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:menu:edit', 'button', 1, 0, 0, '2026-01-05 02:05:15.746674', '2026-01-05 02:05:15.746674', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('113', '110', NULL, 'SystemMenuDelete', NULL, NULL, 'common.delete', NULL, 0, 'f', 'f', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:menu:delete', 'button', 1, 0, 0, '2026-01-05 02:05:15.759969', '2026-01-05 02:05:15.759969', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('111', '110', NULL, 'SystemMenuCreate', NULL, NULL, 'common.create', NULL, 0, 'f', 'f', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:menu:create', 'button', 1, 0, 0, '2026-01-05 02:05:15.73282', '2026-01-05 02:05:15.73282', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('109', NULL, '/system', 'System', NULL, NULL, 'system.title', 'carbon:settings', 9997, 'f', 't', NULL, 'new', 'normal', 'primary', NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'catalog', 1, 0, 0, '2026-01-05 02:05:15.704869', '2026-01-05 02:05:15.704869', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('108', NULL, '/workspace', 'Workspace', '/dashboard/workspace/index', NULL, 'page.dashboard.workspace', 'carbon:workspace', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 't', 'f', 'f', 'f', 'f', NULL, NULL, 'menu', 1, 0, 0, '2026-01-05 02:05:15.691966', '2026-01-05 02:05:15.691966', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('117', '120', NULL, 'SystemDeptDelete', NULL, NULL, 'common.delete', NULL, 0, 'f', 'f', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dept:delete', 'button', 1, 0, 0, '2026-01-05 02:05:15.821446', '2026-01-05 02:05:15.821446', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('116', '120', NULL, 'SystemDeptEdit', NULL, NULL, 'common.edit', NULL, 0, 'f', 'f', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dept:edit', 'button', 1, 0, 0, '2026-01-05 02:05:15.801535', '2026-01-05 02:05:15.801535', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('115', '120', NULL, 'SystemDeptCreate', NULL, NULL, 'common.create', NULL, 0, 'f', 'f', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dept:create', 'button', 1, 0, 0, '2026-01-05 02:05:15.787384', '2026-01-05 02:05:15.787384', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('110', '109', '/system/menu', 'SystemMenu', '/system/menu/list', NULL, 'system.menu.title', 'carbon:menu', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'menu', 1, 0, 0, '2026-01-05 02:05:15.716669', '2026-01-05 02:05:15.716669', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('120', '109', '/system/dept', 'SystemDept', '/system/dept/list', NULL, 'system.dept.title', 'carbon:container-services', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'menu', 1, 0, 0, '2026-01-05 02:05:15.772823', '2026-01-05 02:05:15.772823', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('130', '109', '/system/dict', 'Dict', '/system/dict/index', NULL, 'system.dict.title', 'carbon:language', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, '', 'menu', 1, 0, 0, '2026-01-09 13:55:47.962167', '2026-01-15 16:49:02.790196', NULL, 'super', NULL);
INSERT INTO "public"."sys_menu" VALUES ('150', '109', '/system/role', 'SystemRole', '/system/role/index', NULL, 'system.role.title', 'carbon:user-role', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'menu', 1, 0, 0, '2026-01-15 16:26:35.398432', '2026-01-15 16:26:35.398432', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('114', '110', NULL, 'SystemMenuList', NULL, NULL, 'common.list', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:menu:list', 'button', 1, 0, 0, '2026-01-16 06:47:45.570776', '2026-01-16 06:47:45.570776', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('118', '120', NULL, 'SystemDeptList', NULL, NULL, 'common.list', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dept:list', 'button', 1, 0, 0, '2026-01-16 06:51:37.993827', '2026-01-16 06:51:37.993827', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('134', '130', NULL, 'SysDictTypeList', NULL, NULL, 'common.list', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dict:type:list', 'button', 1, 0, 0, '2026-01-16 06:52:43.598085', '2026-01-16 06:52:43.598085', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('140', '109', '/system/user', 'SystemUser', '/system/user/list', NULL, 'system.user.title', 'carbon:user-multiple', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'menu', 1, 0, 0, '2026-01-13 11:57:48.771226', '2026-01-13 12:31:35.560899', 'super', 'super', NULL);
INSERT INTO "public"."sys_menu" VALUES ('154', '150', NULL, 'SystemRoleList', NULL, NULL, 'common.list', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:role:list', 'button', 1, 0, 0, '2026-01-16 06:54:32.288592', '2026-01-16 06:54:32.288592', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('141', '140', '', 'SystemUserDelete', NULL, NULL, 'common.delete', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:user:delete', 'button', 1, 0, 0, '2026-01-13 12:32:52.881214', '2026-01-13 12:32:52.881214', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('142', '140', '', 'SystemUserEdit', NULL, NULL, 'common.edit', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:user:edit', 'button', 1, 0, 0, '2026-01-13 13:03:43.0803', '2026-01-13 13:03:43.0803', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('143', '140', '', 'SystemUserCreate', NULL, NULL, 'common.create', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:user:create', 'button', 1, 0, 0, '2026-01-13 13:04:56.371324', '2026-01-13 13:04:56.371324', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('144', '140', NULL, 'SystemUserList', NULL, NULL, 'common.list', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:user:list', 'button', 1, 0, 0, '2026-01-16 06:53:45.048583', '2026-01-16 06:53:45.048583', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('133', '130', '', 'SysDictTypeCreate', NULL, NULL, 'common.create', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:dict:type:create', 'button', 1, 0, 0, '2026-01-15 16:50:20.630701', '2026-01-15 16:50:20.630701', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('153', '150', '', 'SystemRoleDelete', NULL, NULL, 'common.delete', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:role:delete', 'button', 1, 0, 0, '2026-01-15 16:32:48.756086', '2026-01-15 16:32:48.756086', 'super', NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('145', '140', NULL, 'SystemUserExport', NULL, NULL, 'common.export', NULL, 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, 'sys:user:export', 'button', 1, 0, 0, '2026-01-16 06:53:45.048583', '2026-01-16 06:53:45.048583', NULL, NULL, NULL);
INSERT INTO "public"."sys_menu" VALUES ('371465495163052032', NULL, '/devops', 'DevOps', NULL, NULL, 'devops.title', 'carbon:ibm-webmethods-developer-portal', 0, 'f', 't', NULL, NULL, 'normal', 'primary', NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'catalog', 1, 0, 0, '2026-01-21 14:12:15.487117', '2026-01-21 16:21:54.880305', '1', '1', NULL);
INSERT INTO "public"."sys_menu" VALUES ('371467353638182912', '371465495163052032', '/devops/log', 'DevOpsLog', '/devops/log/index', NULL, 'devops.log.title', 'carbon:ibm-knowledge-catalog', 0, 'f', 't', NULL, NULL, NULL, NULL, NULL, NULL, 'f', 'f', 'f', 'f', 'f', NULL, NULL, 'menu', 1, 0, 0, '2026-01-21 14:19:38.583221', '2026-01-21 16:22:43.349019', '1', '1', NULL);

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_oper_log";
CREATE TABLE "public"."sys_oper_log" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "business_type" int4 DEFAULT 0,
  "method" varchar(100) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "request_method" varchar(10) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "operator_type" int4 DEFAULT 0,
  "oper_name" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_user_id" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "dept_id" varchar(32) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_url" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_ip" varchar(128) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_location" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_param" jsonb,
  "json_result" jsonb,
  "status" int4 DEFAULT 0,
  "error_msg" text COLLATE "pg_catalog"."default",
  "oper_time" timestamp(6),
  "cost_time" int8 DEFAULT 0,
  "ext_params" jsonb,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "terminal" jsonb
)
;
COMMENT ON COLUMN "public"."sys_oper_log"."title" IS '模块标题';
COMMENT ON COLUMN "public"."sys_oper_log"."business_type" IS '业务类型';
COMMENT ON COLUMN "public"."sys_oper_log"."method" IS '方法名称';
COMMENT ON COLUMN "public"."sys_oper_log"."request_method" IS '请求方式';
COMMENT ON COLUMN "public"."sys_oper_log"."operator_type" IS '操作类别';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_name" IS '操作人员';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_user_id" IS '操作人员id';
COMMENT ON COLUMN "public"."sys_oper_log"."dept_id" IS '部门id pk(sys_dept.id)';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_url" IS '请求URL';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_ip" IS '主机地址';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_location" IS '操作地点';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_param" IS '请求参数';
COMMENT ON COLUMN "public"."sys_oper_log"."json_result" IS '返回参数';
COMMENT ON COLUMN "public"."sys_oper_log"."status" IS '操作状态（0正常 1异常）';
COMMENT ON COLUMN "public"."sys_oper_log"."error_msg" IS '错误消息';
COMMENT ON COLUMN "public"."sys_oper_log"."oper_time" IS '操作时间';
COMMENT ON COLUMN "public"."sys_oper_log"."cost_time" IS '消耗时间';
COMMENT ON TABLE "public"."sys_oper_log" IS '操作日志记录';

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_oper_log_2026_q1
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_oper_log_2026_q1";
CREATE TABLE "public"."sys_oper_log_2026_q1" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "title" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "business_type" int4 DEFAULT 0,
  "method" varchar(100) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "request_method" varchar(10) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "operator_type" int4 DEFAULT 0,
  "oper_name" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_user_id" varchar(50) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "dept_id" varchar(32) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_url" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_ip" varchar(128) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_location" varchar(255) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "oper_param" jsonb,
  "json_result" jsonb,
  "status" int4 DEFAULT 0,
  "error_msg" text COLLATE "pg_catalog"."default",
  "oper_time" timestamp(6),
  "cost_time" int8 DEFAULT 0,
  "ext_params" jsonb,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "terminal" jsonb
)
;
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."title" IS '模块标题';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."business_type" IS '业务类型';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."method" IS '方法名称';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."request_method" IS '请求方式';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."operator_type" IS '操作类别';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_name" IS '操作人员';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_user_id" IS '操作人员id';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."dept_id" IS '部门id';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_url" IS '请求URL';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_ip" IS '主机地址';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_location" IS '操作地点';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_param" IS '请求参数';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."json_result" IS '返回参数';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."status" IS '操作状态（0正常 1异常）';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."error_msg" IS '错误消息';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."oper_time" IS '操作时间';
COMMENT ON COLUMN "public"."sys_oper_log_2026_q1"."cost_time" IS '消耗时间';

-- ----------------------------
-- Records of sys_oper_log_2026_q1
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role";
CREATE TABLE "public"."sys_role" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "role_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "role_value" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int4 DEFAULT 1,
  "description" varchar(500) COLLATE "pg_catalog"."default",
  "order_no" int4 DEFAULT 0,
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(255) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(255) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "data_scope" varchar(20) COLLATE "pg_catalog"."default" DEFAULT '1'::character varying
)
;
COMMENT ON COLUMN "public"."sys_role"."data_scope" IS '数据范围（1:全部数据 2:本部门及以下 3:本部门 4:仅本人 5:自定义）';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO "public"."sys_role" VALUES ('1', 'Super Admin Role', 'super', 1, NULL, 0, 0, 0, '2025-12-31 15:24:33.124457', '2025-12-31 15:24:33.124457', NULL, NULL, NULL, '1');
INSERT INTO "public"."sys_role" VALUES ('2', 'admin', 'admin', 1, NULL, 1, 0, 0, '2025-12-31 08:38:14.742144', '2025-12-31 08:38:14.742144', NULL, NULL, NULL, '1');
INSERT INTO "public"."sys_role" VALUES ('4', 'test', 'test', 1, NULL, 3, 0, 0, '2025-12-31 08:39:06.784532', '2025-12-31 08:39:06.784532', NULL, NULL, NULL, '1');
INSERT INTO "public"."sys_role" VALUES ('3', 'user', 'user', 1, NULL, 2, 0, 0, '2025-12-31 08:39:06.784532', '2026-01-19 11:25:44.920648', NULL, 'super', 'user aaaa', '1');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_dept";
CREATE TABLE "public"."sys_role_dept" (
  "role_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "dept_id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON TABLE "public"."sys_role_dept" IS '角色与部门关联表（自定义数据权限）';

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_menu";
CREATE TABLE "public"."sys_role_menu" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "role_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "menu_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(32) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(32) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO "public"."sys_role_menu" VALUES ('9', '1', '108', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('10', '1', '109', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('11', '1', '110', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('12', '1', '111', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('13', '1', '112', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('14', '1', '113', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('15', '1', '114', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('16', '1', '115', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('17', '1', '116', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('18', '1', '117', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('19', '1', '118', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('21', '1', '120', 0, 0, '2026-01-05 02:07:52.389923', '2026-01-05 02:07:52.389923', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('25', '1', '140', 0, 0, '2026-01-13 05:08:36.787485', '2026-01-13 05:08:36.787485', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('26', '1', '141', 0, 0, '2026-01-13 05:08:36.787485', '2026-01-13 05:08:36.787485', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('27', '1', '142', 0, 0, '2026-01-13 05:08:36.787485', '2026-01-13 05:08:36.787485', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('28', '1', '143', 0, 0, '2026-01-13 05:08:36.787485', '2026-01-13 05:08:36.787485', NULL, NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759104', '4', '111', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759105', '4', '145', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759106', '4', '114', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759107', '4', '112', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759108', '4', '113', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759109', '4', '110', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383507759110', '4', '144', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383511953408', '4', '143', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);
INSERT INTO "public"."sys_role_menu" VALUES ('370684383511953409', '4', '142', 0, 0, '2026-01-19 10:28:24.014904', '2026-01-19 10:28:24.014904', 'super', NULL, NULL);

-- ----------------------------
-- Table structure for sys_test
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_test";
CREATE TABLE "public"."sys_test" (
  "id" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar COLLATE "pg_catalog"."default",
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(255) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(255) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."sys_test"."id" IS 'id';
COMMENT ON COLUMN "public"."sys_test"."name" IS 'sss';

-- ----------------------------
-- Records of sys_test
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user";
CREATE TABLE "public"."sys_user" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "username" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "real_name" varchar(100) COLLATE "pg_catalog"."default",
  "avatar" varchar(500) COLLATE "pg_catalog"."default",
  "email" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "phone" varchar(50) COLLATE "pg_catalog"."default",
  "status" int4 DEFAULT 1,
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(32) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(32) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default",
  "dept_id" varchar(32) COLLATE "pg_catalog"."default",
  "gender" varchar(64) COLLATE "pg_catalog"."default" DEFAULT 'male'::character varying
)
;
COMMENT ON COLUMN "public"."sys_user"."gender" IS '性别 dict_type_code=gender';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO "public"."sys_user" VALUES ('1', 'super', '$2a$10$nPkaH.TwwhXAxViweSe/n./g2On9lDrXBvRXIfjvoJKkpfNLoNFLO', 'Super Admin', NULL, 'email@qq.com', '13800000000', 1, 0, 0, '2025-12-31 15:09:05.125076', '2025-12-31 15:09:05.125076', 'yoyo', 'yoyo', 'check it out', '3', 'female');
INSERT INTO "public"."sys_user" VALUES ('4', 'test', '$2a$10$5JeoSGjEo8rqqwDwvSB9..ZjnO0.VA1en2d3ON3yPsTAhOM30ITh2', 'test', NULL, '1803391503@qq.com', '15156000000', 1, 0, 0, '2026-01-15 10:03:31.512896', '2026-01-16 10:35:44.719567', 'super', 'super', NULL, '1', 'male');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_role";
CREATE TABLE "public"."sys_user_role" (
  "id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "role_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "version" int8 DEFAULT 0,
  "del_flag" int4 DEFAULT 0,
  "create_time" timestamp(6) DEFAULT now(),
  "last_modify_time" timestamp(6) DEFAULT now(),
  "create_by" varchar(32) COLLATE "pg_catalog"."default",
  "last_modify_by" varchar(32) COLLATE "pg_catalog"."default",
  "remark" varchar(500) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO "public"."sys_user_role" VALUES ('1', '1', '1', 0, 0, '2025-12-31 15:24:33.152207', '2025-12-31 15:24:33.152207', NULL, NULL, NULL);
INSERT INTO "public"."sys_user_role" VALUES ('2', '4', '4', 0, 0, '2026-01-14 15:52:14.63487', '2026-01-14 15:52:14.63487', 'super', NULL, NULL);

-- ----------------------------
-- Primary Key structure for table sys_dept
-- ----------------------------
ALTER TABLE "public"."sys_dept" ADD CONSTRAINT "sys_dept_pk" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_dict_item
-- ----------------------------
CREATE INDEX "sys_dict_item_type_id_index" ON "public"."sys_dict_item" USING btree (
  "type_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table sys_dict_item
-- ----------------------------
ALTER TABLE "public"."sys_dict_item" ADD CONSTRAINT "sys_dict_item_id_key" UNIQUE ("id");

-- ----------------------------
-- Uniques structure for table sys_dict_type
-- ----------------------------
ALTER TABLE "public"."sys_dict_type" ADD CONSTRAINT "sys_dict_type_id_key" UNIQUE ("id");

-- ----------------------------
-- Primary Key structure for table sys_menu
-- ----------------------------
ALTER TABLE "public"."sys_menu" ADD CONSTRAINT "sys_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_oper_log
-- ----------------------------
CREATE INDEX "idx_sys_oper_log_ot" ON "public"."sys_oper_log" USING btree (
  "oper_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_oper_log
-- ----------------------------
ALTER TABLE "public"."sys_oper_log" ADD CONSTRAINT "sys_oper_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_oper_log_2026_q1
-- ----------------------------
CREATE INDEX "sys_oper_log_2026_q1_oper_time_idx" ON "public"."sys_oper_log_2026_q1" USING btree (
  "oper_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_oper_log_2026_q1
-- ----------------------------
ALTER TABLE "public"."sys_oper_log_2026_q1" ADD CONSTRAINT "sys_oper_log_2026_q1_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_role_value_key" UNIQUE ("role_value");

-- ----------------------------
-- Primary Key structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_role_dept
-- ----------------------------
ALTER TABLE "public"."sys_role_dept" ADD CONSTRAINT "sys_role_dept_pkey" PRIMARY KEY ("role_id", "dept_id");

-- ----------------------------
-- Primary Key structure for table sys_role_menu
-- ----------------------------
ALTER TABLE "public"."sys_role_menu" ADD CONSTRAINT "sys_role_menu_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_test
-- ----------------------------
ALTER TABLE "public"."sys_test" ADD CONSTRAINT "sys_test_pk" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_username_key" UNIQUE ("username");

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table sys_dict_item
-- ----------------------------
ALTER TABLE "public"."sys_dict_item" ADD CONSTRAINT "sys_dict_item_sys_dict_type_id_fk" FOREIGN KEY ("type_id") REFERENCES "public"."sys_dict_type" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_role_menu
-- ----------------------------
ALTER TABLE "public"."sys_role_menu" ADD CONSTRAINT "sys_role_menu_sys_menu_id_fk" FOREIGN KEY ("menu_id") REFERENCES "public"."sys_menu" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."sys_role_menu" ADD CONSTRAINT "sys_role_menu_sys_role_id_fk" FOREIGN KEY ("role_id") REFERENCES "public"."sys_role" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_sys_dept_id_fk" FOREIGN KEY ("dept_id") REFERENCES "public"."sys_dept" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
