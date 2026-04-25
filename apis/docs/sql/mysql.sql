-- ===================================================================
-- 数据库脚本：账号管理板块 (RBAC模型)
-- 功能：创建用户、角色、权限及关联表，并初始化基础数据
-- 约束：按当前仓库规范，所有表仅保留主键、唯一键和普通索引，不定义外键
-- ===================================================================

-- 切换到您的目标数据库
-- USE your_database_name;

-- -------------------------------------------------------------------
-- 1. 用户表 (sys_user)
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                            `username` varchar(50) NOT NULL COMMENT '登录用户名（唯一）',
                            `password` varchar(100) DEFAULT NULL COMMENT '加密存储的密码（如BCrypt加密）',
                            `real_name` varchar(50) NOT NULL COMMENT '用户真实姓名',
                            `role_id` int NOT NULL COMMENT '关联角色ID',
                            `status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态（0-禁用，1-正常）',
                            `phone_number` varchar(11) NOT NULL DEFAULT '' COMMENT '联系电话（可选）',
                            `email` varchar(100) DEFAULT NULL COMMENT '邮箱（可选）',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`),
                            KEY `idx_role_id` (`role_id`) COMMENT '角色ID索引，用于关联查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';


-- -------------------------------------------------------------------
-- 2. 角色表 (sys_role)
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
                            `id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                            `role_name` varchar(50) NOT NULL COMMENT '角色名称（唯一）',
                            `role_code` varchar(30) NOT NULL COMMENT '角色编码（唯一，如：SUPER_ADMIN）',
                            `role_desc` varchar(200) DEFAULT NULL COMMENT '角色描述',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_role_name` (`role_name`),
                            UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 初始化角色数据
INSERT INTO `sys_role` (`role_name`, `role_code`, `role_desc`) VALUES
                                                                   ('超级管理员', 'SUPER_ADMIN', '系统最高权限角色，拥有所有操作权限'),
                                                                   ('普通管理员', 'ADMIN', '负责业务类问题处理及管理'),
                                                                   ('教师', 'TEACHER', '可提交问题申报，查看自身相关问题'),
                                                                   ('学生', 'STUDENT', '可提交问题申报，跟踪自身问题进度'),
                                                                   ('审计员', 'AUDITOR', '可查看所有问题申报记录，无处理权限'),
                                                                   ('系统运维', 'OPERATOR', '负责处理系统故障类问题');


