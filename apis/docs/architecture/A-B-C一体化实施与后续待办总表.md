# A / B / C 一体化实施与后续待办总表

## 文档版本

- 版本：V1.8
- 更新时间：2026-04-26
- 维护人：Codex

## 文档定位

- 目标：把线程 A、线程 B、线程 C 当前已完成内容、统一后续待办、跨线程收口事项和服务建设缺口合并到一个文档里，作为当前仓库的单一执行总表。
- 适用范围：共享层 `exam-common` 与当前 5 个运行服务：`exam-gateway`、`exam-account`、`exam-class`、`exam-core`、`exam-realtime`，以及这些运行服务承接的系统治理、题库、试卷、成绩、资源、问题申报、问题通知等能力域和历史拆分记录。
- 使用原则：后续优先以本文档为总清单推进；原 A/B/C 文档保留为历史分工记录。

## 一、当前仓库已完成的统一基线

### 1. 当前运行拓扑

- 当前运行拓扑统一固定为 5 个服务：`exam-gateway`、`exam-account`、`exam-class`、`exam-core`、`exam-realtime`。
- 当前宿主聚合关系统一为：
  - `exam-account = account + system`
  - `exam-class = class + question + paper + resource`
  - `exam-core = core + score + issue-core`
  - `exam-realtime = realtime + issue-notify`
- `exam-system`、`exam-question`、`exam-paper`、`exam-resource`、`exam-score`、`exam-issue-core`、`exam-issue-notify` 在本文中仅作为能力域或历史拆分名保留，不再视为当前独立运行微服务。

### 2. 共享层

- `exam-common` 已统一 `ApiResponse<T>`、`PageQuery`、`PageResult<T>`。
- `exam-common` 已冻结最小共享审计模型 `AuditTrail`。
- 共享鉴权失败、请求头上下文无效、时间格式错误、题型/审核状态解析异常已统一纳入 `DomainErrorCode`。
- 共享请求头常量已冻结：
  - `Authorization`
  - `Request-Id`
  - `X-User-Id`
  - `X-Role-Id`
  - `X-Request-Id`
- 已冻结并落仓共享题库枚举：
  - `QuestionType`
  - `QuestionAuditStatus`
- 已补共享错误码，覆盖班级域、题库域、系统域的主链路冲突场景。

### 3. 网关与账号

- `exam-gateway` 已接入：
  - `/api/account/**`
  - `/api/class/**`
  - `/api/system/**`
  - `/api/question/**`
  - `/api/paper/**`
  - `/api/exam/core/**`
  - `/api/score/**`
  - `/api/resource/**`
  - `/api/issue/core/**`
- 网关已支持：
  - `Bearer <token>` 与裸 token 双兼容
  - `X-User-Id`、`X-Role-Id`、`X-Request-Id` 透传
  - 统一 `401` 结构化响应
  - 统一 `403` 结构化响应
- `exam-account` 已兼容共享响应模型与统一异常处理。

### 4. 业务域正式落地

