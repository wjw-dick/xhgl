# API 说明

统一返回：

```json
{ "code": 0, "message": "ok", "data": ... }
```

- 成功 `code=0`；业务错误使用 400/401/403/404/409/429/500 HTTP 状态并返回 `code/message`。
- 登录后请求头 `Authorization: Bearer <JWT>`。
- 分页接口返回 `{ records, total, current, size }`。
- 单企业模式：公开接口不再需要企业/租户参数；登录只提交账号与密码；不返回 `tenant`、`tenantId`、`tenantCode` 等字段。
- 企业数据域由服务端内部固定，不接受客户端切换；越权访问返回 403/404 并留审计。
- 敏感字段（手机号）AES-GCM 加密存储，返回掩码；密钥配置页面回掩码 `********`。

## 公开接口 `/api/public`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 健康检查（DB） |
| GET | `/portal/home` | 首页：企业、产品、案例、公告、H5 菜单、统计 |
| GET | `/products` | 产品分页筛选（boilerType/fuelType/capacity/keyword） |
| GET | `/products/{id}` | 产品详情 + 脱敏资质附件 |
| GET | `/cases` | 成功案例 |
| GET | `/announcements`、`/announcements/{id}` | 公告 |
| GET | `/menus?target=H5|MP` | 门户/移动端导航（后台配置） |
| GET | `/languages` | 启用语言 |
| GET | `/institution` | 企业门户 |
| GET | `/verify?certNo=&keyword=` | 资质公开核验（IP 60 次/分钟；仅摘要） |
| POST | `/demands` | 需求提交（限流 5/分钟；409 查重；Idempotency-Key 防重） |
| POST | `/demands/duplicate` | 提交前查重 |
| GET | `/demands/track?demandNo=&phone=&token=` | 需求进度查询 |
| POST | `/consultations` | 在线咨询（FAQ 初筛） |
| GET | `/faqs` | FAQ |
| GET | `/share/solution?token=` | 分享方案（浏览留痕、证书脱敏） |
| POST | `/share/solution/confirm?token=` | 客户确认方案 → 需求推进谈价 |
| GET | `/share/quote?token=` | 分享报价（脱敏，无底价/审批意见） |
| POST | `/share/quote/confirm?token=` | 客户确认报价 → 生成合同草稿 |
| POST | `/share/quote/bargain?token=` | 客户还价留痕 |
| GET | `/share/demand?demandNo=&token=` | 分享需求进度 |
| POST | `/auth/login` | 客户登录：`{username,password}`（仅客户角色） |
| POST | `/auth/user-login` | H5 统一用户登录：客户/管理员同一账号密码入口，按角色返回对应会话 |
| POST | `/auth/register` | 客户自助注册：`{contactName,phone,email,username,password}`，`companyName/industry/region` 选填；返回登录会话 |
| POST | `/auth/wechat` | 小程序 code2Session + OpenID/UnionID 绑定 |
| POST | `/auth/wechat-oauth` | 公众号网页授权 code 换用户并登录 |
| GET | `/oauth/wechat/url?redirect=` | 生成网页授权 URL（校验回跳域名） |
| GET | `/wechat/js-config?url=` | JS-SDK 签名 |
| GET/POST | `/wechat/callback` | 公众号服务器 URL 验签/明文或 AES 消息回复 |

## 客户中心 `/api/customer`（CUSTOMER）

`/overview`、`/demands`、`/demands/{id}`、`/solutions`、`/solutions/{id}`、`POST /solutions/{id}/confirm`、`/quotes`、`/quotes/{id}`、`POST /quotes/{id}/confirm`、`POST /quotes/{id}/bargain`、`/contracts`、`/contracts/{id}`、`POST /contracts/{id}/sign`、`/tickets`、`POST /tickets`（支持 `photos`，逗号分隔 URL）、`/tickets/{id}`、`POST /tickets/{id}/review`、`/maintenance`、`/equipment`、`/notifications`、`POST /notifications/{id}/read`、`/profile`。

- 客户附件：`POST /files/upload`（multipart `file` + `category`，30 次/分钟限流；返回 `url` 供报修照片等字段引用）。

## 管理端 `/api/admin`（ADMIN）

### 登录/业务
- `POST /auth/login` `{username,password}`；不再接受企业/租户编码。
- `GET /auth/me`、`POST /auth/logout`；`me` 返回 `user` 与 `company`。
- 客户档案：`DELETE /customers/{id}` **物理删除**客户档案，同时物理删除其用户登录账号、联系人及相关通知；已关联需求/咨询/报价/合同/工单/设备/证书/回访/转介绍等业务数据时返回 409，需先处理业务或改为禁用。删除后可再次使用相同企业名称、登录账号与手机号注册。