-- -------------------------------------------------------------------
-- 3. 权限表 (sys_permission)
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
                                  `id` int NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                                  `permission_name` varchar(100) NOT NULL COMMENT '权限名称（如：问题申报提交）',
                                  `permission_code` varchar(50) NOT NULL COMMENT '权限编码（唯一，如：PROBLEM_SUBMIT）',
                                  `module` varchar(50) NOT NULL COMMENT '所属模块（如：问题申报模块）',
                                  `remark` varchar(200) DEFAULT NULL COMMENT '权限说明（对应文档备注）',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_permission_code` (`permission_code`),
                                  KEY `idx_module` (`module`) COMMENT '模块索引，用于按模块查询权限'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 初始化问题申报模块权限数据
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `module`, `remark`) VALUES
('问题申报提交', 'PROBLEM_SUBMIT', '问题申报模块', '需选择问题类型，上传截图佐证'),
('问题查看', 'PROBLEM_VIEW', '问题申报模块', '仅能查看自身提交或负责处理的问题；审计员可查看所有问题'),
('问题处理', 'PROBLEM_HANDLE', '问题申报模块', '系统故障类问题由系统运维处理；业务类问题由管理员处理'),
('问题转派', 'PROBLEM_TRANSFER', '问题申报模块', '可将问题转派给对应负责人，保留转派记录'),
('问题关闭', 'PROBLEM_CLOSE', '问题申报模块', '需确认问题已解决，且申请人无异议'),
('问题进度跟踪', 'PROBLEM_TRACK', '问题申报模块', '申请人可查看问题处理状态（待处理/处理中/已解决/已关闭）');


-- -------------------------------------------------------------------
-- 4. 角色权限关联表 (sys_role_permission)
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联ID',
`role_id` int NOT NULL COMMENT '角色ID',
`permission_id` int NOT NULL COMMENT '权限ID',
PRIMARY KEY (`id`),
UNIQUE KEY `uk_role_permission` (`role_id`,`permission_id`) COMMENT '防止角色权限重复关联',
KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 初始化角色权限关联数据
-- 超级管理员：所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE role_code='SUPER_ADMIN'), id FROM sys_permission;

-- 普通管理员：核心操作权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE role_code='ADMIN'), id
FROM sys_permission
WHERE permission_code IN ('PROBLEM_SUBMIT', 'PROBLEM_VIEW', 'PROBLEM_HANDLE', 'PROBLEM_TRANSFER', 'PROBLEM_CLOSE', 'PROBLEM_TRACK');

-- 教师：提交与查看权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE role_code='TEACHER'), id
FROM sys_permission
WHERE permission_code IN ('PROBLEM_SUBMIT', 'PROBLEM_VIEW', 'PROBLEM_TRACK');

-- 学生：提交与查看权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE role_code='STUDENT'), id
FROM sys_permission
WHERE permission_code IN ('PROBLEM_SUBMIT', 'PROBLEM_VIEW', 'PROBLEM_TRACK');

-- 审计员：仅查看权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE role_code='AUDITOR'), id
FROM sys_permission
WHERE permission_code='PROBLEM_VIEW';

-- 系统运维：技术处理权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE role_code='OPERATOR'), id
FROM sys_permission
WHERE permission_code IN ('PROBLEM_SUBMIT', 'PROBLEM_VIEW', 'PROBLEM_HANDLE', 'PROBLEM_TRANSFER', 'PROBLEM_CLOSE');


-- -------------------------------------------------------------------
-- 5. 初始化用户数据 (示例)
-- 注意：密码为明文'123456'，实际生产环境中应使用BCrypt等算法加密后再存入
-- -------------------------------------------------------------------
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role_id`, `status`, `email`) VALUES
('admin', '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C', '超级管理员', (SELECT id FROM sys_role WHERE role_code='SUPER_ADMIN'), 1, 'admin@example.com'),
('manager', '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C', '张经理', (SELECT id FROM sys_role WHERE role_code='ADMIN'), 1, 'manager@example.com'),
('teacher_li', '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C', '李老师', (SELECT id FROM sys_role WHERE role_code='TEACHER'), 1, 'teacher_li@example.com'),
('student_wang', '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C', '王同学', (SELECT id FROM sys_role WHERE role_code='STUDENT'), 1, 'student_wang@example.com'),
('auditor_chen', '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C', '陈审计', (SELECT id FROM sys_role WHERE role_code='AUDITOR'), 1, 'auditor_chen@example.com'),
('operator_zhao', '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C', '赵运维', (SELECT id FROM sys_role WHERE role_code='OPERATOR'), 1, 'operator_zhao@example.com');

