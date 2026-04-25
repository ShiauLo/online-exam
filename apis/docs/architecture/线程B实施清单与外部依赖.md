# 线程 B 实施清单与外部依赖

> 当前统一执行总表已合并至 `A-B-C一体化实施与后续待办总表.md`，本文保留为历史分工记录。

## 文档版本

- 版本：V1.1
- 修订时间：2026-04-03
- 修订人：线程 B

## 文档定位

- 设计目标：汇总线程 B 当前已完成事项、线程 B 后续仍需继续实现的内容，以及需要线程 A、线程 C 配合落地的事项，作为后续并行推进的执行清单。
- 适用范围：`exam-class`、`exam-system`、共享基线线程、题库线程之间的协作与跟进。
- 当前边界：本文档面向“实施状态与责任拆分”，不替代接口文档、角色文档与线程间口径冻结文档。

## 一、线程 B 已完成事项

### 1. 共享口径接入

已按线程 A 冻结口径在共享层补入并接入以下基础设施：

- 统一响应体：`ApiResponse`
- 请求头常量：
  - `X-User-Id`
  - `X-Role-Id`
  - `X-Request-Id`
  - `Request-Id`
- 角色映射工具：
  - `roleId -> 内部大写角色编码`
  - `roleId -> 对外小写角色编码`

### 2. 工程结构接入

已完成以下工程接入：

- 根工程新增模块：
  - `exam-class`
  - `exam-system`
- 两个模块均已具备：
  - Spring Boot 启动类
  - Maven 依赖
  - Nacos / DataSource 基础配置
  - MyBatis 自动填充处理器
  - ID 生成配置

### 3. `exam-class` 第一批落地内容

已完成：

- 请求上下文解析
- 统一异常处理
- 班级域 DTO / VO / 内存态实体模型
- `ClassController`
- `ClassService`
- `ClassServiceImpl`

已覆盖的接口骨架：

- `/api/class/create`
- `/api/class/query`
- `/api/class/update`
- `/api/class/delete`
- `/api/class/apply-join`
- `/api/class/approve-join`
- `/api/class/remove-student`
- `/api/class/quit`
- `/api/class/import`
- `/api/class/export`

已接入的首批业务规则：

- 教师默认仅可创建 1 个专属班级
- 学生最多加入 3 个班级
- 强制班级不可自主退班
- 教师仅可操作自己创建的班级
- 删除班级前必须无已加入学生

### 4. `exam-system` 第一批落地内容

已完成：

- 请求上下文解析
- 统一异常处理
- 系统管理 DTO / VO
- `SystemController`
- `SystemService`
- `SystemServiceImpl`

已覆盖的接口骨架：

- `/api/system/permission/query`
- `/api/system/role`
- `/api/system/permission/assign`
- `/api/system/config/query`
- `/api/system/config/update`
- `/api/system/alarm/query`
- `/api/system/alarm/setting`
- `/api/system/log/query`
- `/api/system/log/export`
- `/api/system/data/query`
- `/api/system/data/backup`
- `/api/system/data/restore`

当前已具备真实业务口径的部分：

- `permission/query` 已按角色输出菜单、路由、按钮权限模板
- 题库相关权限模板已按线程 B 与线程 C 对齐口径预留
- 管理员 / 超级管理员 / 教师 / 学生 / 审计员 / 运维的基础权限视图已初步可用

### 5. 验证状态

已完成以下验证：

- `exam-common`
- `exam-class`
- `exam-system`

通过命令：

```powershell
./mvnw.cmd -pl exam-class,exam-system -am test
```

当前验证结果：

- 构建通过
- 最小测试通过
- 新模块可参与聚合工程构建

### 6. 第二轮落地结果

- `exam-class` 已从内存态切换为正式持久化仓储实现
- `exam-class` 已补入：
  - `exam_class`
  - `exam_class_member`
  - `exam_class_import_record`
- `exam-class` 已补齐建班上限、入班上限、强制退班、越权、删除前成员校验、导入导出测试
- `exam-system` 已补入角色、权限分配、配置、告警、日志、备份、审计等真实数据模型
- `exam-system` 非 `permission/query` 接口已不再只是空列表或占位返回
- 根工程 `./mvnw.cmd test` 已通过

## 二、线程 B 仍需继续实现的事项

以下内容仍由线程 B 继续负责，不应转交其他线程：

### 1. `exam-class` 持久化替换

当前 `exam-class` 仍以首版内存态实现承接业务规则，后续需继续替换为正式持久化实现：

- 落地真实表结构映射
- 新增 Mapper
- 将内存 Map 替换为数据库读写
- 补齐导入记录模型与持久化
- 补齐导出数据来源

### 2. `exam-class` 测试扩充

当前仅有最小异常返回测试，后续需补齐：

- 建班数量上限测试
- 入班数量上限测试
- 强制班级退班冲突测试
- 教师越权操作测试
- 删除班级前成员未清空测试
- 导入导出基础测试

### 3. `exam-system` 真实数据模型落地

