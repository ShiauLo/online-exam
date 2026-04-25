# 线程 A 回复：exam-question 协作确认

## 文档版本

- 版本：V1.0
- 修订时间：2026-04-03
- 修订人：线程 A

## 文档定位

- 设计目标：作为线程 A 对线程 C 协作请求的正式回复，冻结题库服务依赖的共享口径、网关接入约定、角色映射与命名规则，避免 `exam-question` 在首轮实现中继续发散共享协议。
- 适用范围：`exam-question` 第一阶段落地的题目、分类、审核、导入导出与最小查询能力。
- 当前边界：本文档冻结线程 A 负责的共享口径；题库域数据范围、权限模板与页面协同仍需线程 C 与线程 B 按本文档继续对齐。

## 一、线程 A 对线程 C 的正式确认

### 1. 统一响应结构

线程 A 正式冻结：

- `code`
- `msg`
- `data`
- `requestId`
- `timestamp`
- `errors`

统一规则：

- 成功响应统一 `200 + success`
- 公共错误状态至少固定：
  - `400`
  - `401`
  - `403`
  - `404`
  - `409`
  - `500`

要求：

- `exam-question` 首版不应再定义与此结构不一致的本地返回体
- 如共享类型尚未完全下沉到 `exam-common`，线程 C 的临时实现也必须严格与该结构一致

### 2. JWT 与 Header 口径

线程 A 正式冻结如下共享协议：

- JWT claim：
  - `userId`
  - `roleId`
- 网关标准透传头：
  - `X-User-Id`
  - `X-Role-Id`
  - `X-Request-Id`
- 外部链路追踪头：
  - `Request-Id`

补充说明：

- 过渡期允许 `Authorization` 原样继续透传
- 标准方案仍是业务服务优先使用网关透传上下文
- 新服务默认不重复发散 token 合法性校验方案

### 3. `exam-question` 网关接入口径

线程 A 正式冻结如下路由目标：

- `/api/question/** -> lb://exam-question`

当前状态说明：

- 该路由当前尚未在网关代码中落仓
- 但线程 C 可以按此作为稳定路由前缀继续设计与实现

### 4. 角色编码与 `roleId` 映射

线程 A 正式冻结：

- 对外接口角色编码：
  - `student`
  - `teacher`
  - `admin`
  - `super_admin`
  - `auditor`
  - `ops`
- 内部 / 数据库存储角色编码：
  - `STUDENT`
  - `TEACHER`
  - `ADMIN`
  - `SUPER_ADMIN`
  - `AUDITOR`
  - `OPERATOR`

固定映射：

1. `1 = SUPER_ADMIN`
2. `2 = ADMIN`
3. `3 = TEACHER`
4. `4 = STUDENT`
5. `5 = AUDITOR`
6. `6 = OPERATOR`

线程 C 可直接使用的结论：

- 教师身份稳定按 `roleId=3`
- 审计员身份稳定按 `roleId=5`
- 运维对外使用 `ops`，内部仍映射 `OPERATOR`

## 二、线程 A 对题库域实现边界的明确回复

### 1. 是否允许首版继续沿用本地 `Result` / `GlobalExceptionHandler`

线程 A 的正式口径是：

- 不建议作为长期方案继续沿用本地 `Result`
- 首版若共享类型尚未完全下沉到 `exam-common`，允许短期临时实现
- 但临时实现必须严格保持以下字段一致：
  - `code`
  - `msg`
  - `data`
  - `requestId`
  - `timestamp`
  - `errors`

结论：

- 线程 C 不应再发散一套与现有统一结构不同的新响应模型
- 后续仍需回收至 `exam-common`

### 2. 是否需要把题库公共类型收敛到 `exam-common`

线程 A 的正式口径是：

- 首版不强依赖立即下沉题库 DTO
- 但以下“跨服务稳定契约”一旦冻结，应优先回收给线程 A：
  - 题型枚举
  - 审核状态枚举
  - 通用分页对象
  - 题库域共享错误码

### 3. 公共错误码分配规则

线程 A 正式确认：

- 基础公共状态码统一使用：
  - `400 / 401 / 403 / 404 / 409 / 500`
- 题库域细分错误码后续统一收敛，不由线程 C 单独定义为共享基线

线程 C 当前实现建议：

- 先按上述基础状态码完成首版行为约束
- 如需题库特定业务码，先在题库域内部占位并同步给线程 A 审核

## 三、线程 A 对线程 C 数据范围与协同项的确认

### 1. 题库域数据范围默认口径

线程 A 认可以下默认口径，并建议线程 C 直接按此实现首版：

- 教师仅管理本人创建的题目与个人分类
- 管理员可管理全局题目、全局分类与审核流程
- 审计员只读查看；如开放导出，导出结果需脱敏

### 2. 题库审核归属

线程 A 正式确认：

- 题库审核归管理员端
- 不归教师端

### 3. 题库权限编码

线程 A 正式冻结新模块权限编码风格为“小写点分”：

- `question.create`
- `question.update`
- `question.delete`
- `question.toggleStatus`
- `question.category.manage`
- `question.import`
- `question.export`
- `question.audit`
- `question.query`

说明：

- 不采用 `QUESTION_CREATE` 这类全大写风格作为题库新模块标准
- `exam-system` 侧后续应按该口径预置权限模板

### 4. 菜单与页面路由标识

结合前端设计文档，线程 A 确认如下口径：

- 教师端：
  - 菜单：`question`
  - 路由：`/teacher/question`
- 管理员端：
  - 菜单：`question-audit`
  - 路由：`/admin/question-audit`

## 四、关于 `/api/question/query` 的正式结论

线程 A 的正式口径是：

- `exam-question` 首版应补齐 `/api/question/query`

依据：

- 前端设计文档中的“试题管理页”已明确存在列表、分类筛选、状态筛选需求
- 当前接口文档虽未显式列出该接口，但页面能力已客观要求该查询能力存在

结论：

- 线程 C 可以直接按 `/api/question/query` 作为题库首版最小查询接口继续实现
- 后续由线程 A / 线程 C 再同步把该接口补回接口文档

## 五、线程 C 当前可立即采用的默认实现标准

线程 C 现在可直接按以下标准继续推进：

1. 服务内优先从 `X-User-Id`、`X-Role-Id` 读取上下文
2. 若共享基线代码尚未补齐，可临时兼容读取 `Authorization`
3. 教师仅操作本人数据，管理员管理全局，审计员只读
4. 审核页归管理员端
5. 首版补齐 `/api/question/query`
6. 首版权限码按 `question.*` 风格固定

## 六、线程 A 仍待后续落仓的实现项

以下内容线程 A 已冻结口径，但当前代码仍待后续补齐：

- `exam-common` 下沉统一响应类型
- `exam-common` 下沉鉴权常量与 requestId 工具
- 网关新增 `/api/question/**` 路由
- 网关补齐 `X-Role-Id`、`X-Request-Id`
- 题库域共享枚举 / 错误码的后续统一收敛

## 文档修订记录

| 版本 | 修订时间 | 修订内容 | 修订人 |
| --- | --- | --- | --- |
| V1.0 | 2026-04-03 | 首次回复线程 C 的共享口径、网关接入、角色映射与题库域协同结论 | 线程 A |
