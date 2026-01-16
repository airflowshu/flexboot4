
-- SysDept Data
-- Level 1
INSERT INTO sys_dept (id, dept_name, order_no, status, parent_id) VALUES ('1', 'Headquarters', 1, 1, NULL);
-- Level 2
INSERT INTO sys_dept (id, dept_name, order_no, status, parent_id) VALUES ('2', 'R&D Department', 1, 1, '1');
-- Level 3
INSERT INTO sys_dept (id, dept_name, order_no, status, parent_id) VALUES ('3', 'Development Team 1', 1, 1, '2');
-- Level 4
INSERT INTO sys_dept (id, dept_name, order_no, status, parent_id) VALUES ('4', 'Backend Group', 1, 1, '3');

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO "public"."sys_dict_item" VALUES ('2', 'user_status', 'enabled', '启用', '1', 1, 20, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('3', 'user_status', 'locked', '锁定', '2', 1, 30, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('4', 'gender', 'male', '男', 'M', 1, 10, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('5', 'gender', 'female', '女', 'F', 1, 20, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('6', 'yes_no', 'yes', '是', 'Y', 1, 10, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('7', 'yes_no', 'no', '否', 'N', 1, 20, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('8', 'dept_status', 'normal', '正常', '1', 1, 10, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('9', 'dept_status', 'stopped', '停用', '0', 1, 20, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-08 08:25:31.149974', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('10', 'region', 'china', '中国', 'CN', 1, 10, NULL, 0, 0, '2026-01-08 08:25:45.242958', '2026-01-08 08:25:45.242958', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('11', 'region', 'anhui', '安徽省', '34', 1, 20, 'china', 0, 0, '2026-01-08 08:25:45.242958', '2026-01-08 08:25:45.242958', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('12', 'region', 'hefei', '合肥市', '3401', 1, 30, 'anhui', 0, 0, '2026-01-08 08:25:45.242958', '2026-01-08 08:25:45.242958', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('13', 'region', 'luyang', '庐阳区', '340103', 1, 40, 'hefei', 0, 0, '2026-01-08 08:25:45.242958', '2026-01-08 08:25:45.242958', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_item" VALUES ('1', 'user_status', 'disabled', '禁用', '0', 1, 10, NULL, 0, 0, '2026-01-08 08:25:31.149974', '2026-01-12 15:59:18.505416', NULL, 'super', NULL);

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO "public"."sys_dict_type" VALUES ('1', 'user_status', '用户状态', 1, 10, 0, 0, '2026-01-08 08:25:31.082879', '2026-01-08 08:25:31.082879', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('2', 'gender', '性别', 1, 20, 0, 0, '2026-01-08 08:25:31.082879', '2026-01-08 08:25:31.082879', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('3', 'yes_no', '是/否', 1, 30, 0, 0, '2026-01-08 08:25:31.082879', '2026-01-08 08:25:31.082879', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('4', 'dept_status', '部门状态', 1, 40, 0, 0, '2026-01-08 08:25:31.082879', '2026-01-08 08:25:31.082879', NULL, NULL, NULL);
INSERT INTO "public"."sys_dict_type" VALUES ('5', 'region', '行政区划', 1, 50, 0, 0, '2026-01-08 08:25:45.214786', '2026-01-08 08:25:45.214786', NULL, NULL, NULL);