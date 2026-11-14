# 鑫火锅炉数字化多端平台

面向锅炉制造/销售企业的**单企业**数字化平台，完整覆盖：

**采购需求 → 售前咨询 → 产品选型/方案 → 报价议价（底价审批）→ 合同成交 → 生产发货 → 回款/发票 → 安装验收 → 售后工单/维保年检 → 转介绍/报表**

技术栈：Java 17 + Spring Boot 3.2 + MyBatis-Plus（企业数据域插件）+ MySQL 8 + Redis + RabbitMQ + MinIO（5 驱动存储）+ Vue3/Element Plus 管理后台 + Vue3 `portal-web` 门户 + Vue3 `mp-h5` 移动端 + 微信原生小程序 + Docker Compose。

## 目录结构

```
backend/        Spring Boot 后端、数据库 schema/seed、PDF/Excel/存储/微信/审计
admin-web/      PC 管理后台（管理员端，端口 5173）
portal-web/     公开门户 + 客户中心 + 资质核验 + 分享查询（端口 5174）
mp-h5/          移动 H5（客户移动端，端口 5175）
miniprogram/    微信原生小程序（客户移动端，tabBar 读取后台配置）
h5/             兼容静态门户入口（保留）
deploy/         备份/恢复脚本
docs/           验收、API、运维文档
scripts/        release-flow.ps1 上线前全链路验收脚本（自包含临时数据并自动清理）
docker-compose.yml
```

## 已实现能力

### 单企业底座
- 全站只保留一个企业数据域，不再暴露租户、平台、租户编码等概念；登录只填账号与密码。
- PC 后台只保留管理员角色（`ADMIN`）与客户角色（`CUSTOMER`），没有平台管理、租户管理、销售顾问角色。
- 原租户管理能力并入管理员后台：企业资料、用户、角色、字典、菜单、多语言、存储、微信配置、公告、审计与报表。
- 所有关键写操作审计；敏感手机号 AES-GCM 加密存储、展示掩码；密钥配置回掩码。
- 业务表保留内部企业数据域字段用于数据归属，但不在接口响应、页面或登录流程中暴露。
- 客户档案与需求、咨询、方案、案例、报价、合同、售后工单均支持后台「删除」（物理删除）；有下游关联时返回 409，需先删除下游，删除动作写入审计日志。

### 七大业务域
1. 客户档案（一企一档、联系人、Excel 导入导出、关联聚合视图）
2. 锅炉产品档案 + 资质证书台账（制造许可/监检/能效/安装/定检，90 天红黄灯）
3. 需求线索（H5/小程序/公众号/电话/人工多入口、查重、待认领/认领/分配/转派、跟进超时）
4. 售前咨询（在线/电话/到店、FAQ 初筛、转需求）
5. 方案与案例（选型推荐引擎、版本、分享留痕、客户在线确认、案例库）
6. 报价议价（多版本、逐轮议价、底价红线审批、在线确认、有效期自动过期、PDF、转合同）
7. 成交履约 + 售后（合同审批/签署/归档、订单进度、收款计划/账龄/催款、发票、安装验收、设备台账、工单状态机/派单/现场记录（含报修照片上传）/满意度、维保计划/年检提醒、备件库存、知识库、转介绍、报表）

### 企业能力
- 客户自助注册：门户、移动 H5、小程序填写企业/联系信息即可注册并自动登录；密码 BCrypt、手机号 AES-GCM 加密、注册限流与重复校验。管理员账号由种子数据初始化，不开放自助注册。
- H5 管理员工作台：mp-h5 登录不区分客户与管理员，统一输入账号密码；系统按账号角色自动进入客户首页或管理员工作台。管理员与 PC 后台共用同一 `admin` 账号和 `/api/admin` 接口，移动端提供客户、需求、咨询、方案、报价、合同、工单、维保、备件、产品证书、案例知识库、报表、预警、通知、用户与系统配置等模块。
- 角色精简：仅管理员与客户；销售顾问、平台管理员、租户管理员已从后端、PC 后台、mp-h5、原生小程序全端移除。
- 对象存储 5 驱动：本地 / AWS S3 / 阿里云 OSS / 腾讯云 COS / MinIO；连接测试通过才可保存、切换二次确认、页面密钥掩码。
- 富文本 WangEditor 5：工具栏、图片自动上传、源码、粘贴清洗。
- 图片/附件上传：拖拽、缩略图、预览、删除、进度；PDF/合同/证书附件独立列表。
- 菜单可视化：后台导航 / 小程序 tabBar（4 项） / H5 导航，前台实时读取。
- 多语言语言管理、企业参数可视化、公告 CMS、微信配置、模板消息映射/限流/重试/发送日志。
- 公众号：服务器回调验签（明文/AES）、关键词回复、自定义菜单保存/同步、图文素材、粉丝标签、定向群发。
- 三端账号：小程序 code2Session、公众号 snsapi_userinfo OAuth、UnionID/OpenID 绑定客户档案。
- 编号生成：`GL-HLJ-DEMAND-2026-0001` 形式，MySQL 原子序列防并发重号。
- PDF：报价单、合同、验收单、安装调试记录、维保报告（内置中文字体，含签字位）。
- Excel：客户/产品/证书/备件导入，客户/产品/证书/备件/审计/报表导出。
- 限流防刷、防重提交、静态文件访问、统一异常/401/403/404/409 处理。

