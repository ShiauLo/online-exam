-- ===================================================================
-- 最小真实联调种子数据
-- 目标：打通 web + gateway + realtime + issue-core + issue-notify + core
-- 约束：只写固定联调样例，不创建外键，不清空全库，不删除无关业务数据
--
-- 账号前置假设：
--   1 = admin
--   2 = manager
--   3 = teacher_li
--   4 = student_wang
--   5 = auditor_chen
--   6 = operator_zhao
-- 本脚本会将上述联调账号密码统一重置为明文 `123456`
-- （BCrypt 哈希已预生成），避免真实联调时因密码口径漂移导致无法登录。
-- 如果你的 sys_user 主键已调整，请先按实际账号 ID 修改本脚本。
-- ===================================================================

START TRANSACTION;

-- -------------------------------------------------------------------
-- 1. 联调账号密码重置
-- -------------------------------------------------------------------
UPDATE `sys_user`
SET `password` = '$2a$10$wa5YsonMaQ/KXHSYjZpAbe7LUI1mV2azUqIwc7RPrrWGA6AKYY44C'
WHERE `username` IN (
    'admin',
    'manager',
    'teacher_li',
    'student_wang',
    'auditor_chen',
    'operator_zhao'
);

-- -------------------------------------------------------------------
-- 2. 题库、班级、试卷基础数据
-- -------------------------------------------------------------------
INSERT INTO `question_category` (
    `category_id`, `name`, `parent_id`, `is_personal`, `owner_id`, `status`
) VALUES (
    6101, '联调示例分类', NULL, 0, NULL, 1
)
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `parent_id` = VALUES(`parent_id`),
    `is_personal` = VALUES(`is_personal`),
    `owner_id` = VALUES(`owner_id`),
    `status` = VALUES(`status`);

INSERT INTO `exam_class` (
    `class_id`, `class_code`, `class_name`, `description`, `teacher_id`,
    `forced`, `status`, `created_by`
) VALUES (
    501, 'JAVA-001', 'Java 1班', '真实联调最小示例班级', 3,
    0, 'active', 3
)
ON DUPLICATE KEY UPDATE
    `class_code` = VALUES(`class_code`),
    `class_name` = VALUES(`class_name`),
    `description` = VALUES(`description`),
    `teacher_id` = VALUES(`teacher_id`),
    `forced` = VALUES(`forced`),
    `status` = VALUES(`status`),
    `created_by` = VALUES(`created_by`);

