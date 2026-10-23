# backend

Fastify + better-sqlite3。无账号密码：邀请码成对 + 设备令牌鉴权。

## 启动

```bash
cd backend
npm install
# 若改过表结构，删掉旧库再 init
del data\mind.db
npm run db:init
npm run dev
```

默认 `http://127.0.0.1:8787`。自用服务器：`http://47.122.111.42:8787`。

## 服务器部署（简版）

```bash
# 代码放到 /opt/mind 后：
cd /opt/mind/backend
npm install --omit=dev
npm run db:init   # 仅首次

export PORT=8787 HOST=0.0.0.0
export JWT_SECRET='请换成长随机串'
npm start
# 建议用 systemd / pm2 常驻；云安全组放行 TCP 8787
```

备份：`data/mind.db` + `uploads/`。

## 配对 API

- `POST /pair/create` `{ partnerPronoun?: "she"|"he"|"ta" }` → token、inviteCode、recoveryCode
- `POST /pair/join` `{ code, partnerPronoun? }` → token
- `POST /pair/recover` `{ recoveryCode, side: "a"|"b" }` → 新 token
- `POST /pair/unbind`（需 Bearer）→ 拆掉整对
- `GET /me`（需 Bearer）