-- -------------------------------------------------------------------
-- 6. 系统账户解禁封禁记录表 (sys_account_status_log)
-- -------------------------------------------------------------------
CREATE TABLE `sys_account_status_log` (
                                          `id` bigint NOT NULL COMMENT '主键（雪花算法ID）',
                                          `operate_user_id` bigint NOT NULL COMMENT '操作人ID（管理员/运营账号ID）',
                                          `operate_user_name` varchar(50) NOT NULL COMMENT '操作人账号/姓名（冗余存储，避免操作人账号删除后无记录）',
                                          `operate_user_role` varchar(20) NOT NULL COMMENT '操作人角色（ADMIN/TEACHER/OPERATOR）',
                                          `target_user_id` bigint NOT NULL COMMENT '被操作的账号ID',
                                          `target_user_phone` varchar(11) NOT NULL COMMENT '被操作账号的手机号（冗余存储）',
                                          `operate_type` tinyint NOT NULL COMMENT '操作类型：1=封禁，2=解封',
                                          `operate_reason` varchar(500) NOT NULL COMMENT '操作原因（必填，比如“违规答题”“误封解封”“超期封禁自动解封”）',
                                          `before_status` tinyint NOT NULL COMMENT '操作前账号状态：0=正常，1=封禁',
                                          `after_status` tinyint NOT NULL COMMENT '操作后账号状态：0=正常，1=封禁',
                                          `operate_ip` varchar(50) DEFAULT NULL COMMENT '操作IP地址（客户端IP）',
                                          `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                          PRIMARY KEY (`id`),
    -- 常用查询索引：按被操作账号、操作时间、操作类型查
                                          KEY `idx_target_user_id` (`target_user_id`),
                                          KEY `idx_operate_time` (`operate_time`),
                                          KEY `idx_operate_type` (`operate_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号封禁/解封操作日志表';

-- -------------------------------------------------------------------
-- (可选) 用于测试环境的数据清理语句
-- 如需重置数据，请取消以下注释并按顺序执行
-- -------------------------------------------------------------------
/*
-- 注意：TRUNCATE会清空表数据并重置自增ID，且无法回滚。请在测试环境中使用。
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_role;
*/

-- -------------------------------------------------------------------
-- 7. 班级域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_class_import_record`;
DROP TABLE IF EXISTS `exam_class_audit_log`;
DROP TABLE IF EXISTS `exam_class_member`;
DROP TABLE IF EXISTS `exam_class`;

CREATE TABLE `exam_class` (
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `class_code` varchar(32) NOT NULL COMMENT '班级编码',
    `class_name` varchar(64) NOT NULL COMMENT '班级名称',
    `description` varchar(500) DEFAULT NULL COMMENT '班级描述',
    `teacher_id` bigint DEFAULT NULL COMMENT '教师ID',
    `forced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否强制班级',
    `status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '班级状态',
    `created_by` bigint NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`class_id`),
    UNIQUE KEY `uk_exam_class_code` (`class_code`),
    KEY `idx_exam_class_teacher_id` (`teacher_id`),
    KEY `idx_exam_class_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级主表';

CREATE TABLE `exam_class_member` (
    `member_id` bigint NOT NULL COMMENT '班级成员ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `status` varchar(32) NOT NULL COMMENT '成员状态',
    `remark` varchar(255) DEFAULT NULL COMMENT '申请备注',
    `reason` varchar(255) DEFAULT NULL COMMENT '处理原因',
    `operated_by` bigint DEFAULT NULL COMMENT '最近操作人ID',
    `apply_time` datetime DEFAULT NULL COMMENT '申请时间',
    `decision_time` datetime DEFAULT NULL COMMENT '审批/处理时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`member_id`),
    UNIQUE KEY `uk_exam_class_member_student` (`class_id`,`student_id`),
    KEY `idx_exam_class_member_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级成员表';

CREATE TABLE `exam_class_import_record` (
    `record_id` bigint NOT NULL COMMENT '导入记录ID',
    `file_name` varchar(255) NOT NULL COMMENT '文件名',
    `default_teacher_id` bigint DEFAULT NULL COMMENT '默认教师ID',
    `imported_count` int NOT NULL DEFAULT 0 COMMENT '成功导入数量',
    `skipped_count` int NOT NULL DEFAULT 0 COMMENT '跳过数量',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `remark` varchar(1000) DEFAULT NULL COMMENT '导入备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`record_id`),
    KEY `idx_exam_class_import_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级导入记录表';

CREATE TABLE `exam_class_audit_log` (
    `audit_log_id` bigint NOT NULL COMMENT '审计日志ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `member_id` bigint DEFAULT NULL COMMENT '班级成员ID',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `action_type` varchar(64) NOT NULL COMMENT '动作类型（class.create/class.update/class.delete/class.apply-join/class.approve-join/class.remove-student/class.quit）',
    `target_student_id` bigint DEFAULT NULL COMMENT '目标学生ID',
    `detail` varchar(1000) DEFAULT NULL COMMENT '操作详情',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `operate_time` datetime NOT NULL COMMENT '操作时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`audit_log_id`),
    KEY `idx_exam_class_audit_class_id` (`class_id`),
    KEY `idx_exam_class_audit_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级审计日志表';

-- -------------------------------------------------------------------
-- 8. 题库域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `question_audit_log`;
DROP TABLE IF EXISTS `question_item`;
DROP TABLE IF EXISTS `question_category`;

CREATE TABLE `question_category` (
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `name` varchar(128) NOT NULL COMMENT '分类名称',
    `parent_id` bigint DEFAULT NULL COMMENT '父级分类ID',
    `is_personal` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否个人分类',
    `owner_id` bigint DEFAULT NULL COMMENT '个人分类所属人',
    `status` int NOT NULL DEFAULT 1 COMMENT '分类状态',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`category_id`),
    KEY `idx_question_category_parent_id` (`parent_id`),
    KEY `idx_question_category_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库分类表';

CREATE TABLE `question_item` (
    `question_id` bigint NOT NULL COMMENT '题目ID',
    `category_id` bigint NOT NULL COMMENT '分类ID',
    `creator_id` bigint NOT NULL COMMENT '创建人ID',
    `creator_role_id` int NOT NULL COMMENT '创建人角色ID',
    `content` text NOT NULL COMMENT '题目内容',
    `question_type` varchar(32) NOT NULL COMMENT '题目类型',
    `options` json DEFAULT NULL COMMENT '选项JSON',
    `answer` text NOT NULL COMMENT '答案',
    `analysis` text DEFAULT NULL COMMENT '解析',
    `difficulty` int NOT NULL COMMENT '难度',
    `audit_status` varchar(32) NOT NULL COMMENT '审核状态',
    `is_disabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用',
    `reference_count` int NOT NULL DEFAULT 0 COMMENT '被引用次数',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`question_id`),
    KEY `idx_question_item_category_id` (`category_id`),
    KEY `idx_question_item_creator_id` (`creator_id`),
    KEY `idx_question_item_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

CREATE TABLE `question_audit_log` (
    `audit_log_id` bigint NOT NULL COMMENT '审核日志ID',
    `question_id` bigint NOT NULL COMMENT '题目ID',
    `auditor_id` bigint NOT NULL COMMENT '审核人ID',
    `audit_result` varchar(32) NOT NULL COMMENT '审核结果',
    `reason` varchar(500) DEFAULT NULL COMMENT '审核说明',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `audit_time` datetime NOT NULL COMMENT '审核时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`audit_log_id`),
    KEY `idx_question_audit_log_question_id` (`question_id`),
    KEY `idx_question_audit_log_auditor_id` (`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目审核日志表';

-- -------------------------------------------------------------------
-- 9. 试卷域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_paper_audit_log`;
DROP TABLE IF EXISTS `exam_paper_publish_class`;
DROP TABLE IF EXISTS `exam_paper_question`;
DROP TABLE IF EXISTS `exam_paper`;

CREATE TABLE `exam_paper` (
    `paper_id` bigint NOT NULL COMMENT '试卷ID',
    `paper_name` varchar(128) NOT NULL COMMENT '试卷名称',
    `creator_id` bigint NOT NULL COMMENT '创建人ID',
    `creator_role_id` int NOT NULL COMMENT '创建人角色ID',
    `status` varchar(32) NOT NULL COMMENT '试卷生命周期状态（DRAFT/APPROVED/REJECTED/PUBLISHED/TERMINATED/RECYCLED）',
    `source_type` varchar(32) NOT NULL COMMENT '组卷来源（MANUAL/AUTO）',
    `exam_time` int NOT NULL COMMENT '考试时长（分钟）',
    `pass_score` int NOT NULL COMMENT '及格分',
    `total_score` int NOT NULL COMMENT '总分（当前按题目数计分）',
    `scheduled_exam_time` datetime DEFAULT NULL COMMENT '计划开考时间',
    `published_at` datetime DEFAULT NULL COMMENT '发布时间',
    `recycled_at` datetime DEFAULT NULL COMMENT '回收时间',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`paper_id`),
    KEY `idx_exam_paper_creator_id` (`creator_id`),
    KEY `idx_exam_paper_status` (`status`),
    KEY `idx_exam_paper_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷主表';

CREATE TABLE `exam_paper_question` (
    `binding_id` bigint NOT NULL COMMENT '绑定ID',
    `paper_id` bigint NOT NULL COMMENT '试卷ID',
    `question_id` bigint NOT NULL COMMENT '题目ID',
    `sort_no` int NOT NULL COMMENT '题目顺序',
    `assigned_score` int NOT NULL DEFAULT 1 COMMENT '单题分值',
    `question_type` varchar(32) NOT NULL COMMENT '题目类型',
    `difficulty` int NOT NULL COMMENT '题目难度',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`binding_id`),
    UNIQUE KEY `uk_exam_paper_question` (`paper_id`,`question_id`),
    KEY `idx_exam_paper_question_question_id` (`question_id`),
    KEY `idx_exam_paper_question_paper_id` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目绑定表';

CREATE TABLE `exam_paper_publish_class` (
    `relation_id` bigint NOT NULL COMMENT '发布关联ID',
    `paper_id` bigint NOT NULL COMMENT '试卷ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `uk_exam_paper_publish_class` (`paper_id`,`class_id`),
    KEY `idx_exam_paper_publish_class_class_id` (`class_id`),
    KEY `idx_exam_paper_publish_class_paper_id` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷发布班级关联表';

CREATE TABLE `exam_paper_audit_log` (
    `audit_log_id` bigint NOT NULL COMMENT '审核日志ID',
    `paper_id` bigint NOT NULL COMMENT '试卷ID',
    `auditor_id` bigint NOT NULL COMMENT '审核人ID',
    `audit_result` varchar(32) NOT NULL COMMENT '审核结果（APPROVED/REJECTED/TERMINATED）',
    `reason` varchar(500) DEFAULT NULL COMMENT '审核或终止说明',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `audit_time` datetime NOT NULL COMMENT '审核时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`audit_log_id`),
    KEY `idx_exam_paper_audit_log_paper_id` (`paper_id`),
    KEY `idx_exam_paper_audit_log_auditor_id` (`auditor_id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷审核与终止日志表';

-- -------------------------------------------------------------------
-- 10. 考试核心域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_instance_retest_apply`;
DROP TABLE IF EXISTS `exam_instance_status_log`;
DROP TABLE IF EXISTS `exam_instance_student`;
DROP TABLE IF EXISTS `exam_instance_class`;
DROP TABLE IF EXISTS `exam_instance`;

CREATE TABLE `exam_instance` (
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `exam_name` varchar(128) NOT NULL COMMENT '考试名称',
    `paper_id` bigint NOT NULL COMMENT '关联试卷ID',
    `paper_name` varchar(128) NOT NULL COMMENT '试卷名称快照',
    `status` varchar(32) NOT NULL COMMENT '考试状态（DRAFT/PUBLISHED/UNDERWAY/PAUSED/ENDED/TERMINATED）',
    `creator_id` bigint NOT NULL COMMENT '创建人ID',
    `creator_role_id` int NOT NULL COMMENT '创建人角色ID',
    `duration` int NOT NULL COMMENT '考试时长（分钟）',
    `start_time` datetime NOT NULL COMMENT '计划开考时间',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`exam_id`),
    KEY `idx_exam_instance_paper_id` (`paper_id`),
    KEY `idx_exam_instance_creator_id` (`creator_id`),
    KEY `idx_exam_instance_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试实例表';

CREATE TABLE `exam_instance_class` (
    `relation_id` bigint NOT NULL COMMENT '考试班级关联ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `uk_exam_instance_class` (`exam_id`,`class_id`),
    KEY `idx_exam_instance_class_class_id` (`class_id`),
    KEY `idx_exam_instance_class_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试班级关联表';

CREATE TABLE `exam_instance_student` (
    `relation_id` bigint NOT NULL COMMENT '考试学生分发ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `class_id` bigint NOT NULL COMMENT '来源班级ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`relation_id`),
    UNIQUE KEY `uk_exam_instance_student` (`exam_id`,`student_id`),
    KEY `idx_exam_instance_student_student_id` (`student_id`),
    KEY `idx_exam_instance_student_class_id` (`class_id`),
    KEY `idx_exam_instance_student_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试学生分发表';

CREATE TABLE `exam_instance_status_log` (
    `log_id` bigint NOT NULL COMMENT '状态日志ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `operator_role` varchar(32) NOT NULL COMMENT '操作人角色编码',
    `action_type` varchar(64) NOT NULL COMMENT '动作类型（exam.pause/exam.resume）',
    `from_status` varchar(32) NOT NULL COMMENT '原状态',
    `to_status` varchar(32) NOT NULL COMMENT '目标状态',
    `reason` varchar(500) NOT NULL COMMENT '操作原因',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `operate_time` datetime NOT NULL COMMENT '操作时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_exam_instance_status_log_exam_id` (`exam_id`),
    KEY `idx_exam_instance_status_log_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试状态切换日志表';

CREATE TABLE `exam_instance_retest_apply` (
    `retest_apply_id` bigint NOT NULL COMMENT '补考申请ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `status` varchar(32) NOT NULL COMMENT '申请状态（PENDING/APPROVED/REJECTED）',
    `apply_reason` varchar(500) DEFAULT NULL COMMENT '申请原因',
    `decision_reason` varchar(500) DEFAULT NULL COMMENT '审核说明',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `review_time` datetime DEFAULT NULL COMMENT '审核时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`retest_apply_id`),
    UNIQUE KEY `uk_exam_instance_retest_apply` (`exam_id`,`student_id`),
    KEY `idx_exam_instance_retest_apply_class_id` (`class_id`),
    KEY `idx_exam_instance_retest_apply_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试补考申请表';

-- -------------------------------------------------------------------
-- 11. 成绩域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_score_change_log`;
DROP TABLE IF EXISTS `exam_score_appeal`;
DROP TABLE IF EXISTS `exam_score_detail`;
DROP TABLE IF EXISTS `exam_score_record`;
DROP TABLE IF EXISTS `exam_realtime_abnormal_record`;

CREATE TABLE `exam_score_record` (
    `score_id` bigint NOT NULL COMMENT '成绩记录ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `exam_name` varchar(128) NOT NULL COMMENT '考试名称快照',
    `paper_id` bigint NOT NULL COMMENT '试卷ID',
    `paper_name` varchar(128) NOT NULL COMMENT '试卷名称快照',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `class_name` varchar(128) NOT NULL COMMENT '班级名称快照',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `student_name` varchar(64) NOT NULL COMMENT '学生姓名快照',
    `total_score` int NOT NULL COMMENT '总成绩',
    `objective_score` int NOT NULL DEFAULT 0 COMMENT '客观题得分',
    `subjective_score` int NOT NULL DEFAULT 0 COMMENT '主观题得分',
    `status` varchar(32) NOT NULL COMMENT '成绩生命周期状态（PENDING=待自动评分或待评分，SCORING=客观题已完成且仍待主观题，SCORED=评分完成待发布或待人工调整，PUBLISHED=成绩已发布可查询/可申诉）',
    `submitted_at` datetime DEFAULT NULL COMMENT '交卷时间',
    `published_at` datetime DEFAULT NULL COMMENT '发布时间',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`score_id`),
    UNIQUE KEY `uk_exam_score_record_exam_student` (`exam_id`,`student_id`),
    KEY `idx_exam_score_record_class_id` (`class_id`),
    KEY `idx_exam_score_record_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩记录表';

CREATE TABLE `exam_score_detail` (
    `detail_id` bigint NOT NULL COMMENT '成绩明细ID',
    `score_id` bigint NOT NULL COMMENT '成绩记录ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `question_id` bigint NOT NULL COMMENT '题目ID',
    `sort_no` int NOT NULL COMMENT '题目顺序',
    `question_type` varchar(32) NOT NULL COMMENT '题型',
    `question_stem` text NOT NULL COMMENT '题干快照',
    `student_answer` text DEFAULT NULL COMMENT '学生答案',
    `correct_answer` text DEFAULT NULL COMMENT '标准答案快照',
    `assigned_score` int NOT NULL COMMENT '单题满分',
    `score` int NOT NULL DEFAULT 0 COMMENT '单题得分',
    `is_correct` tinyint(1) DEFAULT NULL COMMENT '是否判定正确',
    `review_comment` varchar(500) DEFAULT NULL COMMENT '评语（当前也兼作主观题已批阅标记，空字符串表示已批阅但无额外评语）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`detail_id`),
    UNIQUE KEY `uk_exam_score_detail_score_question` (`score_id`,`question_id`),
    KEY `idx_exam_score_detail_exam_student` (`exam_id`,`student_id`),
    KEY `idx_exam_score_detail_score_id` (`score_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩明细表';

CREATE TABLE `exam_score_appeal` (
    `appeal_id` bigint NOT NULL COMMENT '申诉ID',
    `score_id` bigint NOT NULL COMMENT '成绩记录ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `question_id` bigint NOT NULL COMMENT '题目ID',
    `appeal_reason` varchar(500) NOT NULL COMMENT '申诉原因',
    `status` varchar(32) NOT NULL COMMENT '申诉状态（PENDING=待处理，APPROVED=已通过并回退成绩到待重新批阅，REJECTED=已驳回）',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `handler_id` bigint DEFAULT NULL COMMENT '处理人ID',
    `handle_result` varchar(32) DEFAULT NULL COMMENT '处理结果快照',
    `handle_reason` varchar(500) DEFAULT NULL COMMENT '处理说明',
    `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`appeal_id`),
    KEY `idx_exam_score_appeal_score_id` (`score_id`),
    KEY `idx_exam_score_appeal_student_id` (`student_id`),
    KEY `idx_exam_score_appeal_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩申诉表';

CREATE TABLE `exam_realtime_abnormal_record` (
    `abnormal_id` bigint NOT NULL COMMENT '实时异常记录ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `reporter_id` bigint NOT NULL COMMENT '上报人ID',
    `reporter_role_id` int NOT NULL COMMENT '上报人角色ID',
    `type` varchar(64) NOT NULL COMMENT '异常类型（如 screen-out/submit-failed/network）',
    `description` varchar(500) NOT NULL COMMENT '异常描述',
    `img_urls` json DEFAULT NULL COMMENT '截图URL列表',
    `screen_out_count` int DEFAULT NULL COMMENT '切屏次数快照，非切屏类异常可为空',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`abnormal_id`),
    KEY `idx_exam_realtime_abnormal_exam_id` (`exam_id`),
    KEY `idx_exam_realtime_abnormal_reporter_id` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试实时异常记录表';

CREATE TABLE `exam_score_change_log` (
    `change_log_id` bigint NOT NULL COMMENT '成绩修改日志ID',
    `score_id` bigint NOT NULL COMMENT '成绩记录ID',
    `exam_id` bigint NOT NULL COMMENT '考试ID',
    `class_id` bigint NOT NULL COMMENT '班级ID',
    `student_id` bigint NOT NULL COMMENT '学生ID',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `approver_id` bigint NOT NULL COMMENT '审批人ID',
    `previous_total_score` int NOT NULL COMMENT '修改前总分',
    `new_total_score` int NOT NULL COMMENT '修改后总分',
    `reason` varchar(500) NOT NULL COMMENT '修改原因',
    `request_id` varchar(64) DEFAULT NULL COMMENT '请求链路ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`change_log_id`),
    KEY `idx_exam_score_change_log_score_id` (`score_id`),
    KEY `idx_exam_score_change_log_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩修改日志表';

-- -------------------------------------------------------------------
-- 12. 问题申报域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `exam_issue_process_log`;
DROP TABLE IF EXISTS `exam_issue_record`;

CREATE TABLE `exam_issue_record` (
    `issue_id` bigint NOT NULL COMMENT '问题ID',
    `type` varchar(32) NOT NULL COMMENT '问题类型（BUSINESS/EXAM/SYSTEM）',
    `title` varchar(128) NOT NULL COMMENT '问题标题',
    `description` varchar(1000) NOT NULL COMMENT '问题描述',
    `status` varchar(32) NOT NULL COMMENT '问题状态（PENDING/PROCESSING/CLOSED）',
    `reporter_id` bigint NOT NULL COMMENT '申报人ID',
    `current_handler_id` bigint DEFAULT NULL COMMENT '当前处理人ID',
    `exam_id` bigint DEFAULT NULL COMMENT '关联考试ID',
    `class_id` bigint DEFAULT NULL COMMENT '关联班级ID',
    `latest_result` varchar(256) DEFAULT NULL COMMENT '最新处理结果摘要',
    `latest_solution` varchar(1000) DEFAULT NULL COMMENT '最新处理方案',
    `img_urls` json DEFAULT NULL COMMENT '截图URL JSON列表',
    `audit_trail` json NOT NULL COMMENT '共享审计轨迹JSON',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`issue_id`),
    KEY `idx_exam_issue_record_type` (`type`),
    KEY `idx_exam_issue_record_status` (`status`),
    KEY `idx_exam_issue_record_reporter_id` (`reporter_id`),
    KEY `idx_exam_issue_record_current_handler_id` (`current_handler_id`),
    KEY `idx_exam_issue_record_exam_id` (`exam_id`),
    KEY `idx_exam_issue_record_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题申报主记录表';

CREATE TABLE `exam_issue_process_log` (
    `log_id` bigint NOT NULL COMMENT '问题轨迹日志ID',
    `issue_id` bigint NOT NULL COMMENT '问题ID',
    `action` varchar(32) NOT NULL COMMENT '轨迹动作（CREATED/HANDLED/TRANSFERRED/CLOSED）',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `from_handler_id` bigint DEFAULT NULL COMMENT '原处理人ID',
    `to_handler_id` bigint DEFAULT NULL COMMENT '目标处理人ID',
    `content` varchar(1000) DEFAULT NULL COMMENT '轨迹内容',
    `audit_trail` json NOT NULL COMMENT '共享审计轨迹JSON',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_exam_issue_process_log_issue_id` (`issue_id`),
    KEY `idx_exam_issue_process_log_action` (`action`),
    KEY `idx_exam_issue_process_log_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问题申报处理轨迹表';

-- -------------------------------------------------------------------
-- 13. 系统域表结构
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS `system_audit_record`;
DROP TABLE IF EXISTS `system_backup_record`;
DROP TABLE IF EXISTS `system_log`;
DROP TABLE IF EXISTS `system_alarm_setting`;
DROP TABLE IF EXISTS `system_config`;
DROP TABLE IF EXISTS `system_permission_assignment`;
DROP TABLE IF EXISTS `system_role`;

CREATE TABLE `system_role` (
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `role_name` varchar(64) NOT NULL COMMENT '角色名称',
    `permission_ids` json NOT NULL COMMENT '权限码JSON列表',
    `template_id` bigint DEFAULT NULL COMMENT '模板ID',
    `created_by` bigint NOT NULL COMMENT '创建人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE `system_permission_assignment` (
    `assignment_id` bigint NOT NULL COMMENT '权限分配ID',
    `account_id` bigint NOT NULL COMMENT '账号ID',
    `role_id` int NOT NULL COMMENT '分配角色ID',
    `expire_time` varchar(64) DEFAULT NULL COMMENT '失效时间文本',
    `assigned_by` bigint NOT NULL COMMENT '分配人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`assignment_id`),
    UNIQUE KEY `uk_system_permission_assignment_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号权限分配表';

CREATE TABLE `system_config` (
    `config_id` bigint NOT NULL COMMENT '配置ID',
    `config_key` varchar(128) NOT NULL COMMENT '配置键',
    `config_value` varchar(1000) NOT NULL COMMENT '配置值',
    `category` varchar(64) NOT NULL COMMENT '配置分类',
    `updated_by` bigint NOT NULL COMMENT '更新人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE `system_alarm_setting` (
    `alarm_id` bigint NOT NULL COMMENT '告警设置ID',
    `alarm_type` varchar(64) NOT NULL COMMENT '告警类型',
    `threshold` varchar(128) NOT NULL COMMENT '阈值',
    `recipients` json NOT NULL COMMENT '接收人列表JSON',
    `updated_by` bigint NOT NULL COMMENT '更新人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`alarm_id`),
    UNIQUE KEY `uk_system_alarm_type` (`alarm_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统告警设置表';

CREATE TABLE `system_log` (
    `log_id` bigint NOT NULL COMMENT '日志ID',
    `log_type` varchar(64) NOT NULL COMMENT '日志类型',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `operator` varchar(64) NOT NULL COMMENT '操作人角色标识',
    `detail` varchar(1000) DEFAULT NULL COMMENT '日志详情',
    `approver_id` varchar(64) DEFAULT NULL COMMENT '审批人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_system_log_type` (`log_type`),
    KEY `idx_system_log_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

CREATE TABLE `system_backup_record` (
    `backup_id` bigint NOT NULL COMMENT '备份记录ID',
    `backup_type` varchar(64) NOT NULL COMMENT '备份类型',
    `status` varchar(32) NOT NULL COMMENT '异步任务状态（BACKUP_PENDING/BACKUP_RUNNING/BACKUP_SUCCESS/BACKUP_FAILED/RESTORE_PENDING/RESTORE_RUNNING/RESTORE_SUCCESS/RESTORE_FAILED）',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`backup_id`),
    KEY `idx_system_backup_type` (`backup_type`),
    KEY `idx_system_backup_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统备份记录表';

CREATE TABLE `system_notification_record` (
    `notification_id` bigint NOT NULL COMMENT '通知记录ID',
    `event_type` varchar(64) NOT NULL COMMENT '事件类型',
    `target_type` varchar(64) NOT NULL COMMENT '目标类型',
    `target_id` varchar(64) DEFAULT NULL COMMENT '目标ID',
    `status` varchar(32) NOT NULL COMMENT '通知状态（PENDING/FAILED）',
    `channel` varchar(32) NOT NULL COMMENT '通知通道（当前固定 OUTBOX）',
    `payload` text NOT NULL COMMENT '通知载荷JSON',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
    `request_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`notification_id`),
    KEY `idx_system_notification_event_type` (`event_type`),
    KEY `idx_system_notification_status` (`status`),
    KEY `idx_system_notification_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知出站记录表';

CREATE TABLE `system_audit_record` (
    `audit_id` bigint NOT NULL COMMENT '审计记录ID',
    `action_type` varchar(64) NOT NULL COMMENT '动作类型',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `target_type` varchar(64) NOT NULL COMMENT '目标类型',
    `target_id` varchar(64) DEFAULT NULL COMMENT '目标ID',
    `request_id` varchar(64) DEFAULT NULL COMMENT '链路ID',
    `detail` varchar(1000) DEFAULT NULL COMMENT '详情',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`audit_id`),
    KEY `idx_system_audit_action_type` (`action_type`),
    KEY `idx_system_audit_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统审计记录表';

-- ===================================================================
-- 脚本执行完毕
-- ===================================================================