当前 `exam-system` 中除权限查询外，其余接口大多还是“稳定骨架 + 占位返回”，后续需补齐：

- 角色模型
- 权限分配模型
- 系统配置模型
- 告警配置模型
- 系统日志模型
- 备份记录模型
- 审计记录模型

### 4. `exam-system` 接口逻辑补全

线程 B 仍需继续补齐以下接口的真实逻辑：

- 角色创建 / 修改
- 权限分配
- 系统配置查询 / 修改
- 告警配置查询 / 修改
- 系统日志查询 / 导出
- 备份记录查询
- 数据备份
- 数据恢复

### 5. `exam-system` 测试扩充

后续需补齐：

- 普通管理员越权分配测试
- 审计员只读测试
- 配置修改二次验证测试
- 日志导出权限测试
- 备份 / 恢复权限测试
- 各角色 `permission/query` 模板测试

## 三、需要线程 A 实现或落仓的事项

以下事项口径虽已冻结，但实现仍应由线程 A 负责统一落仓，线程 B 不应自行发散成长期公共标准。

### 1. `exam-common` 统一沉淀

线程 A 仍需统一落仓：

- 公共响应类型正式收敛
- 公共分页对象与校验模板
- 公共异常处理模板
- 公共鉴权头常量与 requestId 工具
- `class/system` 领域的共享错误码与异常类型

### 2. `exam-gateway` 接入与透传增强

线程 A 仍需实现：

- `/api/class/** -> lb://exam-class`
- `/api/system/** -> lb://exam-system`
- 透传 `X-Role-Id`
- 透传 `X-Request-Id`
- 网关统一 `401 / 403` 包装逻辑

### 3. 共享异常与错误码收敛

线程 A 仍需把以下场景收敛为正式共享异常 / 错误码：

- 班级不存在
- 班级码无效
- 建班数量超限
- 入班数量超限
- 强制班级不可退班
- 删除班级前仍有学生
- 审批状态冲突
- 权限分配越权
- 配置项不存在
- 二次验证失败
- 备份 / 恢复状态冲突

### 4. 共享契约的最终落仓要求

线程 B 当前实现以线程 A 冻结文档为准，但线程 A 后续若正式落仓共享能力，线程 B 将按线程 A 最终实现回收适配。

## 四、需要线程 C 实现或继续推进的事项

以下事项由线程 C 继续负责，线程 B 当前已做权限模板预留，但不代替题库线程的正式落地。

### 1. `exam-question` 模块正式实现

线程 C 仍需完成：

- 题目新增
- 题目编辑
- 题目删除
- 题目禁用 / 启用
- 分类管理
- 查询接口
- 导入
- 导出
- 审核

### 2. 题库权限编码与页面实现对齐

线程 C 需按已协同口径继续实现：

- `question.query`
- `question.create`
- `question.update`
- `question.delete`
- `question.toggleStatus`
- `question.category.manage`
- `question.import`
- `question.export`
- `question.audit`

### 3. 路由与前端页面落地

线程 C 仍需与前端继续对齐：

- 教师端：`/teacher/question`
- 管理员端：`/admin/question-audit`

### 4. 线程 C 完成后需回传线程 B 的事项

线程 C 后续若对以下内容有正式冻结，应同步回传线程 B：

- 题库最终权限码
- 题库最终菜单标识
- 题库最终路由与组件标识
- 审核流细节变更

## 五、当前线程分工建议

为避免重复施工，当前建议按如下分工继续推进：

### 线程 B 继续做

- `exam-class` 持久化与测试补齐
- `exam-system` 数据模型与真实逻辑补齐
- `exam-system /permission/query` 后续模板维护
- 对接线程 A / C 回传后的兼容调整

### 线程 A 继续做

- 公共契约正式落仓
- 网关路由与 Header 透传增强
- 公共异常与错误码沉淀
- 统一 `401 / 403` 结构化响应

### 线程 C 继续做

- `exam-question` 业务实现
- 题库权限码与页面口径正式落地
- 与线程 B 的权限模板收敛

## 六、备注

- 当前线程 B 已经正式开工，不再处于等待线程 A 回执阶段。
- 线程 A 的回执已经足以支撑线程 B 继续实现，但不等于共享层代码已全部落仓。
- 线程 B 当前代码中存在“过渡适配层”性质的实现，后续如线程 A 正式下沉公共能力，应优先回收而不是继续扩散。
- 本文档应与以下文档配套阅读：
  - `线程A冻结口径回执-线程B.md`
  - `线程B回复线程C-题库权限与协作口径.md`
  - `角色文档.md`

## 文档修订记录

| 版本 | 修订时间 | 修订内容 | 修订人 |
| --- | --- | --- | --- |
| V1.1 | 2026-04-03 | 补充 `exam-class` 持久化、`exam-system` 真实模型与全量测试通过状态 | 线程 B |
| V1.0 | 2026-04-03 | 首次整理线程 B 当前实施状态、后续清单和外部线程依赖 | 线程 B |
