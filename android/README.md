# android

目标机：小米 14 · Android 16（API 36）。

## 打开

Android Studio 打开本目录 → Sync → 选真机 Run。

## 联调

1. 起后端：`cd ../backend && npm run dev`
2. `app/build.gradle.kts` 里 `API_BASE_URL` 当前为 `http://47.122.111.42:8787/`
3. **若以前连过局域网后端**：清除 App 数据或卸载重装后再配对

## 接口（服务器需暴露）

| 项 | 值 |
|---|---|
| 协议 | HTTP（暂无 Nginx / HTTPS） |
| 地址 | `http://47.122.111.42:8787/` |
| 健康检查 | `GET /health` |
| 安全组 | 入站 TCP **8787** |

## 打包（正式旁加载，非 debug）

```bash
cd android
.\gradlew.bat assembleRelease
```

APK：`app/build/outputs/apk/release/app-release.apk`  
（arm64-only + R8 压缩；自用 debug 签名，可直接装小米）

## 使用

- 一方「创建一对」→ 把邀请码给对方
- 另一方「输入邀请码加入」
- 创建方进入互动页后可发喝水测闭环
- 「解绑」会拆掉整对（双方都要重新邀请）