- `exam-class` 已完成正式持久化替换，不再依赖内存 `Map` 主链路。
- `exam-class` 查询结果已补齐 `approvedMemberCount`、`pendingMemberCount` 统计字段。
- `exam-class` 导出结果已补齐 `approvedMemberCount`、`pendingMemberCount` 统计列。
- `exam-class` 导出结果已继续补齐 `description`、`createdBy`、`createTime`、`updateTime` 列，便于运维与审计核查。
- `exam-class` 已在原 `/approve-join` 接口上兼容批量审批能力。
- `exam-class` 导入已兼容标准导入头与班级导出表头，形成最小模板闭环。
- `exam-class` 已补导入模板样例与导出字段说明文档，导入回灌和导出核查口径已收口到统一接口文档。
- `exam-class` 已新增独立审计日志表 `exam_class_audit_log`，班级创建、更新、删除、申请入班、审批、移除、退班等关键写操作都会留痕。
- `exam-class` 已落表：
  - `exam_class`
  - `exam_class_member`
  - `exam_class_import_record`
  - `exam_class_audit_log`
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）已完成正式持久化替换，并切换到共享 `ApiResponse/PageResult`。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）已补齐分类创建人过滤和分类层级循环校验。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）导入结果已对齐 `totalCount`、`successCount`、`failedCount`、`rowErrors` 文档口径，并保留兼容字段。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）已启用分类禁用规则，禁用分类不可挂接子分类，也不可继续新增/导入题目。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）导入已兼容标准导入头与题目导出表头；被引用题目更新前置校验已扩展到题干、难度、答案、选项。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）查询已支持按“是否被引用”筛选，并返回 `referenceLocked` 供后续试卷能力域直接复用当前锁定语义。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）导出已补齐 `referenceCount` 列，继续保持导出模板可回灌导入。
- 题库能力域（历史拆分名：`exam-question`，现由 `exam-class` 承接）已落表：
  - `question_item`
  - `question_category`
  - `question_audit_log`
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）已完成角色、权限分配、配置、告警、日志、备份、审计等真实模型落地。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）已补齐日志时间区间过滤、审计员导出审批校验与导出审计留痕。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）已补齐备份记录查询的时间区间过滤。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）数据恢复已收紧为仅 `SUCCESS` 状态备份允许恢复，避免重复恢复。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）备份记录查询权限已对齐文档，仅超级管理员、审计员、运维可访问。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）备份类型已冻结为 `full/incremental/config/audit` 四类，拒绝非法类型入参。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）已补更细粒度审计明细，角色、权限、配置、告警操作均记录变更前后值。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）备份查询已补 `updateTime`、`canRestore`、`lifecycleStage` 字段，恢复链路会写入生命周期备注。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）已新增库内通知出站记录，配置变更与备份/恢复受理、终态都会写入 `system_notification_record`。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）备份与恢复已切换为“异步受理 + 后台执行 + 启动恢复回收”模型，状态机已冻结为 `BACKUP_PENDING/BACKUP_RUNNING/BACKUP_SUCCESS/BACKUP_FAILED/RESTORE_PENDING/RESTORE_RUNNING/RESTORE_SUCCESS/RESTORE_FAILED`。
- 系统治理能力域（历史拆分名：`exam-system`，现由 `exam-account` 承接）已落表：
  - `system_role`
  - `system_permission_assignment`
  - `system_config`
  - `system_alarm_setting`
  - `system_log`
  - `system_backup_record`
  - `system_notification_record`
  - `system_audit_record`
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已落地首批试卷接口：
  - `/api/paper/query`
  - `/api/paper/create/manual`
  - `/api/paper/create/auto`
  - `/api/paper/update`
  - `/api/paper/delete`
  - `/api/paper/audit`
  - `/api/paper/publish`
  - `/api/paper/terminate`
  - `/api/paper/recycle`
  - `/api/paper/export`
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已冻结试卷生命周期状态：`DRAFT/APPROVED/REJECTED/PUBLISHED/TERMINATED/RECYCLED`。
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已冻结首批业务口径：手动/自动组卷默认 `DRAFT`，自动组卷默认 `examTime=90`、`passScore=ceil(totalScore*0.6)`，`knowledgeRatio` 当前按 `categoryId` 字符串权重解释。
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已接入共享库直读模型，并在建卷、改卷、删卷时同步维护题目 `referenceCount`。
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已补试卷终止能力，当前仅允许终止已发布试卷，终止原因写入 `exam_paper_audit_log`。
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已补试卷导出能力，当前返回资源元数据，真实 CSV 写入共享本地资源目录，审计员导出需携带 `approverId`，导出结果附带操作者水印。
- 试卷能力域（历史拆分名：`exam-paper`，现由 `exam-class` 承接）已落表：
  - `exam_paper`
  - `exam_paper_question`
  - `exam_paper_publish_class`
  - `exam_paper_audit_log`
