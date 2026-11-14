# 鑫火锅炉任务清单（单企业版）

> 更新时间：2026-10-31  
> 状态说明：已完成项已实现并经本地构建/接口/浏览器验收；外部资质项保持未完成。

## 阶段一：后端基础

- [x] Spring Boot 3.2.5 + Java 17 + MyBatis-Plus + MySQL 8 工程
- [x] 统一返回、异常处理、401/403/404/409/429、JWT、BCrypt、AES-GCM
- [x] 数据表 schema/seed 与幂等初始化、初始密码 BCrypt 化
- [x] 企业数据域插件、白名单、审计 AOP、限流、防重提交
- [x] 对象存储 5 驱动、PDF/Excel、微信网关、通知服务

## 阶段二：账号与角色收敛

- [x] 管理员登录改为 `{username,password}`
- [x] 用户登录/注册删除 `tenantCode`
- [x] `TENANT_ADMIN` → `ADMIN`，仅保留 `ADMIN`、`CUSTOMER`
- [x] 删除 `PLATFORM`、`SALES` 及历史业务角色
- [x] 删除平台端 `PlatformController`、`platform_user`、`/api/platform/**`
- [x] 删除租户注册 `GET /public/tenants`、`POST /tenants/register`
- [x] 删除后台企业注册页与路由
- [x] 原租户配置能力并入管理员后台（企业资料/用户/角色/字典/菜单/多语言/存储/微信/公告/审计/报表）

## 阶段三：公开接口去租户参数

- [x] 门户首页/产品/案例/公告/菜单/语言/机构页删除 `tenant` 参数
- [x] FAQ/需求/咨询/需求查重删除 `tenantCode`
- [x] 微信登录/网页授权/JS-SDK/回调删除 `tenantCode`/`tenant` 参数
- [x] 分享方案/报价响应 `tenant` → `company`
- [x] 登录/门户/分享接口返回 `company`，不再返回 `tenant`
- [x] `AuthUser`/`BaseEntity`/`AuditLog`/`Announcement` 隐藏 `tenantId`
- [x] 错误文案“租户不存在/停用”改为“企业信息不存在/系统已停用”

## 阶段四：三端前端

- [x] admin-web 登录页删除企业编码，改为账号/密码
- [x] admin-web 删除平台菜单、租户下钻、租户切换、平台页面
- [x] admin-web 用户页仅管理员/客户角色，默认管理员
- [x] admin-web 设置页企业资料、字典页企业扩展、公告来源系统/企业
- [x] portal-web 登录/注册删除租户与企业选择
- [x] portal-web 首页/产品/公告/客户中心/分享页使用 `companyName`
- [x] mp-h5 登录/注册删除租户与企业选择
- [x] mp-h5 首页/个人中心/咨询/需求提交删除租户参数与文案
- [x] 原生小程序登录/注册删除租户输入与企业选择
- [x] 原生小程序首页/个人中心/咨询/需求提交删除租户参数
- [x] 三端删除 SaaS/多租户/平台管理员文案

## 阶段五：数据清理

- [x] 删除 JSD/YN 两个演示企业数据域及全部关联数据
- [x] 数据库仅保留 1 个企业数据域
- [x] 删除 `platform_user` 表、实体与 Mapper
- [x] `role_def.scope` 迁移为 `COMPANY`
- [x] 公告来源 `PLATFORM/TENANT` 迁移为 `SYSTEM/COMPANY`
- [x] 通知用户类型 `TENANT` 迁移为 `ADMIN`
- [x] 企业配置键 `tenant_name` 迁移为 `company_name`
- [x] 种子数据删除演示企业、演示菜单与演示配置

## 阶段六：验收与文档

- [x] 后端 `mvn -o -DskipTests package` 构建通过
- [x] admin-web / portal-web / mp-h5 `vue-tsc --noEmit` + `vite build` 通过
- [x] `scripts/verify.ps1` 26/26 通过
- [x] `scripts/verify-flow.ps1` 全链路通过
- [x] Playwright 单企业 UI 验收 9/9 通过，控制台 0 错误
- [x] README、PRO、tasks、checklist、API、验收、运维、数据域文档同步
- [ ] 微信服务号/开放平台/模板消息/支付/短信真实资质联调
- [ ] 小程序真机与合法域名验收
- [ ] Redis/RabbitMQ/Docker 生产联调

## 后续可选项

- [ ] 将内部 `tenant` 表名/字段名进一步重命名为 `company`（当前仅内部数据模型，不对外暴露）
- [ ] 业务编号前缀 `HLJ` 改为企业品牌前缀 `XH`（需同步历史数据与打印模板）
- [ ] 管理员双因素认证与密码定期更换策略
