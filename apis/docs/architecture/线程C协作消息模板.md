# 线程 C 协作消息模板

## 发给线程 A

````md
# 线程 C 协作请求：exam-question 依赖确认

我这边负责 `exam-question`，已经按 `apis/docs` 梳理出题库服务第一阶段范围。为了避免线程间协议漂移，想请你这边先帮我冻结下面这些共享基线项：

## 需要你确认/处理
- [ ] 冻结统一响应结构：`code / msg / data / requestId / timestamp / errors`
- [ ] 确认网关透传约定，至少保证 `Authorization` 原样透传；如计划统一透传用户信息，请明确 Header 名称
- [ ] 确认 JWT 中 `userId`、`roleId` claim 口径后续不再变更
- [ ] 在网关增加 `exam-question` 路由：`/api/question/** -> lb://exam-question`
- [ ] 确认角色编码与 `roleId` 映射口径
- [ ] 确认 `exam-question` 首版是否允许继续沿用本地 `Result` / `GlobalExceptionHandler`
- [ ] 如题型、审核状态、分页对象需要共享，请统一收敛到 `exam-common`
- [ ] 确认公共错误码分配规则，至少覆盖 400/401/403/404/409/500

## 我这边最依赖的阻塞项
1. JWT / Header / 网关路由口径
2. 统一错误响应口径
3. 角色映射是否冻结

## 我当前默认假设
- 服务内可直接通过 `Authorization` + `JwtUtils` 解析 `userId` / `roleId`
- `exam-question` 首版可先复用现有服务风格的本地 `Result` 和异常处理
- 网关会新增 `/api/question/**` 路由

如果其中任何一项不成立，请你直接回我最终口径，我会按你的口径调整线程 C 方案。
````

## 发给线程 B

````md
# 线程 C 协作请求：exam-question 权限与数据范围确认

我这边负责 `exam-question`，准备按第一阶段先落题目、分类、审核、导入导出和最小查询能力。为了保证后续和 `class/system` 一致，想请你这边先帮我确认题库域的权限和数据范围口径。

## 需要你确认
- [ ] 教师“仅操作本人创建数据”的统一判断口径
- [ ] 管理员在题库域是否默认可看/可管全部题目与分类
- [ ] 审计员在题库域是否仅只读查看，并且导出必须脱敏
- [ ] 教师身份是否稳定按 `roleId=3` 判断
- [ ] 教师端题库页面是否确认需要列表查询能力
- [ ] 后台题库审核是否明确归管理员端，不归教师端
- [ ] 是否需要统一权限编码，若需要请确认以下编码是否可用：
  - `QUESTION_CREATE`
  - `QUESTION_UPDATE`
  - `QUESTION_DELETE`
  - `QUESTION_TOGGLE_STATUS`
  - `QUESTION_CATEGORY_MANAGE`
  - `QUESTION_IMPORT`
  - `QUESTION_EXPORT`
  - `QUESTION_AUDIT`
  - `QUESTION_QUERY`

## 我这边最依赖的阻塞项
1. 教师 / 管理员 / 审计员三类角色在题库域的数据范围
2. 题库审核归属谁
3. 前端是否确认需要 `/api/question/query`

## 我当前默认假设
- 教师仅管理本人题目与个人分类
- 管理员可管理全局题目、全局分类和审核流程
- 审计员只读，导出结果脱敏
- 教师端“试题管理页”实际需要查询接口，因此我会补齐 `/api/question/query`

如果你的线程已经有更统一的权限模型，请直接给我最终口径，我按你那边收敛。
````

## 合并版

````md
# 线程 C 协作依赖同步：exam-question

我这边负责 `exam-question`，第一阶段准备落：
- 题目
- 分类
- 审核
- 导入导出
- 最小查询能力

当前最需要两边帮我冻结的是：

## 给线程 A
- [ ] 统一响应结构
- [ ] JWT claim：`userId` / `roleId`
- [ ] 网关透传约定
- [ ] `/api/question/** -> lb://exam-question` 路由
- [ ] 公共错误码口径
- [ ] 是否需要把题库公共类型收敛到 `exam-common`

## 给线程 B
- [ ] 教师 / 管理员 / 审计员在题库域的数据范围
- [ ] 题库审核归属管理员端
- [ ] 教师端是否确认需要题库列表查询
- [ ] 题库权限编码是否统一分配

## 线程 C 当前默认假设
- 服务内直接解析 token 获取 `userId` / `roleId`
- 教师仅管理本人题目，管理员管理全局，审计员只读
- 首版补齐 `/api/question/query`
- 首版先不强依赖 `exam-common` 新增题库 DTO

请 A/B 直接回最终口径，我会据此锁定 `exam-question` 实施方案。
````