- `exam-core` 已完成基座创建，包含 Maven 模块、应用入口、基础配置、请求上下文解析、统一异常处理与最小启动测试。
- `exam-core` 已落地首批考试接口：
  - `/api/exam/core/create`
  - `/api/exam/core/query`
  - `/api/exam/core/update/params`
  - `/api/exam/core/distribute`
  - `/api/exam/core/submit`
  - `/api/exam/core/apply-retest`
  - `/api/exam/core/toggle-status`
  - `/api/exam/core/approve-retest`
- `exam-core` 已冻结考试最小状态基线：`DRAFT/PUBLISHED/UNDERWAY/PAUSED/ENDED/TERMINATED`。
- `exam-core` 已冻结首批创建口径：创建考试要求试卷为 `PUBLISHED`，班级必须在试卷已发布范围内，教师仅能使用自己创建的班级。
- `exam-core` 已补最小查询口径：教师仅查本人创建班级下的考试，学生当前按显式分发表查询本人可见考试，管理员/审计员可按条件只读检索。
- `exam-core` 已补最小参数调整口径：当前只调整 `startTime/duration`，未开考前可改二者，开考后仅允许延长 `duration`。
- `exam-core` 已补最小分发口径：显式分发到学生名单，学生查询改按分发表可见；当前仅允许分发到考试班级内且已审批通过的学生；分发成功后会同步初始化 `exam_score_record` 与 `exam_score_detail`，若该考试已有非 `PENDING` 成绩则拒绝重新分发。
- `exam-core` 已补最小提交口径：当前仅允许学生本人在已到开考时间且考试状态为 `PUBLISHED/UNDERWAY` 时提交；提交后回写 `exam_score_detail.student_answer`、`exam_score_record.submitted_at`，并同步完成客观题自动判分，按是否存在主观题推进到 `SCORING/SCORED`。
- `exam-core` 已补最小补考申请口径：当前仅允许学生本人在考试计划结束后、且本场尚未交卷时发起补考申请；申请直接落 `exam_instance_retest_apply`，继续复用现有审核链。
- `exam-core` 已补最小暂停恢复口径：当前仅支持 `PUBLISHED/UNDERWAY -> PAUSED` 与 `PAUSED -> PUBLISHED/UNDERWAY`，并把状态切换原因写入状态日志。
- `exam-core` 已补最小补考审核口径：当前仅审核已存在申请，教师按班级归属审核，驳回必须填写原因，并按 `exam_id + student_id` 冻结单场单次补考语义。
- `exam-core` 已落表：
  - `exam_instance`
  - `exam_instance_class`
  - `exam_instance_student`
  - `exam_instance_status_log`
  - `exam_instance_retest_apply`
- 成绩能力域（历史拆分名：`exam-score`，现由 `exam-core` 承接）已完成模块基座、统一异常处理与最小启动测试，并已完成冻结基线 10 个接口闭环。
- 成绩能力域（历史拆分名：`exam-score`，现由 `exam-core` 承接）已接通 `exam-core` 分发初始化、答卷提交与客观题自动判分联动，当前主链闭环已完成。
- 资源能力域（历史拆分名：`exam-resource`，现由 `exam-class` 承接）已完成模块基座、统一异常处理、共享本地资源目录分发和首批资源接口闭环。
- 问题申报能力域（历史拆分名：`exam-issue-core`，现由 `exam-core` 承接）已完成模块基座、网关路由、统一异常处理、问题主记录/轨迹记录模型与首批 6 个接口闭环。
- 问题申报能力域（历史拆分名：`exam-issue-core`，现由 `exam-core` 承接）已冻结问题类型 `BUSINESS/EXAM/SYSTEM`、状态 `PENDING/PROCESSING/CLOSED` 与轨迹动作 `CREATED/HANDLED/TRANSFERRED/CLOSED`。
- `exam-realtime` 已完成独立 Node.js 模块、Fastify + TypeScript 基座、Nacos 注册、网关路由、Redis 草稿缓存、Socket 连接、倒计时、自动保存、异常上报与超时触发交卷首批闭环。
- `exam-realtime` 已冻结“过程归 realtime，正式交卷落库仍走 core”的边界；`report-abnormal` 当前只写最小异常记录表，不扩展问题流程。
- `exam-realtime` 独立收口主文档已固定为 `docs/architecture/exam-realtime技术栈与首批落地方案.md`；后续若继续调整实时服务口径，优先更新该文档，再同步摘要到总表、接口、部署与前端文档。
- `exam-realtime` 当前已聚合承接 `realtime + issue-notify` 两类 Node 能力；问题通知 HTTP 继续走 `/api/issue/notify/**`，Socket 继续复用同一进程并改为 `path=/issue-socket.io`。
- 问题通知能力（历史拆分名：`exam-issue-notify`）目录与文档当前保留为历史实现记录，但运行拓扑、部署脚本和联调入口已统一收口为 `exam-realtime` 单进程。
- 问题申报能力域（历史拆分名：`exam-issue-core`，现由 `exam-core` 承接）已落表：
  - `exam_issue_record`
  - `exam_issue_process_log`
