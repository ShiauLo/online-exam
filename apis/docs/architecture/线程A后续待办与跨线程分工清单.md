# 线程A后续待办与跨线程分工清单

> 当前统一执行总表已合并至 `A-B-C一体化实施与后续待办总表.md`，本文保留为历史分工记录。

## 文档版本

- 版本：V1.1
- 更新时间：2026-04-03
- 维护人：线程 A

## 文档定位

用于记录线程 A 本轮完成共享契约冻结后的后续工作边界，明确：

- 哪些事项仍由线程 A 继续收口
- 哪些事项需要线程 B / 线程 C 各自实现
- 哪些事项需要跨线程联调时再共同处理

本清单以当前仓库已落地代码为准；若后续实现与本文冲突，以线程 A 新一轮冻结文档为准。

## 一、当前已完成事项

### 1. 共享基线已落地

- `exam-common` 已提供统一响应 `ApiResponse<T>`
- 已冻结公共状态码 `200 / 400 / 401 / 403 / 404 / 409 / 500`
- 已冻结鉴权与透传常量：
  - `Authorization`
  - `Request-Id`
  - `X-User-Id`
  - `X-Role-Id`
  - `X-Request-Id`
- 已冻结 JWT claims：
  - `userId`
  - `roleId`
- 已提供 `RequestIdUtils`
- 已提供分页基础类型 `PageQuery`
- 已补首批 `class/system` 域共享错误码 `DomainErrorCode`

### 2. 网关基线已落地

- `exam-gateway` 已统一 `401` 结构化响应
- 已支持 `Bearer <token>` 与裸 token 双兼容
- 已透传 `X-User-Id`、`X-Role-Id`、`X-Request-Id`
- 已保留原始 `Authorization` 头透传
- 已新增路由：
  - `/api/class/** -> lb://exam-class`
  - `/api/system/** -> lb://exam-system`
  - `/api/question/** -> lb://exam-question`

### 3. 账号兼容已完成

- `exam-account` 已切换到 `exam-common` 的统一响应模型
- 全局异常处理已切到统一口径
- `refresh token` 后的 `access token` 已保留 `roleId`

### 4. 工程与测试基线已确认

- 根工程已纳入：
  - `exam-class`
  - `exam-system`
  - `exam-question`
- 根工程 `./mvnw.cmd test` 已通过

### 5. 第二轮共享收口已完成

- `exam-common` 已补充通用分页返回对象 `PageResult<T>`
- `exam-common` 已下沉题库共享枚举 `QuestionType`、`QuestionAuditStatus`
- `exam-common` 已补充题库域与系统域共享错误码
- `exam-question` 已切换到 `ApiResponse<T>`，不再保留本地 `Result/PageResult`
- `apis/docs/sql/mysql.sql` 已补入 `class/question/system` 三域正式表结构

## 二、线程A后续还需要做的事

以下事项仍属于线程 A 的持续职责，但不阻塞 B / C 当前继续开发。

### 1. 继续沉淀共享层

- 视 B / C 实现反馈，将真正跨服务复用的 DTO / 枚举继续回收至 `exam-common`
- 如果 `class/system/question` 后续出现稳定的审计对象模型，可再统一沉淀到 `exam-common`
- 如后续出现统一分页返回模型需求，可补充通用分页响应对象

### 2. 继续维护共享错误码池

- 审核线程 B / C 新增的业务错误码是否属于“公共层应统一管理”的范围
- 对已经稳定的跨服务业务错误码继续编目，避免各服务自行发散

### 3. 继续维护网关横切能力

- 如后续需要统一 `403` 网关包装，线程 A 负责收口实现
- 如后续需要补充请求链路日志、统一 trace 扩展字段，也由线程 A 统一推进
- 若鉴权白名单策略发生变更，统一由线程 A 调整，不由业务线程各自修改

### 4. 继续维护公共测试基线

- 维护 `exam-common` 中的 API 契约测试生成能力
- 当新字段类型进入公共契约测试生成器无法识别时，由线程 A 统一补齐解析规则

## 三、需要线程B实现的东西

线程 B 负责 `exam-class` 与 `exam-system` 的业务落地，不由线程 A 代做。

### 1. `exam-class` 需要线程B实现

- 班级域表结构、实体、DTO、VO、Service、Controller
- 班级创建、查询、更新、删除、加入、退出、审批等业务接口
- 教师/学生在班级域内的数据范围控制
- 审批流与状态流转实现
- 审计字段实际落库或留痕实现
- 班级域业务测试

