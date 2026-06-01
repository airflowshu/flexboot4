# FlexBoot4 视频设备实战接入指南

本文面向需要在开发环境真实接入视频设备的开发者，按可操作步骤完成：

- ZLMediaKit 流媒体服务接入
- GB28181 摄像机/NVR 注册、目录同步和直播
- 固定地址 RTSP/RTMP 设备接入
- 直播、回放、PTZ、截图和分屏验证
- 常见问题排查

## 1. 接入前准备

### 1.1 网络要求

开发环境至少需要三类网络互通：

| 方向 | 说明 |
| --- | --- |
| FlexBoot4 -> ZLMediaKit | 调用 ZLM HTTP API，例如 `http://127.0.0.1:8080/index/api/version` |
| ZLMediaKit -> FlexBoot4 | ZLM Hook 回调到 `media.callback-base-url` |
| 设备 -> FlexBoot4 | GB28181 设备向视频网关 SIP 端口注册，默认 UDP `5060` |

如果使用真实摄像机或 NVR，建议 FlexBoot4、ZLMediaKit、设备处于同一局域网。跨网段、NAT、Docker、虚拟机环境下，优先确认 IP 和端口映射，不要先怀疑业务代码。

### 1.2 必备组件

- FlexBoot4 后端：`flexboot4-bootstrap`
- FlexBoot4 前端：`vue-vben-admin/apps/web-antd`
- PostgreSQL：用于保存媒体配置和设备通道
- Redis：建议开启，用于分布式锁等运行态能力
- ZLMediaKit：用于收流、转协议和播放
- 至少一种视频源：
  - GB28181 摄像机/NVR
  - 固定地址 RTSP/RTMP 流

### 1.3 后端配置

在业务应用配置中启用媒体能力：

```yaml
media:
  enabled: true
  callback-base-url: http://<FlexBoot4后端可被ZLM访问的地址>:8080
  default-play-protocol: http-flv
  snapshot-biz-type: media_snapshot

  runtime-check-enabled: true
  runtime-check-initial-delay-millis: 30000
  runtime-check-fixed-delay-millis: 30000
  server-hook-timeout-seconds: 180
  device-keepalive-timeout-seconds: 180
  pending-session-timeout-seconds: 30
  streaming-session-timeout-seconds: 300
  gateway-auto-recover: true
```

`callback-base-url` 必须填写 ZLMediaKit 能访问到的后端地址。不要在 Docker/局域网联调里填 `localhost`，除非 ZLMediaKit 和 FlexBoot4 在同一个网络命名空间内。

### 1.4 启动后端和前端

后端：

```powershell
.\gradlew.bat :flexboot4-bootstrap:bootRun
```

前端：

```powershell
cd E:\flexboot4\flexboot-web\vue-vben-admin
pnpm run dev:antd
```

确认登录后台后能看到：

- 视频中心
- 流媒体服务
- 视频网关
- 视频设备
- 分屏展示
- 国标级联

## 2. 接入 ZLMediaKit

### 2.1 启动 ZLMediaKit

启动方式按本地环境选择。关键是确认 ZLM HTTP API 可访问：

```powershell
curl "http://<ZLM地址>:<HTTP端口>/index/api/version?secret=<ZLM_SECRET>"
```

能返回版本信息即可进入下一步。

### 2.2 新增流媒体服务

进入后台：

`视频中心 -> 流媒体服务 -> 新增流媒体服务`

建议字段：

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| 服务名称 | `dev-zlm` | 自定义名称 |
| Base URL | `http://192.168.1.20:8080` | ZLM HTTP API 基址 |
| API Secret | `zlm-secret` | ZLM API secret |
| Hook Secret | `hook-secret` | FlexBoot4 校验 ZLM hook 用 |
| 播放域名 | `192.168.1.20` | 浏览器访问播放流的地址 |
| 公网主机 | `192.168.1.20` | 无播放域名时使用 |
| 默认 Stream App | `rtp` | GB28181 默认使用 `rtp` |
| 启用 Hook | 开启 | 建议开启 |

保存后点击：

1. `测试`
2. `Hook`
3. `同步 Hook`

### 2.3 校验 Hook 配置

点击 `Hook` 后，应看到类似：