- `exam-realtime` 已落表：
  - `exam_realtime_abnormal_record`

### 5. SQL 与测试

- 多域表结构已统一写入 `apis/docs/sql/mysql.sql`。
- 已完成当前运行拓扑对应的模块级与根工程测试通过记录：
  - `./mvnw.cmd -pl exam-account,exam-class,exam-core -am test`
  - `./mvnw.cmd -pl exam-gateway -am test`
  - `./mvnw.cmd test`
  - `npm --prefix exam-realtime run test:run`

## 二、本轮主任务完成后的统一剩余待办

以下事项属于“本轮主任务完成后，仍建议继续推进但不需要新开微服务”的范围。

### 1. 网关横切继续收口

- 继续稳定 `X-User-Id`、`X-Role-Id`、`X-Request-Id` 透传口径。
- 如后续需要链路追踪字段或统一审计透传，再在网关集中补，不在业务服务各自扩散。

### 2. 文档继续补齐

- 持续更新总接口文档，保证 `class/question/system` 等能力域与仓库实际实现一致。
- 后续如试卷、成绩、问题申报、资源等能力域继续演进，继续在统一接口文档中补齐，不再拆回线程文档。
- 架构文档优先维护本总表，原线程文档只保留历史背景。

### 3. 共享层继续回收

- 继续审核新增错误码，避免业务服务再次本地分叉。
- 保持分页、响应体、枚举、请求头常量的单一来源。

### 4. 业务规则可继续增强但不阻塞当前基线

- 题库能力域（历史拆分名：`exam-question`）：引用关系与后续试卷能力域真实联动维护。
- 系统治理能力域（历史拆分名：`exam-system`）：真实消息投递通道、分布式任务调度和更细粒度失败补偿继续细化。
- `exam-core`：进入考试、自动保存、超时提交、倒计时、异常上报统一转交 `exam-realtime`。
- 成绩能力域（历史拆分名：`exam-score`）：进一步过程联动深化和跨服务深联调保留为增强项。
- 资源能力域（历史拆分名：`exam-resource`）：真实文件登记、对象存储和资源治理保留为增强项。

## 三、当前不需要新开微服务也能继续做的事

- 继续补 API 文档、SQL 注释、测试用例、异常码覆盖。
- 继续把横切能力沉到 `exam-common` 和 `exam-gateway`。
- 继续补 `class/question/system` 三域的边界规则和导入导出细节。
- 继续完善现有已落地服务的稳定性、可观测性和联调口径。

## 四、当前运行服务与能力域边界

### 1. 当前运行服务

- `exam-gateway`
- `exam-account`
  - 当前为独立运行服务，承接 `account + system`。
  - 当前已完成账户主链与系统治理能力域的聚合承接。
- `exam-class`
  - 当前为独立运行服务，承接 `class + question + paper + resource`。
  - 当前已完成班级主链、题库能力域、试卷能力域和资源能力域的聚合承接。
- `exam-core`
  - 当前为独立运行服务，承接 `core + score + issue-core`。
  - 当前已完成考试主链闭环，并聚合承接成绩能力域与问题申报能力域。