## 本地启动

### 1. 数据库
MySQL 8 需创建库（默认 `xh_boiler`）：

```sql
CREATE DATABASE xh_boiler CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

后端首次启动会自动执行 `backend/src/main/resources/db/schema.sql` 与 `seed.sql`（幂等），并由 `DataInitializer` 将初始密码 BCrypt 化。

### 2. 后端
```powershell
$env:JAVA_HOME='C:\Program Files\RedHat\java-17-openjdk-17.0.19.0.10-1'
$env:DB_URL='jdbc:mysql://127.0.0.1:3307/xh_boiler?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='root'
$env:UPLOAD_DIR='d:\ai\boiler-platform\uploads'
cd backend
mvn -DskipTests package
java -jar target/boiler-platform-1.0.0.jar
```
后端：`http://127.0.0.1:8080`，健康检查 `/api/public/health`。

### 3. 三端前端
```powershell
cd admin-web;  npm install; npm run dev   # 5173
cd portal-web; npm install; npm run dev   # 5174
cd mp-h5;      npm install; npm run dev   # 5175
```
微信原生小程序用微信开发者工具导入 `miniprogram/`，`project.config.json` 中 `urlCheck=false`；本地接口默认 `http://127.0.0.1:8080/api`。

## 注册与登录

- **管理员**：后台 `/login` 直接使用初始化账号 `admin / 123456` 登录，不再填写企业编码。
- **客户**：门户 `/register`、移动 H5 `/register` 或小程序「注册用户」页面，填写联系信息即可注册（企业/单位名称、所属行业、地区选填），注册后自动登录客户中心；登录只填账号与密码。
- **员工账号**：管理员登录后在「系统用户」中创建管理员或用户账号。
- 登录入口：后台 `/login`、门户 `/login`、mp-h5 `/login`、小程序登录页，全部不再要求租户/企业编码。
- 后台不提供企业注册入口；客户演示账号与登录页演示入口已移除。

## 验收

```powershell
# 上线前全链路验收：临时客户注册→需求→方案→报价→合同→回款→验收→设备→工单→报表→PDF，结束自动清理
powershell -File scripts/release-flow.ps1

# 三端构建
cd admin-web;  npm run type-check; npm run build
cd portal-web; npm run type-check; npm run build
cd mp-h5;      npm run type-check; npm run build
```

> `scripts/verify.ps1`、`scripts/verify-flow.ps1` 为早期演示数据验收脚本，依赖已移除的演示客户/需求/报价；当前数据库只保留真实注册数据，请以上线前全链路脚本 `scripts/release-flow.ps1` 为准。
本地验收结果、截图与未闭环外部条件见 `docs/acceptance.md`；关键页面截图位于 `docs/screenshots/`。

## Docker 部署

```bash
cp .env.example .env
docker compose up -d --build
```
服务端口：后台 8081、门户 8082、移动 H5 8083、后端 8080、MySQL 3306、Redis 6379、RabbitMQ 5672/15672、MinIO 9000/9001（可由 `.env` 调整）。详见 `docs/operations.md`。

## 说明与外部依赖

- 真实微信公众号（服务号）认证、开放平台 UnionID 主体、模板消息 ID、服务器回调 URL、合法域名、微信开发者工具真机环境未在本环境提供；相关代码与接口已实现，端到端验收需在资质环境完成。
- 本机未安装 Docker/Redis/RabbitMQ，开发验收使用独立 MySQL 8 实例（3307）。Redis/RabbitMQ 已在 Compose 中编排且后端已接入依赖与配置；生产建议开启异步消息与分布式缓存。
- 微信支付、短信通道按需求为预留开关/接口；需商户号与短信服务商后接入。
- 英文内容录入和正式域名 hreflang 需业务提供英文文案与备案域名；门户已提供语言切换与 hreflang 占位。