```text
hook.on_stream_changed = http://<FlexBoot4后端>/api/admin/media/zlm/hook/<MediaServerId>/on_stream_changed
hook.on_stream_none_reader = http://<FlexBoot4后端>/api/admin/media/zlm/hook/<MediaServerId>/on_stream_none_reader
hook.on_server_keepalive = http://<FlexBoot4后端>/api/admin/media/zlm/hook/<MediaServerId>/on_server_keepalive
hook.on_rtp_server_timeout = http://<FlexBoot4后端>/api/admin/media/zlm/hook/<MediaServerId>/on_rtp_server_timeout
hook.admin_params = secret=<Hook Secret>
```

当前实现只接受带 `serverId` 的 Hook 路径，不保留无 `serverId` 的旧路径。

同步成功后，等待 ZLM keepalive 回调，`流媒体服务` 的 `lastHookTime` 应更新。也可以通过 API 手工检查：

```powershell
curl -H "Authorization: Bearer <JWT>" `
  "http://localhost:8080/api/admin/media/server/<MediaServerId>/hook-info"
```

## 3. 接入 GB28181 设备

GB28181 适用于支持国标协议的摄像机、NVR、平台。通常流程是：

1. 在 FlexBoot4 创建并启动视频网关
2. 在设备侧配置 SIP 服务器信息
3. 设备向 FlexBoot4 注册
4. FlexBoot4 同步设备目录和通道
5. 在通道上发起直播、回放、PTZ

### 3.1 新增视频网关

进入：

`视频中心 -> 视频网关 -> 新增视频网关`

建议字段：

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| 所属流媒体服务 | `dev-zlm` | 选择上一步创建的 ZLM |
| 网关名称 | `dev-gb-gateway` | 自定义 |
| 网关编码 | `34020000001110000001` | 建议和 SIP ID 一致 |
| SIP ID | `34020000001110000001` | 本平台/网关国标 ID |
| SIP 域 | `3402000000` | 通常为行政区域编码前 10 位 |
| SIP 密码 | `12345678` | 设备注册认证密码，可为空 |
| 本地 IP | `0.0.0.0` 或网卡 IP | SIP 监听地址 |
| 本地端口 | `5060` | 设备注册到此端口 |
| 公网 IP | `192.168.1.10` | 设备可访问的后端 IP |
| 公网端口 | `5060` | 设备可访问的 SIP 端口 |
| RTP IP | `192.168.1.20` | ZLM 收 RTP 的 IP，通常填 ZLM 所在机器 |
| 传输协议 | `UDP` | V1 推荐 UDP |
| RTP 起始/结束端口 | 可留空 | 当前 ZLM 使用动态 `openRtpServer` |
| 活动网关 | 开启 | 单节点单活动网关 |
| 启用 | 开启 | 必须开启 |

保存后点击 `启动`。启动成功后 `runtimeStatus` 应为 `RUNNING`。

### 3.2 配置摄像机/NVR

在设备 Web 管理页找到 GB28181 或国标接入配置，填入：

| 设备侧字段 | 填写值 |
| --- | --- |
| SIP 服务器 ID | FlexBoot4 视频网关 `SIP ID` |
| SIP 服务器域 | FlexBoot4 视频网关 `SIP 域` |
| SIP 服务器 IP | FlexBoot4 视频网关 `公网 IP` |
| SIP 服务器端口 | FlexBoot4 视频网关 `公网端口`，例如 `5060` |
| 设备国标 ID | 设备自己的 20 位国标编码 |
| 注册密码 | FlexBoot4 视频网关 `SIP 密码` |
| 注册有效期 | `3600` |
| 心跳周期 | `60` |
| 传输协议 | `UDP` |

保存后设备应主动注册。回到 FlexBoot4：

`视频中心 -> 视频设备`

应能看到自动创建或更新的设备，状态为 `ONLINE`，详情里能看到通道列表。

### 3.3 验证目录同步

设备注册成功后，网关会发起目录查询。检查：

- 设备详情页存在通道
- 通道编码与设备/NVR 中的通道国标 ID 一致
- 通道状态为 `ONLINE` 或设备上报的状态

如果设备在线但没有通道，通常是设备未响应 Catalog 或目录分页字段异常，先查看后端日志中的 GB28181 MESSAGE/Catalog 处理日志。

### 3.4 发起直播

进入：

`视频中心 -> 视频设备 -> 设备详情 -> 通道列表`

选择通道，点击实时预览或播放按钮。

预期结果：

- 后端创建 `media_stream_session`
- ZLM 打开 RTP server
- 网关向设备发送 INVITE
- ZLM 收到设备 RTP 流后触发 `on_stream_changed`
- 通道 `playStatus` 变为 `ONLINE`
- 前端播放器出现画面

如果没有画面，按顺序检查：

1. `media_stream_session.status` 是否从 `PENDING` 变为 `STREAMING`
2. ZLM `getMediaList` 是否能看到 `app=rtp` 的流
3. ZLM hook 是否回调到 `/api/admin/media/zlm/hook/{serverId}/on_stream_changed`
4. 设备是否实际向 ZLM RTP 端口发流
5. 浏览器是否能访问返回的 `http-flv` 或 `ws-flv` URL

### 3.5 停止直播

停止播放后，后端会：

- 关闭 SIP 会话
- 关闭 ZLM RTP server
- 将 session 标记为 `CLOSED`
- 若通道没有活动会话，将 `playStatus` 置为 `STOPPED`

建议每次联调都检查停止后没有遗留 `STREAMING` 会话。

### 3.6 验证录像回放

前提：设备/NVR 支持录像查询，并且目标时间段内有录像。

在设备详情页选择通道：

1. 设置回放时间范围
2. 查询录像
3. 选择录像片段
4. 开始回放
5. 停止回放

如果录像查询为空：

- 确认设备本地/NVR 确实有录像
- 检查设备是否支持 GB28181 `RecordInfo`
- 检查时间范围和设备时区
- 检查后端是否收到 `RECORDINFO` 响应

### 3.7 验证云台控制

PTZ 只对支持云台的通道有效。

在设备详情页选择通道，执行上、下、左、右、停止等命令。预期设备转动或停止。

如果返回成功但设备不动：

- 确认通道 `ptzType` 不是 `NONE`
- 确认设备启用了云台权限
- 确认设备厂商是否要求特定 PTZ 命令格式
- 检查设备是否接收到了 `DeviceControl` MESSAGE

### 3.8 验证截图

截图依赖前端播放器当前画面。先让通道进入直播或回放状态，播放器出现画面后点击 `截图`。

预期结果：

- 前端从当前视频画面生成 PNG
- 文件上传接口返回成功
- 页面提示 `截图已上传到文件中心`
- 文件中心能按 `media_snapshot` 业务类型查询到截图文件

如果截图失败：

- 确认播放器已经有实际画面，不只是空白或加载状态
- 确认浏览器能访问播放地址，且没有跨域导致 canvas 被污染
- 确认文件上传接口和文件中心配置可用
- 查看浏览器控制台是否有 `canvas`、网络或权限错误

## 4. 接入固定地址 RTSP/RTMP 设备

固定地址适用于网络摄像机、视频文件代理、已有 RTSP/RTMP 推拉流地址。它不需要 GB28181 注册。

### 4.1 新增固定地址设备

进入：

`视频中心 -> 视频设备 -> 新增设备`

建议字段：

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| 所属流媒体 | `dev-zlm` | 必填 |
| 设备名称 | `rtsp-camera-01` | 自定义 |
| 设备编码 | `RTSP-CAMERA-01` | 保持唯一 |
| 接入方式 | `FIXED_ADDRESS` | 固定地址 |
| 固定地址 / 媒体 URL | `rtsp://user:pass@192.168.1.30:554/Streaming/Channels/101` | 可作为设备默认地址 |
| 启用 | 开启 | 必须开启 |

