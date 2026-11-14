# 单企业数据域与权限说明

## 1. 模式说明

本项目已收敛为**单企业模式**：

- 全站固定一个企业数据域，不提供租户注册、租户切换、平台下钻或跨企业查询。
- 不再存在“租户”角色；后台只有管理员（`ADMIN`）与客户（`CUSTOMER`）两类账号。
- 登录只提交账号与密码，不再提交企业/租户编码。
- 原租户管理能力并入管理员后台：企业资料、用户、角色、字典、菜单、多语言、存储、微信配置、公告、审计与报表。
- 业务表仍保留内部企业归属字段，用于历史数据与查询约束；该字段不参与 JSON 序列化，也不在页面与登录流程中暴露。

## 2. 实现位置

- 数据域插件：`backend/src/main/java/com/xinhuo/boiler/config/MybatisPlusConfig.java`
  - `TenantLineInnerInterceptor` 自动为业务表注入企业归属条件。
  - 白名单：`tenant`、`role_def`、`sys_dict_global`、`platform_param`、`announcement`、`id_sequence`。
  - `announcement` 支持系统公告（无企业归属）与企业公告，由业务查询显式过滤。
- 数据域上下文：`TenantContext` / `AuthContext` ThreadLocal；登录后固定写入当前企业。
- JWT：`JwtService` 内部写入企业归属、角色、客户 ID 等声明；`AuthUser` 对外序列化时隐藏企业字段。
- 拦截器：`AuthInterceptor`
  - `/api/customer/**`、`/api/mp/**` → 仅 CUSTOMER。
  - `/api/admin/**` → 仅 ADMIN。
  - 无平台端、无租户下钻、无 `X-Tenant-Id`。
- 数据范围：
  - 管理员：本企业全部业务数据。
  - 客户：服务层强制 `customer_id = 当前客户`，他人需求/方案/报价/合同/工单 → 403 或空列表。
- 审计：`AuditAspect` + `AuditService`；越权访问写审计。

## 3. 本地验收证据

| 场景 | 请求 | 预期 | 实测 |
|---|---|---|---|
| 管理员登录 | `POST /api/admin/auth/login` `{username,password}` | 成功，返回 `company` | 通过 |
| 平台接口下线 | `GET /api/platform/dashboard` | 404 | 通过 |
| 公开门户 | `GET /api/public/portal/home` | 无需企业参数，返回 `company` | 通过 |
| 公开菜单 | `GET /api/public/menus?target=MP` | 4 条 | 通过 |
| 客户读他人需求 | `GET /api/customer/demands/2` | 403/404 | 通过 |
| 响应企业字段 | 登录/门户/产品/公告 | 不含 `tenant`/`tenantId` | 通过 |
| 审计 | `GET /api/admin/audits` | ≥1 条且无“租户”文案 | 通过 |

## 4. 上线前检查

1. 执行 `scripts/verify.ps1`，确认 26 项全部通过。
2. 检查登录、门户、产品、公告、分享接口响应中不含企业/租户字段。
3. 检查 admin-web 菜单无平台管理、租户管理、企业注册入口。
4. 检查门户、H5、小程序登录/注册页无企业编码、所属企业选择器。
5. 检查数据库仅保留一个企业数据域，历史演示数据已清理。
6. 客户 A 访问客户 B 的需求/合同/工单/维保仍需 403/404。
7. 检查审计日志记录越权操作，且无“租户”措辞。
