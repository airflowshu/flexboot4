# FlexBoot4 Media Starter

`flexboot4-media-starter` 提供视频中心核心能力：
- 流媒体服务（ZLMediaKit）接入、连通性测试与 hook 回写
- GB28181 网关运行时（SIP 注册、目录同步、实时/回放、PTZ）
- 设备与通道管理（GB28181 + 固定地址）
- 分屏方案管理
- 国标级联平台与通道绑定

## 实施进度（对应 `Media-PLAN.md`）

1. DDL、菜单、权限、实体、Mapper、Service 骨架：已完成  
2. `MediaServer`、`ZlmClient` 与 ZLM hook 闭环：已完成（Hook URL 使用服务 ID 稳定识别，支持一键同步到 ZLM）  
3. `MediaGateway` SIP 运行时与 GB28181 注册/目录同步：已完成  
4. RTSP 固定地址设备与通道管理：已完成  
5. 实时预览、录像回放、PTZ、截图：已完成  
6. 分屏展示：已完成  
7. 国标级联：已完成  
8. 联调加固、异常恢复、示例配置与使用文档：已完成（当前文档 + 运行时巡检任务）

## 联调加固与异常恢复

已内置 `MediaRuntimeMaintenanceTask` 定时巡检。Starter 默认关闭媒体运行时与巡检任务；显式开启 `media.runtime-check-enabled=true` 后，默认每 30 秒执行一次：
- 多实例部署时，巡检任务会通过 `DistributedLockService` 加锁；聚合引入 `admin-starter` 且 Redis 可用时自动使用 Redis 锁
- 网关运行态漂移修复：数据库显示 `RUNNING` 但进程内 runtime 不存在时，按配置自动拉起
- ZLM hook 超时降级：hook 心跳超时将媒体服务器标记为 `OFFLINE`
- GB28181 设备心跳超时降级：设备标记离线，通道状态回写为 `OFFLINE/STOPPED`
- 会话超时回收：`PENDING/STREAMING` 会话超时后自动关闭，避免僵尸流和僵尸播放状态

并在会话状态变更时同步通道状态：
- 会话进入 `STREAMING` 时，通道 `playStatus` 自动置为 `ONLINE`
- 会话关闭后，若通道无活动会话，自动置为 `STOPPED`

并对 ZLM hook 做了统一安全校验：
- `on_stream_changed`、`on_stream_none_reader`、`on_server_keepalive`、`on_rtp_server_timeout` 全部支持签名校验
- 支持传统签名：`sha256(body:hookSecret)`
- 支持带时间戳签名：`sha256(timestamp:body:hookSecret)`，默认时间窗口 300 秒（`media.hook-timestamp-tolerance-seconds`）
- 时间戳头默认 `X-Media-Hook-Timestamp`，签名头由 `media.hook-secret-header` 配置

## 示例配置

```yaml
media:
  enabled: true
  callback-base-url: http://localhost:8080
  default-play-protocol: http-flv
  snapshot-biz-type: media_snapshot

  gateway-core-threads: 2
  gateway-max-threads: 8
  gateway-queue-capacity: 1024

  hook-secret-header: X-Media-Hook-Signature
  hook-timestamp-tolerance-seconds: 300

  runtime-check-enabled: true
  runtime-check-initial-delay-millis: 30000
  runtime-check-fixed-delay-millis: 30000
  server-hook-timeout-seconds: 180
  device-keepalive-timeout-seconds: 180
  pending-session-timeout-seconds: 30
  streaming-session-timeout-seconds: 300
  gateway-auto-recover: true
```

Starter 默认不会注册媒体运行时 Bean 和后台 API；需要在业务应用中显式设置
`media.enabled=true`。需要定时巡检时再设置 `media.runtime-check-enabled=true`。

## ZLM Hook 配置

每个 `MediaServer` 都有独立 Hook URL，回调路径中必须包含服务 ID，避免多个 ZLM 实例回调时无法识别服务来源。可通过后台接口查询或同步：

- 查询：`GET /api/admin/media/server/{id}/hook-info`
- 同步到 ZLM：`POST /api/admin/media/server/{id}/sync-hook`

同步会写入 ZLM `hook.on_stream_changed`、`hook.on_stream_none_reader`、`hook.on_server_keepalive`、`hook.on_rtp_server_timeout` 和 `hook.admin_params`。
Hook 控制器只接受 `/api/admin/media/zlm/hook/{serverId}/*` 形式的回调路径；无 `serverId` 的旧路径不再保留。

## 联调建议顺序

1. 先完成 `MediaServer` 联通测试，确认 hook 可回写  
2. 再启动并联调 `MediaGateway`，确保设备注册和目录同步  
3. 验证通道实时/回放/PTZ，再验证分屏  
4. 最后联调国标级联，观察平台注册状态与目录可见性  
5. 演练故障：停 ZLM、断设备、重启网关，确认自动恢复和状态回写符合预期

## Integration Artifacts

- Practical access guide: `VIDEO-DEVICE-ACCESS-GUIDE.md`
- Full acceptance checklist: `MEDIA-INTEGRATION-CHECKLIST.md`
- Fast smoke script: `scripts/media-integration-smoke.ps1`

### Smoke Script Example

```powershell
pwsh ./scripts/media-integration-smoke.ps1 `
  -BaseUrl "http://localhost:8080" `
  -Token "<JWT_TOKEN>" `
  -ServerId "<MEDIA_SERVER_ID>" `
  -GatewayId "<MEDIA_GATEWAY_ID>" `
  -ChannelId "<CHANNEL_ID>" `
  -CascadeId "<CASCADE_ID>"
```