### 4.2 新增固定地址通道

在设备详情页新增通道：

| 字段 | 示例 |
| --- | --- |
| 通道名称 | `主码流` |
| 通道编码 | `RTSP-CAMERA-01-CH1` |
| 通道类型 | `VIDEO` |
| 固定地址 | `rtsp://user:pass@192.168.1.30:554/Streaming/Channels/101` |
| Stream App | `proxy` |
| Stream ID | `rtsp-camera-01-ch1` |
| 支持录像 | 按实际情况 |

### 4.3 发起固定地址直播

点击实时预览。后端会调用 ZLM `addStreamProxy`，ZLM 从固定地址拉流并转成 HTTP-FLV/WS-FLV/HLS 等播放地址。

如果播放失败：

1. 在 FlexBoot4 服务器上直接测试 RTSP 地址是否可访问
2. 确认 ZLM 所在机器能访问 RTSP 地址
3. 检查摄像机账号密码和通道路径
4. 查看 ZLM `getMediaList` 是否出现 `proxy/<streamId>`
5. 查看浏览器是否能访问返回的播放地址

## 5. 分屏展示验证

进入：

`视频中心 -> 分屏展示`

步骤：

1. 新增分屏方案
2. 选择 `1x1`、`2x2`、`3x3` 或自定义布局
3. 为每个槽位选择通道
4. 保存
5. 重新打开方案，确认槽位和通道能回显
6. 逐个槽位播放，确认不会串流

