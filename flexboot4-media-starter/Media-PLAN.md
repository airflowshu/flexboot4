# FlexBoot4 视频中心 V1 实施计划

## 摘要
- 目标是在 `flexboot4-media-starter` 内嵌实现 JetLinks 风格视频中心首期完整版，保持当前 starter 架构，不新增独立服务。
- 功能闭环按 `流媒体服务 -> GB28181 视频网关 -> 视频设备/通道 -> 实时预览 -> 录像回放 -> 云台控制 -> 分屏展示 -> 国标级联` 落地，并按真实 ZLMediaKit、GB28181 设备、RTSP 样例和上级平台联调验收。
- 适配原则是不复刻 JetLinks 的通用产品/网络组件平台层，而是在 FlexBoot4 中收敛为视频专用模型；公共 CRUD 底座复用 `admin-kernel` 的 BaseCrudController/BaseServiceImpl/MyBatis-Flex 能力，管理台组合运行时再接入 `admin-starter` 的 RBAC、Redis、MinIO 和现有 Vue 管理台。

## 关键变更
- 在 `flexboot4-media-starter` 新增持久化模型：`MediaServer`、`MediaGateway`、`MediaDevice`、`MediaChannel`、`MediaStreamSession`、`MediaScreen`、`MediaScreenSlot`、`MediaCascadePlatform`、`MediaCascadeBinding`。
- PostgreSQL 负责保存配置、设备目录、分屏方案和级联绑定；Redis 负责设备注册态、心跳、播放会话 TTL、ZLM hook 运行态和网关缓存。
- 扩展 `MediaProperties`，仅保留应用级开关、回调基址、默认播放协议、默认截图业务类型、网关线程池和 hook 校验配置；设备、网关、流媒体具体配置全部落库。
- 补齐 `flexboot4-media-starter/src/main/resources/db/init.sql` 与 `flexboot4-media-starter/src/main/resources/db/menu_data.sql`，菜单新增 `视频中心/流媒体服务/视频网关/视频设备/分屏展示/国标级联`，权限码统一使用 `media:*`。
- 用 Spring `RestClient` 封装 `ZlmClient`，实现流媒体连通性测试、流列表查询、流关闭、hook 验签和播放地址生成；播放地址统一由后台签发，优先返回 `ws-flv/http-flv`，HLS 作为回退。
- 新增 ZLM hook 控制器，接收流注册、无人观看、流关闭等事件，回写 `MediaStreamSession` 和通道流状态。
- 采用 JAIN-SIP 作为嵌入式 SIP 核心，在 `media-starter` 内启动单节点、单活动 GB28181 网关实例；UDP 端口、SIP ID、SIP 域、对外地址、收流端口范围从 `MediaGateway` 配置装载。
- `MediaGateway` 负责注册、心跳、目录订阅、设备状态同步、实时流 INVITE/BYE、录像回放 INVITE、PTZ 指令和上级平台级联注册。
- `MediaDevice` 同时支持 `GB28181` 和 `FIXED_ADDRESS` 两类接入；GB28181 设备注册后自动建档并同步通道目录，固定地址设备允许手工维护多条 RTSP/RTMP 通道。
- 截图能力采用前端播放器帧抓取后上传，落到现有文件体系，`bizType` 固定为 `media_snapshot`，不引入 ffmpeg 运行时依赖。
- `MediaScreen`/`MediaScreenSlot` 存储单屏、四分屏、九分屏和自定义布局的通道绑定，支持保存方案、回显和全屏。
- `MediaCascadePlatform` 保存上级平台 SIP 配置、认证、心跳周期和厂商信息；`MediaCascadeBinding` 保存被推送通道与国标 ID；级联只实现目录推送、通道绑定、启停和实时/回放透传，不扩展告警级联与本地录像托管。
- 后端全部落在 `flexboot4-media-starter`，前端全部落在 `flexboot4-web/apps/web-antd/src/api/media` 与 `flexboot4-web/apps/web-antd/src/views/media`。

## 实施顺序
1. 完成 DDL、菜单、权限、实体、Mapper、Service 骨架。
2. 完成 `MediaServer`、`ZlmClient` 和 ZLM hook 闭环。
3. 完成 `MediaGateway` 嵌入式 SIP 运行时和 GB28181 注册、目录同步。
4. 完成 RTSP 固定地址设备与通道管理。
5. 完成实时预览、录像回放、PTZ、截图。
6. 完成分屏展示。
7. 完成国标级联。
8. 完成联调加固、异常恢复、示例配置和使用文档。

## 新增接口与类型
- 后台控制器固定为 `/api/admin/media/server`、`/api/admin/media/gateway`、`/api/admin/media/device`、`/api/admin/media/channel`、`/api/admin/media/screen`、`/api/admin/media/cascade`、`/api/admin/media/zlm/hook/*`。
- 关键 DTO 固定为 `MediaServerTestRequest`、`GatewayReloadRequest`、`ChannelLiveRequest`、`PlaybackQueryRequest`、`PlaybackStartRequest`、`PtzControlRequest`、`ScreenSaveRequest`、`CascadeBindRequest`。
- 前端页面固定为 `流媒体服务`、`视频网关`、`视频设备列表/详情`、`分屏展示`、`国标级联`；设备详情页内集成通道树、实时播放、回放、PTZ 和截图。

## 测试与验收
- 单元/集成测试覆盖 ZLM API 客户端、hook 验签、SIP 消息编解码、设备状态机、通道同步、分屏存取和级联绑定。
- 联调验收必须全部通过：ZLMediaKit 服务测试成功并能回写 hook；GB28181 设备可注册上线并同步通道；RTSP 设备可手工建通道并播放；GB28181 通道可直播、停播、查询回放并开始/停止回放；云台控制在支持设备上可用，不支持时前端明确禁用；分屏方案可保存、回显、切换 1/4/9 屏；级联平台可注册成功并让上级平台看到已绑定通道；ZLM 不可达、设备离线、网关重启、会话超时后状态可恢复或清理，不产生僵尸流。

## 默认假设与参考
- 默认按单应用节点设计，同一节点只允许运行一个活动 GB28181 网关实例。
- 默认不建设 JetLinks 式通用产品/协议/网络组件平台层，视频中心直接使用视频专用聚合模型。
- 默认不做本地录像存储、AI 分析和告警联动；录像回放来源于设备本地或设备/平台云端记录。
- 参考基线：[视频设备 / 分屏展示 / 国标级联](https://doc.jetlinks.cn/Video_Center/Video_equipment10.html)；[视频接入流程](https://doc.jetlinks.cn/media-guide/media_access_process.html)；[设备接入网关与流媒体服务配置字段](https://doc.jetlinks.cn/Mocha_ITOM/Device_access_gateway5.1.html)；[JetLinks 企业版初始化中对 `jetlinks-media` 的说明](https://doc.jetlinks.cn/install-deployment/enterprise-version-start.html)。
