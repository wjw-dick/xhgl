# tasks.md / checklist.md 落地映射（单企业版）

> 代码路径以根目录为准；本表说明任务在当前工程中的实际落点与本地验收证据。

## 阶段 1：基础与数据库

| 任务 | 落点 | 本地验证 |
|---|---|---|
| T01 工程/企业数据域 | `backend/pom.xml`、`BoilerApplication`、`TenantContext`、`JwtService` | 编译、启动、登录 |
| T02 全局字典/企业字典 | `schema.sql`、`TenantSettingsController`、`DictsView` | 字典接口/页面 |
| T03 企业数据域插件 | `schema.sql`、`MybatisPlusConfig` | 插件注入、接口响应隐藏字段 |
| T04 客户档案 | `Customer`/`CustomerAdminController`/`CustomersView`/`CustomerDetailView` | 列表/详情/导入导出 |
| T05 产品/证书 | `Product`/`Certificate`/`ProductAdminController`/`CertificateAdminController` | 产品/资质页面 |
| T06 用户/RBAC/审计/附件 | `TenantUser`、`RoleDef`、`AuthInterceptor`、`AuditAspect`、`FileController` | 角色/审计/上传 |
| T07 需求/咨询 | `DemandAdminController`、`ConsultationAdminController` | 需求/咨询页面 |
| T08 方案/报价/议价 | `SolutionAdminController`、`QuoteAdminController`、`QuoteFlowService` | 方案/报价/PDF/审批 |
| T09 合同/订单/回款/安装 | `ContractAdminController`、`PaymentAdminController`、`OrderAdminController` | 合同履约/回款/验收 |
| T10 售后/维保/备件 | `TicketAdminController`、`MaintenanceAdminController`、`SpareStockController` | 工单/维保/库存 |
| T11 转介绍/满意度/知识库 | 实体 + CRUD 页面 | 页面/接口 |
| T12 公共基础设施 | `IdGenerator`、`StorageService`、`ExcelService`、`PdfService`、`RateLimiter` | PDF/Excel/存储/编号 |
| T13 消息通知 | `NotificationService`、`WechatGateway`、模板配置/发送日志 | 兜底+日志 |
| T14 验收框架 | `scripts/verify.ps1`、`docs/acceptance.md` | 26/26 |

## 阶段 2：管理后台（T15~T24）

| 任务 | 落点 | 本地验证 |
|---|---|---|
| T15 UI 框架 | `admin-web/src/layout/AppLayout.vue`、路由/多页签 | 浏览器验收 |
| T16 企业配置 | `TenantSettingsController`、`SettingsView` | 页面/接口 |
| T17 i18n | `language` 表、`I18nView`、门户语言切换 | 语言管理页面 |
| T18 菜单可视化 | `MenuAdminController`、`MenusView`、`/public/menus` | MP 4 项、编辑器 |
| T19 存储配置页 | `StorageAdminController`、`StorageView` | 5 卡片/测试/掩码 |
| T20 富文本 | `RichTextEditor.vue`、WangEditor + 上传 | 构建 |
| T21 图片/附件组件 | `ImageUpload.vue`、`FileUpload.vue` | 构建 |
| T22 企业资料/工作台 | `TenantSettingsController`、`DashboardView` | 管理员浏览器测试 |
| T23 RBAC/数据权限 | `AuthInterceptor`、服务层 customer 校验 | 越权测试 |
| T24 审计/布局 | `AuditAdminController`、`AuditsView`、Element Plus 列表规范 | 页面 |

## 阶段 3~8：主数据/需求/方案报价/履约/售后/报表（T25~T57）

均已在后端 Controller + 对应 Vue 页面落地；关键链路在 `scripts/verify.ps1` 与 `scripts/verify-flow.ps1` 实测：

- 客户导入/导出、产品/证书/备件导入、资质临期。
- 公开需求提交→生成编号→查询→重复拦截；需求认领/分配/跟进/状态。
- 咨询 FAQ/转需求；选型推荐；方案推送/客户确认。
- 报价议价/底价审批/在线确认/过期/PDF/合同草稿。
- 合同审批/签署/归档/订单/收款/发票/安装验收/设备建档/维保计划。
- 售后派单/处理/完工/满意度；备件库存/预警；知识库/转介绍/回访。
- 经营报表 + 预警聚合 + 合同/验收/安装/维保 PDF。

## 阶段 9：多端（T58、T61~T66）

| 任务 | 落点 | 状态 |
|---|---|---|
| T58 微信底座 | `WechatAdminController`、`WechatGateway`、`WechatView`、`WechatCallbackController` | 代码/本地回调验签；真实服务号待资质 |
| T61 小程序 | `miniprogram/` 18 页 | 代码完成；真机待微信开发者工具 |
| T62 公众号 | 回调/关键词/菜单/素材/标签/粉丝/群发/OAuth | 代码/本地模拟；真实服务号待资质 |
| T63 模板消息 | `TemplateMessageConfig`、`NotificationService`、限流/重试/日志 | 代码/本地兜底；真实模板 ID 待资质 |
| T64 portal-web 公开门户 | `portal-web/` 首页/产品/需求/咨询/机构/公告 | 类型检查/构建/浏览器通过 |
| T65 客户中心/核验/分享 | `AccountView`/`VerifyView`/分享页 | 已实测登录/核验/分享 |
| T66 三端联调 | 动态菜单/客户隔离/UnionID 代码 | 本地越权/菜单通过；UnionID/真机待资质 |

## 去租户改造

| 变更 | 落点 |
|---|---|
| 管理员登录无企业编码 | `AdminAuthController`、`admin-web/LoginView.vue` |
| 用户登录/注册无企业编码 | `PublicAuthController`、`CustomerAuthController`、`RegistrationService` |
| 角色仅 ADMIN/CUSTOMER | `AuthService`、`UserAdminController`、`role_def` |
| 平台端删除 | `PlatformController`（删除）、`platform_user`（删除）、`admin-web/views/platform`（删除） |
| 公开接口去企业参数 | `PublicPortalController`、`PublicDemandController`、`PublicAuthController`、`WechatCallbackController` |
| 响应隐藏企业字段 | `AuthUser`、`BaseEntity`、`AuditLog`、`Announcement` 的 `@JsonIgnore` |
| 企业配置键 | `tenant_config.company_name`、`SettingsView` |
| 公告来源 | `SYSTEM`/`COMPANY` |
| 文档 | `README.md`、`PRO.md`、`tasks.md`、`checklist.md`、`docs/*` |

## 未闭环项

严格以 `docs/acceptance.md` §5 为准：公众号认证、开放平台 UnionID、模板 ID、服务器回调 URL、合法域名、真机、微信支付、短信、正式域名/英文内容、Docker/MQ 生产联调。