如果分屏能保存但播放失败，优先按单通道直播排障，不要先排查分屏页面。

## 6. 运行 smoke 脚本

已有脚本：

```powershell
pwsh .\flexboot4-media-starter\scripts\media-integration-smoke.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Token "<JWT_TOKEN>" `
  -ServerId "<MEDIA_SERVER_ID>" `
  -GatewayId "<MEDIA_GATEWAY_ID>" `
  -DeviceId "<MEDIA_DEVICE_ID>" `
  -ChannelId "<CHANNEL_ID>"
```

这个脚本会覆盖流媒体测试、网关启动、设备详情、直播、停播、录像查询和 PTZ 的基础路径。

## 7. 常见问题排查

### 7.1 ZLM 测试失败

检查：

- `Base URL` 是否填到 ZLM HTTP API 端口
- `API Secret` 是否和 ZLM 配置一致
- FlexBoot4 后端机器能否访问 ZLM
- ZLM 是否启用了 HTTP API

### 7.2 Hook 不回写

检查：

- `media.callback-base-url` 是否是 ZLM 能访问到的地址
- 后台 `同步 Hook` 是否成功
- ZLM hook URL 是否带 `/api/admin/media/zlm/hook/{serverId}/`
- `hook.admin_params` 中的 `secret` 是否等于 `Hook Secret`
- 后端日志是否出现 `invalid hook signature` 或 `unknown media server`

### 7.3 设备注册不上

检查：

- 设备侧 SIP 服务器 IP/端口是否指向 FlexBoot4 视频网关
- FlexBoot4 网关 `localPort` 是否被占用
- 防火墙是否放行 UDP `5060`
- 设备注册密码是否等于网关 `SIP 密码`
- SIP 域是否和设备配置一致
- 网关是否已经启动且 `runtimeStatus=RUNNING`

### 7.4 设备在线但没有通道

检查：

- 设备是否支持 Catalog 查询
- NVR 是否有通道并允许国标目录上报
- 后端日志是否收到 `CmdType=Catalog`
- 通道国标 ID 是否为空或重复

### 7.5 直播会话一直 PENDING

检查：

- ZLM 是否成功打开 RTP server
- 设备是否接受 INVITE
- 设备是否向 ZLM RTP 端口发送 RTP
- ZLM 是否产生 `on_stream_changed`
- RTP IP 是否填写为设备可达的 ZLM 地址
- NAT/Docker 端口映射是否正确

### 7.6 前端拿到播放地址但无画面

检查：

- 浏览器能否直接访问返回的 `http-flv`/`ws-flv` URL
- `playDomain` 或 `publicHost` 是否浏览器可达
- ZLM 是否启用了对应播放协议
- 浏览器控制台是否有跨域或网络错误

### 7.7 固定地址拉流失败

检查：

- RTSP/RTMP 地址是否能用 VLC 或 ffplay 播放
- ZLM 所在机器是否能访问摄像机
- 摄像机是否限制同时连接数
- URL 中用户名、密码、特殊字符是否需要转义

## 8. 建议验收顺序

1. ZLM API 测试成功
2. ZLM Hook 同步成功，`lastHookTime` 能更新
3. 固定地址 RTSP 通道能播放
4. GB28181 网关能启动
5. GB28181 设备能注册上线
6. GB28181 通道目录能同步
7. GB28181 通道能直播和停播
8. 支持录像的通道能查询和回放
9. 支持云台的通道能 PTZ
10. 分屏方案能保存、回显和播放

按这个顺序能最快定位问题。不要一开始就直接调分屏或级联，它们依赖前面的基础链路全部正常。