关键业务端点：
- 需求：`POST /demands`、`POST /{id}/claim|assign|transfer|follow|status`、`GET /public-pool|funnel|duplicate|recommend`、`DELETE /demands/{id}`。
- 方案：`POST /solutions`、`POST /{id}/send|confirm|new-version`、`DELETE /solutions/{id}`。
- 案例：`/cases` CRUD（`DELETE /cases/{id}` 物理删除）。
- 咨询：`POST /consultations`、`/{id}/reply|to-demand`、`DELETE /consultations/{id}`。
- 报价：`POST /quotes`、`POST /{id}/negotiate|approval|approve|send|confirm|convert-contract|new-version`、`GET /{id}/pdf`、`POST /expire-scan`、`DELETE /quotes/{id}`。
- 合同：`POST /contracts`、`/{id}/approve|sign|archive|payment-plans|invoices|installations/{installId}/update`、`/{id}/pdf|acceptance-pdf|installation-pdf`、`DELETE /contracts/{id}`。
- 回款：`/payments/aging`、`POST /payments/remind`、`POST /contracts/payment-plans/{id}/receive`。
- 工单：`POST /tickets`、`/{id}/dispatch|accept|records|complete|review`、`GET /tickets/analysis`、`DELETE /tickets/{id}`。
- 维保：`POST /maintenance`、`/{id}/complete`、`/maintenance/warnings`、`/{id}/pdf`。
- 备件：`/spares` CRUD、`/spare-records`、`POST /spare-records/operation`、`/spare-records/alerts`。
- 导入：`POST /customers/import`、`/products/import`、`/certificates/import`、`/spares/import`。
- 导出：`GET /customers|products|certificates|spares|audits|reports/export`。
- 报表：`GET /reports`（漏斗、月度成交回款、业绩归属、回款状态、售后指标）。
- 预警：`GET /warnings`、`POST /warnings/scan`。

### 业务数据删除

以下 `DELETE` 均为**物理删除**（直接执行 `DELETE FROM`，不保留逻辑删除标记），写入审计日志；存在下游关联时返回 409 并提示先删除下游。

| 接口 | 级联/约束 |
|---|---|
| `DELETE /demands/{id}` | 删除需求及跟进记录；关联咨询服务置空；已存在方案或报价时 409 |
| `DELETE /consultations/{id}` | 直接物理删除咨询记录 |
| `DELETE /solutions/{id}` | 已存在关联报价时 409 |
| `DELETE /cases/{id}` | 直接物理删除案例 |
| `DELETE /quotes/{id}` | 删除报价及议价记录；已生成合同时 409 |
| `DELETE /contracts/{id}` | 级联删除订单、收款计划、发票、安装验收、设备、维保计划、满意度、回访；关联工单设备字段置空 |
| `DELETE /tickets/{id}` | 级联删除处理记录、满意度、备件领用记录 |


### 配置与管理
- `/settings/configs`、`PUT /settings/configs`、`PUT /settings/profile`、`/settings/dicts`、`/settings/languages`。
- `/menus`（target=ADMIN/MP/H5，拖拽排序 `/reorder`）。
- `/users` + `/users/roles` + 重置密码。
- `/storage`、`POST /storage/test`、`PUT /storage`。
- `/wechat/config`、`/wechat/menu`、`/wechat/menu/sync`、`/wechat/materials|tags|fans|mass-send|mass-logs`。
- `/notifications`、`/notifications/configs`、`/notifications/logs`、`/notifications/test`。
- `/announcements`、`/audits`。
- `/files/upload`（multipart `file` + `category`）、`DELETE /files/{id}`。

## 已移除接口

- `/api/platform/**` 全部移除（平台登录、平台看板、租户开通/续期/配额/下钻）。
- `/api/public/tenants`、`/api/public/tenants/register` 移除。
- `/api/admin/sales/**` 与销售顾问角色相关接口移除。

## 错误码示例

| HTTP | 场景 |
|---|---|
| 401 | 未登录/Token 失效/密码错误 |
| 403 | 越权/客户访问管理端/非管理员访问管理接口 |
| 404 | 记录不存在或接口已下线 |
| 409 | 近期重复需求、重复提交、24 小时重复报修 |
| 429 | 公开需求/核验/登录等接口限流 |
| 500 | 未处理异常（返回脱敏消息，日志保留堆栈） |
