# 部署与运维手册

## 1. 部署拓扑

```
浏览器/微信
  ├─ admin-web  (Nginx:8081) ─┐
  ├─ portal-web (Nginx:8082) ─┼─ /api,/uploads → backend:8080 → MySQL:3306
  └─ mp-h5      (Nginx:8083) ─┘                          → Redis / RabbitMQ
                                                        → 本地磁盘 或 MinIO/S3/OSS/COS
```

前端为 SPA，Nginx `try_files ... /index.html` 已完成 History 路由回退；`/api` 与 `/uploads` 反代后端。

## 2. Docker Compose 上线

```bash
cp .env.example .env
# 必须修改：MYSQL_ROOT_PASSWORD、JWT_SECRET、APP_ENCRYPT_KEY、PUBLIC_BASE_URL
docker compose up -d --build
docker compose ps
```

端口（默认可在 `.env` 调整）：

| 服务 | 端口 |
|---|---|
| 管理后台 | 8081 |
| 门户 portal-web | 8082 |
| 移动 H5 | 8083 |
| 后端 API | 8080 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ / 管理台 | 5672 / 15672 |
| MinIO API / 控制台 | 9000 / 9001 |

首次启动后端自动建表 + 初始化数据（幂等）；健康检查 `/api/public/health`。

### 域名与 HTTPS
- 建议 Nginx/云负载均衡统一入口：`admin.example.com`、`portal.example.com`、`m.example.com`，全部 HTTPS。
- 微信小程序 request 合法域名与公众号网页授权域名需使用已备案 HTTPS 域名。
- 前端同域反代 `/api` 可避免 CORS；本工程后端已允许可配置 CORS。
- 正式域名确定后更新 `portal-web/index.html` 的 canonical/hreflang 与 SEO 配置。

## 3. 初始账号

见根目录 `README.md`。首启后 `DataInitializer` 将 `plain:` 密码 BCrypt 化；管理员初始化账号为 `admin / 123456`，客户通过门户/H5/小程序注册页自助开通账号。正式部署前请修改管理员密码，并清理不再使用的测试账号。

## 4. 对象存储

后台【对象存储】页：
1. 选择 LOCAL / S3 / OSS / COS / MinIO；
2. 填写参数，AK/SK 密码框 + 眼睛；
3. **连接测试通过后保存按钮才可用**；
4. 切换驱动二次确认；历史文件原地址保持可读。

建议生产使用 MinIO 或云 OSS/COS，Bucket 权限仅允许该应用读写；开启服务商版本/生命周期保护。本地驱动适合单机演示。

## 5. 微信配置（需外部资质）

后台【微信配置】：
- 小程序：AppID/AppSecret、推送 Token/AESKey、request/upload/download 合法域名。
- 服务号：AppID/AppSecret、服务器 URL（`https://api.example.com/api/public/wechat/callback`）、Token、EncodingAESKey、网页授权域名、JS 安全域名。
- 开放平台 AppID：小程序与公众号绑定同一主体，UnionID 才可互通。
- 菜单：编辑 JSON → 保存 →「一键同步微信」；无真实服务号时会明确报错并写审计。
- 消息配置：为 10 个业务节点填入微信模板 ID、启用、最小发送间隔；未填/失败自动站内兜底并写日志。
- 内容运营：素材/标签/粉丝/定向群发；真实群发需认证服务号且受微信频率限制。

## 6. 备份与恢复

```bash
# 每日 02:00 全量，保留 7 份（宿主机 crontab）
0 2 * * * /opt/xinhuo/deploy/backup.sh

# 手动
./deploy/backup.sh
./deploy/restore.sh backups/xh_boiler-20261031-020000.sql.gz
```

恢复演练步骤：
1. 在隔离环境 `docker compose up -d mysql`；
2. 导入最新备份；
3. 启动 backend，检查 `/api/public/health`、管理员登录、客户/需求/报价/合同数量；
4. 抽查对象存储附件 URL 可读；
5. 记录演练时间与结果。

`uploads_data` 卷需单独备份（或使用对象存储服务商版本保护）。

## 7. 日志与巡检

```bash
docker compose logs -f backend
docker compose logs -f admin-web portal-web mp-h5
docker compose ps
curl -s http://127.0.0.1:8080/api/public/health
```

重点关注：
- 每日 02:00 定时扫描日志（证书过期、报价过期、回款逾期、企业服务到期）。
- 09:00 维保/需求超时提醒日志。
- `NotifyLog` 失败率、模板限流；`AuditLog` 越权访问；上传失败。

## 8. 升级

```bash
git pull
docker compose build backend admin-web portal-web mp-h5
docker compose up -d
```

数据库结构为 `CREATE TABLE IF NOT EXISTS`，新增字段需编写增量 SQL 并执行，再启动后端；生产建议引入 Flyway 版本化。

## 9. 生产安全检查

1. 修改默认密码、JWT_SECRET、APP_ENCRYPT_KEY（长度 ≥32）。
2. MySQL 不对公网暴露；使用独立低权限账号而非 root。
3. Nginx 强制 HTTPS/HSTS；限制上传大小；开启 WAF/限流。
4. `/api/public/verify`、`/demands` 已有 IP 限流；如高并发需 Redis 分布式限流。
5. 用户账号隔离回归；管理员接口越权访问拦截与审计；接口响应不含企业/租户字段。
6. 备份恢复演练；对象存储密钥加密；日志中不输出明文手机号/Secret。
7. RabbitMQ/Redis 设置密码与内网访问；开启 MQ 消费者后将通知/审计异步化。
8. 小程序合法域名、公众号回调、模板 ID 全部在微信侧验证。

## 10. 常见问题

| 现象 | 排查 |
|---|---|
| 后端启动 3306 连接失败 | MySQL 是否健康、DB_URL 库是否存在、账号密码 |
| 登录 500 | 查看后端日志；`plain:` 密码首启 BCrypt；JWT/Encrypt Key 长度 |
| 中文乱码 | MySQL 库/表 `utf8mb4`；连接参数 `characterEncoding=utf8` |
| 上传 401/403 | 使用管理/客户 token；对象存储连接测试；Bucket 权限 |
| PDF 中文方框 | 确认 jar 内含 `fonts/simhei.ttf`；Dockerfile 复制资源 |
| 公众号回调 403 | URL/Token 与后台配置一致；EncodingAESKey 解密；签名算法 |
| 模板消息失败 | 模板 ID、服务号认证、IP 白名单、access_token、发送频率；查 `NotifyLog` |
| 三端 UnionID 不同 | 小程序与公众号是否绑定同一开放平台主体 |
| 管理员登录被拒 | 账号停用或企业服务到期；检查 `tenant_user.status` 与 `tenant.expire_time` |
| 前端 404 刷新 | Nginx `try_files` History 回退 |
| 小程序请求失败 | 开发者工具关闭域名校验；正式环境配置合法域名与 HTTPS |