### 2. `exam-system` 需要线程B实现

- 角色、权限、配置、日志、备份恢复等系统域模块
- 权限查询、角色授权、越权拦截等系统域业务逻辑
- 菜单、路由、按钮权限模板输出
- 题库相关权限模板预置：
  - `question.create`
  - `question.update`
  - `question.delete`
  - `question.toggleStatus`
  - `question.category.manage`
  - `question.import`
  - `question.export`
  - `question.audit`
  - `question.query`
- 系统域业务测试

### 3. 线程B实现时必须遵守的口径

- 不再自定义本地 `Result`
- 不新增自定义鉴权头命名
- 默认从 `X-User-Id`、`X-Role-Id`、`X-Request-Id` 读取上下文
- 默认不重复做 token 合法性校验
- 分页字段统一使用 `pageNum`、`pageSize`

## 四、需要线程C实现的东西

线程 C 负责 `exam-question` 的业务落地与题库域收口，不由线程 A 代做。

### 1. `exam-question` 需要线程C实现

- 题目、分类、审核、导入导出、查询等业务接口
- 首版 `/api/question/query` 的完整实现
- 教师 / 管理员 / 审计员在题库域的数据范围控制
- 导出脱敏逻辑
- 审核流与状态流转逻辑
- 题库域业务测试

### 2. 线程C后续建议继续收口的项

- 将当前仍在本地的题库响应包装逐步向 `exam-common` 靠拢
- 当题型枚举、审核状态枚举、题库错误码稳定后，提交给线程 A 评估是否下沉到 `exam-common`
- 逐步减少对本地解析 `Authorization` 的依赖，优先使用网关透传上下文

### 3. 线程C实现时必须遵守的口径

- 权限码统一按 `question.*` 小写点分风格
- 教师默认只管理本人题目与个人分类
- 管理员默认管理全局题目、分类与审核
- 审计员默认只读，导出需脱敏
- 菜单 / 路由口径固定：
  - `question -> /teacher/question`
  - `question-audit -> /admin/question-audit`

## 五、需要B / C分别回收给线程A的事项

以下不是线程 B / C 可以自行冻结的内容，出现后应回收给线程 A 统一收口。

### 1. 需要回收给线程A的共享模型

- 明确被多个服务复用的 DTO / VO
- 需要多个服务共同使用的枚举
- 稳定的公共分页返回模型
- 稳定的审计日志模型

### 2. 需要回收给线程A的共享错误码

- 不只属于单个业务域的错误码
- 将来可能在网关、系统权限、题库审核、班级审批之间复用的错误码

### 3. 需要回收给线程A的网关与鉴权需求

- 新增统一白名单诉求
- 新增统一透传头诉求
- 新增统一网关错误响应诉求
- 需要调整 token 语义或 claim 的诉求

## 六、联调触发条件

出现以下情况时，建议立即发起线程间联调，而不是各自继续闷头实现。

- 线程 B 发现 `exam-common` 中共享错误码不足以覆盖系统域/班级域稳定场景
- 线程 C 发现题库域已有稳定公共枚举或错误码需要下沉
- 线程 B / C 发现当前 `X-User-Id`、`X-Role-Id`、`X-Request-Id` 无法满足上下文需求
- 线程 B / C 发现接口文档、前端设计文档与当前冻结口径冲突

## 七、推进建议

### 1. 线程A下一步建议

- 以“被 B / C 实际复用”为标准继续下沉共享模型
- 不提前预埋未经业务线程验证的新公共类型

### 2. 线程B下一步建议

- 优先把 `exam-class`、`exam-system` 的 Controller / Service / Exception / 测试骨架补齐
- 先按现有冻结口径落业务，再把稳定新增项回收给线程 A

### 3. 线程C下一步建议

- 优先完成 `/api/question/query`、审核流、分类与导入导出主链路
- 先保证响应结构和上下文读取方式不偏离冻结口径

## 修订记录

| 版本 | 时间 | 内容 | 维护人 |
| --- | --- | --- | --- |
| V1.1 | 2026-04-03 | 补充共享分页、题库共享枚举、题库响应收敛与三域 SQL 落仓状态 | 线程 A |
| V1.0 | 2026-04-03 | 首次整理线程 A 已完成事项、后续待办与 B/C 分工边界 | 线程 A |