- `exam-realtime`
  - 当前为独立运行服务，承接 `realtime + issue-notify`。
  - 当前已完成实时考试能力与问题通知能力的单进程收口。

### 2. 历史拆分与能力域记录

- `exam-system`：历史拆分名，当前作为系统治理能力域由 `exam-account` 承接。
- `exam-question`：历史拆分名，当前作为题库能力域由 `exam-class` 承接。
- `exam-paper`：历史拆分名，当前作为试卷能力域由 `exam-class` 承接。
- `exam-resource`：历史拆分名，当前作为资源能力域由 `exam-class` 承接。
- `exam-score`：历史拆分名，当前作为成绩能力域由 `exam-core` 承接。
- `exam-issue-core`：历史拆分名，当前作为问题申报能力域由 `exam-core` 承接。
- `exam-issue-notify`：历史拆分名，当前作为问题通知能力由 `exam-realtime` 承接。

### 3. 当前结论

- A / B / C 主链路已经基本收口，当前运行拓扑统一固定为 5 个服务，不再为了现有能力补尾项而恢复旧的独立微服务拆分。
- 试卷、成绩、资源、问题申报、问题通知等仍可继续推进，但统一按“能力域演进 + 宿主服务承接”方式描述和实施。
- 后续优先级、冻结标准和测试冻结基线，统一以 `apis/docs/architecture/未来微服务推进优先级与冻结标准.md` 为准。
- 如果下一阶段继续做“试卷 / 考试 / 成绩 / 申报 / 资源 / 实时交互”，直接按该独立文档中的能力域优先级执行更合适。
## 五、推进建议

- 以本总表作为当前单一待办入口继续推进。
- 不再把共享层、网关层、业务层任务拆散到三个线程文档里重复维护。
- 后续若有新增能力域落地或需要重新拆分服务，再在本文档直接追加“已完成 / 待办 / 依赖 / 联调口径”。

## 六、当前仍未落地但不阻塞本轮的事项

- 题库能力域（历史拆分名：`exam-question`）与试卷能力域（历史拆分名：`exam-paper`）的题目引用计数已完成真实维护；后续若继续深化，仅剩更细粒度引用审计或再次拆分后的同步策略。
- 系统治理能力域（历史拆分名：`exam-system`）已落本地 outbox 通知与服务内异步状态机，但尚未扩展到真实消息投递和分布式任务调度。
- `exam-class` 已补独立审计日志表；后续若继续深化，仅剩审计查询接口或更细粒度报表，不再属于当前收口阻塞项。
- 试卷能力域（历史拆分名：`exam-paper`）已完成查询、组卷、审核、发布、终止、回收、导出闭环；当前已通过 `exam-core` 分发与提交链路接通最小成绩初始化、答案回写与客观题自动判分，但更完整的考试过程联动仍未开始。
- `exam-core` 首批接口已全部落地，并已完成与 `exam-score` 的最小分发初始化联调、提交答卷联调、客观题自动判分联调和补考申请/审核闭环；自动保存、超时提交、倒计时和考试异常上报统一转交 `exam-realtime`。
- 成绩能力域（历史拆分名：`exam-score`）已完成冻结基线 10 个接口闭环，并已接入 `exam-core` 分发初始化、答卷提交与客观题自动判分；后续仅剩考试过程联动深化与和试卷能力域的更完整联调，不再作为当前收口阻塞项。
- 资源能力域（历史拆分名：`exam-resource`）已完成首批资源接口闭环，并已接入本地共享导出回写链路；后续仍可继续补真实文件登记、对象存储和更完整的资源治理。
- 问题申报能力域（历史拆分名：`exam-issue-core`）已完成首批问题申报主流程闭环；后续实时通知由 `exam-realtime` 内聚的问题通知能力承接，不再继续以独立服务口径扩展。
- `exam-realtime` 已完成首批实时考试闭环；后续仍可继续补多实例连接协调、教师监考推送与更细粒度实时策略。
- 问题通知能力（历史拆分名：`exam-issue-notify`）已并入 `exam-realtime`；后续仍可继续补已读状态、未读计数、多实例去重和消息驱动链路。