INSERT INTO `exam_class_member` (
    `member_id`, `class_id`, `student_id`, `status`, `remark`, `reason`,
    `operated_by`, `apply_time`, `decision_time`
) VALUES (
    501001, 501, 4, 'APPROVED', '真实联调内置样例学生', '初始化联调种子',
    3, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
)
ON DUPLICATE KEY UPDATE
    `member_id` = VALUES(`member_id`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`),
    `reason` = VALUES(`reason`),
    `operated_by` = VALUES(`operated_by`),
    `apply_time` = VALUES(`apply_time`),
    `decision_time` = VALUES(`decision_time`);

INSERT INTO `question_item` (
    `question_id`, `category_id`, `creator_id`, `creator_role_id`, `content`,
    `question_type`, `options`, `answer`, `analysis`, `difficulty`,
    `audit_status`, `is_disabled`, `reference_count`
) VALUES
(
    7001, 6101, 3, 3, 'Java 中，JDK 默认字符编码是？',
    'single', JSON_ARRAY('UTF-8', 'GBK', 'ISO-8859-1', 'ASCII'),
    'A', '联调用客观题，标准答案使用选项 A。', 2,
    'APPROVED', 0, 1
),
(
    7002, 6101, 3, 3, '请简述 JVM 内存结构。',
    'subjective', NULL,
    '关键字：堆、栈、方法区', '联调用主观题，用于验证提交后转入待评分状态。', 3,
    'APPROVED', 0, 1
)
ON DUPLICATE KEY UPDATE
    `category_id` = VALUES(`category_id`),
    `creator_id` = VALUES(`creator_id`),
    `creator_role_id` = VALUES(`creator_role_id`),
    `content` = VALUES(`content`),
    `question_type` = VALUES(`question_type`),
    `options` = VALUES(`options`),
    `answer` = VALUES(`answer`),
    `analysis` = VALUES(`analysis`),
    `difficulty` = VALUES(`difficulty`),
    `audit_status` = VALUES(`audit_status`),
    `is_disabled` = VALUES(`is_disabled`),
    `reference_count` = VALUES(`reference_count`);

INSERT INTO `exam_paper` (
    `paper_id`, `paper_name`, `creator_id`, `creator_role_id`, `status`,
    `source_type`, `exam_time`, `pass_score`, `total_score`,
    `scheduled_exam_time`, `published_at`, `request_id`
) VALUES (
    9101, 'Java 实时联调试卷', 3, 3, 'PUBLISHED',
    'MANUAL', 120, 60, 100,
    DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW(), 'seed-live-integration'
)
ON DUPLICATE KEY UPDATE
    `paper_name` = VALUES(`paper_name`),
    `creator_id` = VALUES(`creator_id`),
    `creator_role_id` = VALUES(`creator_role_id`),
    `status` = VALUES(`status`),
    `source_type` = VALUES(`source_type`),
    `exam_time` = VALUES(`exam_time`),
    `pass_score` = VALUES(`pass_score`),
    `total_score` = VALUES(`total_score`),
    `scheduled_exam_time` = VALUES(`scheduled_exam_time`),
    `published_at` = VALUES(`published_at`),
    `request_id` = VALUES(`request_id`);

INSERT INTO `exam_paper_question` (
    `binding_id`, `paper_id`, `question_id`, `sort_no`, `assigned_score`,
    `question_type`, `difficulty`
) VALUES
(
    910101, 9101, 7001, 1, 50,
    'single', 2
),
(
    910102, 9101, 7002, 2, 50,
    'subjective', 3
)
ON DUPLICATE KEY UPDATE
    `binding_id` = VALUES(`binding_id`),
    `sort_no` = VALUES(`sort_no`),
    `assigned_score` = VALUES(`assigned_score`),
    `question_type` = VALUES(`question_type`),
    `difficulty` = VALUES(`difficulty`);

INSERT INTO `exam_paper_publish_class` (
    `relation_id`, `paper_id`, `class_id`
) VALUES (
    910201, 9101, 501
)
ON DUPLICATE KEY UPDATE
    `relation_id` = VALUES(`relation_id`);

-- -------------------------------------------------------------------
-- 3. 考试、分发、成绩初始化
-- -------------------------------------------------------------------
INSERT INTO `exam_instance` (
    `exam_id`, `exam_name`, `paper_id`, `paper_name`, `status`,
    `creator_id`, `creator_role_id`, `duration`, `start_time`, `request_id`
) VALUES (
    8801, 'Java 实时联调考试', 9101, 'Java 实时联调试卷', 'UNDERWAY',
    3, 3, 120, DATE_SUB(NOW(), INTERVAL 5 MINUTE), 'seed-live-integration'
)
ON DUPLICATE KEY UPDATE
    `exam_name` = VALUES(`exam_name`),
    `paper_id` = VALUES(`paper_id`),
    `paper_name` = VALUES(`paper_name`),
    `status` = VALUES(`status`),
    `creator_id` = VALUES(`creator_id`),
    `creator_role_id` = VALUES(`creator_role_id`),
    `duration` = VALUES(`duration`),
    `start_time` = VALUES(`start_time`),
    `request_id` = VALUES(`request_id`);

INSERT INTO `exam_instance_class` (
    `relation_id`, `exam_id`, `class_id`
) VALUES (
    880101, 8801, 501
)
ON DUPLICATE KEY UPDATE
    `relation_id` = VALUES(`relation_id`);

INSERT INTO `exam_instance_student` (
    `relation_id`, `exam_id`, `class_id`, `student_id`
) VALUES (
    880201, 8801, 501, 4
)
ON DUPLICATE KEY UPDATE
    `relation_id` = VALUES(`relation_id`),
    `class_id` = VALUES(`class_id`);

INSERT INTO `exam_score_record` (
    `score_id`, `exam_id`, `exam_name`, `paper_id`, `paper_name`, `class_id`,
    `class_name`, `student_id`, `student_name`, `total_score`,
    `objective_score`, `subjective_score`, `status`, `submitted_at`,
    `published_at`, `request_id`
) VALUES (
    880301, 8801, 'Java 实时联调考试', 9101, 'Java 实时联调试卷', 501,
    'Java 1班', 4, '王同学', 0,
    0, 0, 'PENDING', NULL,
    NULL, 'seed-live-integration'
)
ON DUPLICATE KEY UPDATE
    `score_id` = VALUES(`score_id`),
    `exam_name` = VALUES(`exam_name`),
    `paper_id` = VALUES(`paper_id`),
    `paper_name` = VALUES(`paper_name`),
    `class_id` = VALUES(`class_id`),
    `class_name` = VALUES(`class_name`),
    `student_name` = VALUES(`student_name`),
    `total_score` = VALUES(`total_score`),
    `objective_score` = VALUES(`objective_score`),
    `subjective_score` = VALUES(`subjective_score`),
    `status` = VALUES(`status`),
    `submitted_at` = VALUES(`submitted_at`),
    `published_at` = VALUES(`published_at`),
    `request_id` = VALUES(`request_id`);

INSERT INTO `exam_score_detail` (
    `detail_id`, `score_id`, `exam_id`, `student_id`, `question_id`, `sort_no`,
    `question_type`, `question_stem`, `student_answer`, `correct_answer`,
    `assigned_score`, `score`, `is_correct`, `review_comment`
) VALUES
(
    880401, 880301, 8801, 4, 7001, 1,
    'single', 'Java 中，JDK 默认字符编码是？', NULL, 'A',
    50, 0, NULL, NULL
),
(
    880402, 880301, 8801, 4, 7002, 2,
    'subjective', '请简述 JVM 内存结构。', NULL, '关键字：堆、栈、方法区',
    50, 0, NULL, NULL
)
ON DUPLICATE KEY UPDATE
    `detail_id` = VALUES(`detail_id`),
    `sort_no` = VALUES(`sort_no`),
    `question_type` = VALUES(`question_type`),
    `question_stem` = VALUES(`question_stem`),
    `student_answer` = VALUES(`student_answer`),
    `correct_answer` = VALUES(`correct_answer`),
    `assigned_score` = VALUES(`assigned_score`),
    `score` = VALUES(`score`),
    `is_correct` = VALUES(`is_correct`),
    `review_comment` = VALUES(`review_comment`);

COMMIT;

-- 执行完成后，建议再跑一遍：
--   pwsh -File .\scripts\live-integration-smoke.ps1
