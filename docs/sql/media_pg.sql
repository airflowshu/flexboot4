CREATE TABLE IF NOT EXISTS media_server (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    server_name VARCHAR(128) NOT NULL,
    server_type VARCHAR(32) NOT NULL DEFAULT 'ZLMEDIAKIT',
    base_url VARCHAR(255) NOT NULL,
    api_secret VARCHAR(128),
    hook_secret VARCHAR(128),
    public_host VARCHAR(255),
    play_domain VARCHAR(255),
    rtp_ip VARCHAR(64),
    rtp_port_start INTEGER,
    rtp_port_end INTEGER,
    default_stream_app VARCHAR(64) DEFAULT 'rtp',
    hook_enabled BOOLEAN DEFAULT TRUE,
    enabled BOOLEAN DEFAULT TRUE,
    status VARCHAR(32) DEFAULT 'UNKNOWN',
    last_test_time TIMESTAMP,
    last_hook_time TIMESTAMP,
    last_error TEXT
);

COMMENT ON COLUMN media_server.id IS '主键ID';
COMMENT ON COLUMN media_server.version IS '版本号';
COMMENT ON COLUMN media_server.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_server.create_time IS '创建时间';
COMMENT ON COLUMN media_server.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_server.create_by IS '创建人';
COMMENT ON COLUMN media_server.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_server.remark IS '备注';
COMMENT ON COLUMN media_server.server_name IS '服务器名称';
COMMENT ON COLUMN media_server.server_type IS '服务器类型(ZLMEDIAKIT等)';
COMMENT ON COLUMN media_server.base_url IS '服务器基础URL';
COMMENT ON COLUMN media_server.api_secret IS 'API密钥';
COMMENT ON COLUMN media_server.hook_secret IS 'Hook密钥';
COMMENT ON COLUMN media_server.public_host IS '公网主机地址';
COMMENT ON COLUMN media_server.play_domain IS '播放域名';
COMMENT ON COLUMN media_server.rtp_ip IS 'RTP服务IP地址';
COMMENT ON COLUMN media_server.rtp_port_start IS 'RTP端口起始值';
COMMENT ON COLUMN media_server.rtp_port_end IS 'RTP端口结束值';
COMMENT ON COLUMN media_server.default_stream_app IS '默认流应用名';
COMMENT ON COLUMN media_server.hook_enabled IS '是否启用Hook';
COMMENT ON COLUMN media_server.enabled IS '是否启用';
COMMENT ON COLUMN media_server.status IS '状态(UNKNOWN/ONLINE/OFFLINE)';
COMMENT ON COLUMN media_server.last_test_time IS '最后测试时间';
COMMENT ON COLUMN media_server.last_hook_time IS '最后Hook时间';
COMMENT ON COLUMN media_server.last_error IS '最后错误信息';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_server_name ON media_server (server_name) WHERE del_flag = 0;

CREATE TABLE IF NOT EXISTS media_gateway (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    server_id VARCHAR(64),
    gateway_name VARCHAR(128) NOT NULL,
    gateway_code VARCHAR(64) NOT NULL,
    sip_id VARCHAR(64) NOT NULL,
    sip_domain VARCHAR(64) NOT NULL,
    sip_password VARCHAR(128),
    local_ip VARCHAR(64),
    local_port INTEGER DEFAULT 5060,
    public_ip VARCHAR(64),
    public_port INTEGER DEFAULT 5060,
    transport VARCHAR(16) DEFAULT 'UDP',
    rtp_ip VARCHAR(64),
    rtp_port_start INTEGER,
    rtp_port_end INTEGER,
    heartbeat_interval_seconds INTEGER DEFAULT 60,
    register_expires_seconds INTEGER DEFAULT 3600,
    catalog_subscribe_cycle_seconds INTEGER DEFAULT 300,
    thread_pool_size INTEGER DEFAULT 2,
    enabled BOOLEAN DEFAULT TRUE,
    active BOOLEAN DEFAULT FALSE,
    runtime_status VARCHAR(32) DEFAULT 'STOPPED',
    last_start_time TIMESTAMP,
    last_stop_time TIMESTAMP,
    last_keepalive_time TIMESTAMP,
    last_error TEXT
);

COMMENT ON COLUMN media_gateway.id IS '主键ID';
COMMENT ON COLUMN media_gateway.version IS '版本号';
COMMENT ON COLUMN media_gateway.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_gateway.create_time IS '创建时间';
COMMENT ON COLUMN media_gateway.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_gateway.create_by IS '创建人';
COMMENT ON COLUMN media_gateway.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_gateway.remark IS '备注';
COMMENT ON COLUMN media_gateway.server_id IS '关联的媒体服务器ID';
COMMENT ON COLUMN media_gateway.gateway_name IS '网关名称';
COMMENT ON COLUMN media_gateway.gateway_code IS '网关编码';
COMMENT ON COLUMN media_gateway.sip_id IS 'SIP设备ID';
COMMENT ON COLUMN media_gateway.sip_domain IS 'SIP域名';
COMMENT ON COLUMN media_gateway.sip_password IS 'SIP密码';
COMMENT ON COLUMN media_gateway.local_ip IS '本地IP地址';
COMMENT ON COLUMN media_gateway.local_port IS '本地SIP端口';
COMMENT ON COLUMN media_gateway.public_ip IS '公网IP地址';
COMMENT ON COLUMN media_gateway.public_port IS '公网SIP端口';
COMMENT ON COLUMN media_gateway.transport IS '传输协议(UDP/TCP)';
COMMENT ON COLUMN media_gateway.rtp_ip IS 'RTP流接收IP地址';
COMMENT ON COLUMN media_gateway.rtp_port_start IS 'RTP端口起始值';
COMMENT ON COLUMN media_gateway.rtp_port_end IS 'RTP端口结束值';
COMMENT ON COLUMN media_gateway.heartbeat_interval_seconds IS '心跳间隔秒数';
COMMENT ON COLUMN media_gateway.register_expires_seconds IS '注册过期秒数';
COMMENT ON COLUMN media_gateway.catalog_subscribe_cycle_seconds IS '目录订阅周期秒数';
COMMENT ON COLUMN media_gateway.thread_pool_size IS '线程池大小';
COMMENT ON COLUMN media_gateway.enabled IS '是否启用';
COMMENT ON COLUMN media_gateway.active IS '是否激活';
COMMENT ON COLUMN media_gateway.runtime_status IS '运行时状态(STOPPED/RUNNING等)';
COMMENT ON COLUMN media_gateway.last_start_time IS '最后启动时间';
COMMENT ON COLUMN media_gateway.last_stop_time IS '最后停止时间';
COMMENT ON COLUMN media_gateway.last_keepalive_time IS '最后心跳时间';
COMMENT ON COLUMN media_gateway.last_error IS '最后错误信息';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_gateway_code ON media_gateway (gateway_code) WHERE del_flag = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_media_gateway_sip_id ON media_gateway (sip_id) WHERE del_flag = 0;

CREATE TABLE IF NOT EXISTS media_device (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    server_id VARCHAR(64),
    gateway_id VARCHAR(64),
    device_name VARCHAR(128) NOT NULL,
    device_code VARCHAR(64) NOT NULL,
    access_type VARCHAR(32) NOT NULL DEFAULT 'GB28181',
    manufacturer VARCHAR(64),
    model VARCHAR(64),
    owner VARCHAR(64),
    civil_code VARCHAR(64),
    address VARCHAR(255),
    ip VARCHAR(64),
    port INTEGER,
    username VARCHAR(64),
    password VARCHAR(128),
    media_url VARCHAR(512),
    stream_mode VARCHAR(32) DEFAULT 'AUTO',
    online_status VARCHAR(32) DEFAULT 'OFFLINE',
    register_status VARCHAR(32) DEFAULT 'UNKNOWN',
    last_register_time TIMESTAMP,
    last_keepalive_time TIMESTAMP,
    last_catalog_time TIMESTAMP
);

COMMENT ON COLUMN media_device.id IS '主键ID';
COMMENT ON COLUMN media_device.version IS '版本号';
COMMENT ON COLUMN media_device.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_device.create_time IS '创建时间';
COMMENT ON COLUMN media_device.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_device.create_by IS '创建人';
COMMENT ON COLUMN media_device.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_device.remark IS '备注';
COMMENT ON COLUMN media_device.server_id IS '关联的媒体服务器ID';
COMMENT ON COLUMN media_device.gateway_id IS '关联的网关ID';
COMMENT ON COLUMN media_device.device_name IS '设备名称';
COMMENT ON COLUMN media_device.device_code IS '设备编码(国标编码)';
COMMENT ON COLUMN media_device.access_type IS '接入类型(GB28181等)';
COMMENT ON COLUMN media_device.manufacturer IS '生产厂商';
COMMENT ON COLUMN media_device.model IS '设备型号';
COMMENT ON COLUMN media_device.owner IS '设备归属人';
COMMENT ON COLUMN media_device.civil_code IS '行政区域编码';
COMMENT ON COLUMN media_device.address IS '设备地址';
COMMENT ON COLUMN media_device.ip IS '设备IP地址';
COMMENT ON COLUMN media_device.port IS '设备端口';
COMMENT ON COLUMN media_device.username IS '用户名';
COMMENT ON COLUMN media_device.password IS '密码';
COMMENT ON COLUMN media_device.media_url IS '媒体流URL';
COMMENT ON COLUMN media_device.stream_mode IS '流模式(AUTO/UDP/TCP)';
COMMENT ON COLUMN media_device.online_status IS '在线状态(ONLINE/OFFLINE)';
COMMENT ON COLUMN media_device.register_status IS '注册状态';
COMMENT ON COLUMN media_device.last_register_time IS '最后注册时间';
COMMENT ON COLUMN media_device.last_keepalive_time IS '最后心跳时间';
COMMENT ON COLUMN media_device.last_catalog_time IS '最后目录同步时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_device_code ON media_device (device_code) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_media_device_gateway ON media_device (gateway_id);
CREATE INDEX IF NOT EXISTS idx_media_device_server ON media_device (server_id);

CREATE TABLE IF NOT EXISTS media_channel (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    server_id VARCHAR(64),
    gateway_id VARCHAR(64),
    device_id VARCHAR(64) NOT NULL,
    parent_channel_id VARCHAR(64),
    channel_name VARCHAR(128) NOT NULL,
    channel_code VARCHAR(64) NOT NULL,
    channel_type VARCHAR(32) DEFAULT 'VIDEO',
    manufacturer VARCHAR(64),
    model VARCHAR(64),
    owner VARCHAR(64),
    civil_code VARCHAR(64),
    address VARCHAR(255),
    ptz_type VARCHAR(32),
    has_record BOOLEAN DEFAULT FALSE,
    status VARCHAR(32) DEFAULT 'OFFLINE',
    play_status VARCHAR(32) DEFAULT 'IDLE',
    longitude VARCHAR(32),
    latitude VARCHAR(32),
    fixed_url VARCHAR(512),
    stream_app VARCHAR(64),
    stream_id VARCHAR(128),
    last_play_time TIMESTAMP,
    last_offline_time TIMESTAMP
);

COMMENT ON COLUMN media_channel.id IS '主键ID';
COMMENT ON COLUMN media_channel.version IS '版本号';
COMMENT ON COLUMN media_channel.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_channel.create_time IS '创建时间';
COMMENT ON COLUMN media_channel.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_channel.create_by IS '创建人';
COMMENT ON COLUMN media_channel.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_channel.remark IS '备注';
COMMENT ON COLUMN media_channel.server_id IS '关联的媒体服务器ID';
COMMENT ON COLUMN media_channel.gateway_id IS '关联的网关ID';
COMMENT ON COLUMN media_channel.device_id IS '关联的设备ID';
COMMENT ON COLUMN media_channel.parent_channel_id IS '父通道ID';
COMMENT ON COLUMN media_channel.channel_name IS '通道名称';
COMMENT ON COLUMN media_channel.channel_code IS '通道编码(国标编码)';
COMMENT ON COLUMN media_channel.channel_type IS '通道类型(VIDEO/AUDIO)';
COMMENT ON COLUMN media_channel.manufacturer IS '生产厂商';
COMMENT ON COLUMN media_channel.model IS '设备型号';
COMMENT ON COLUMN media_channel.owner IS '归属人';
COMMENT ON COLUMN media_channel.civil_code IS '行政区域编码';
COMMENT ON COLUMN media_channel.address IS '通道地址';
COMMENT ON COLUMN media_channel.ptz_type IS '云台类型(NONE/PTZ/FASTBALL等)';
COMMENT ON COLUMN media_channel.has_record IS '是否有录像';
COMMENT ON COLUMN media_channel.status IS '通道状态(ONLINE/OFFLINE)';
COMMENT ON COLUMN media_channel.play_status IS '播放状态(IDLE/PLAYING/PAUSED)';
COMMENT ON COLUMN media_channel.longitude IS '经度';
COMMENT ON COLUMN media_channel.latitude IS '纬度';
COMMENT ON COLUMN media_channel.fixed_url IS '固定播放URL';
COMMENT ON COLUMN media_channel.stream_app IS '流应用名';
COMMENT ON COLUMN media_channel.stream_id IS '流ID';
COMMENT ON COLUMN media_channel.last_play_time IS '最后播放时间';
COMMENT ON COLUMN media_channel.last_offline_time IS '最后离线时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_channel_code ON media_channel (channel_code) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_media_channel_device ON media_channel (device_id);
CREATE INDEX IF NOT EXISTS idx_media_channel_stream ON media_channel (stream_app, stream_id);

CREATE TABLE IF NOT EXISTS media_stream_session (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    server_id VARCHAR(64),
    gateway_id VARCHAR(64),
    device_id VARCHAR(64),
    channel_id VARCHAR(64),
    session_type VARCHAR(32) NOT NULL,
    stream_app VARCHAR(64),
    stream_id VARCHAR(128),
    play_protocol VARCHAR(32),
    play_url VARCHAR(1024),
    proxy_key VARCHAR(128),
    ssrc VARCHAR(32),
    dialog_id VARCHAR(128),
    rtp_port INTEGER,
    viewer_count INTEGER DEFAULT 0,
    started_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_time TIMESTAMP,
    status VARCHAR(32) DEFAULT 'PENDING'
);

COMMENT ON COLUMN media_stream_session.id IS '主键ID';
COMMENT ON COLUMN media_stream_session.version IS '版本号';
COMMENT ON COLUMN media_stream_session.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_stream_session.create_time IS '创建时间';
COMMENT ON COLUMN media_stream_session.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_stream_session.create_by IS '创建人';
COMMENT ON COLUMN media_stream_session.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_stream_session.remark IS '备注';
COMMENT ON COLUMN media_stream_session.server_id IS '关联的媒体服务器ID';
COMMENT ON COLUMN media_stream_session.gateway_id IS '关联的网关ID';
COMMENT ON COLUMN media_stream_session.device_id IS '关联的设备ID';
COMMENT ON COLUMN media_stream_session.channel_id IS '关联的通道ID';
COMMENT ON COLUMN media_stream_session.session_type IS '会话类型(LIVE/PLAYBACK)';
COMMENT ON COLUMN media_stream_session.stream_app IS '流应用名';
COMMENT ON COLUMN media_stream_session.stream_id IS '流ID';
COMMENT ON COLUMN media_stream_session.play_protocol IS '播放协议(RTMP/HLS/WEBSOCKET等)';
COMMENT ON COLUMN media_stream_session.play_url IS '播放URL';
COMMENT ON COLUMN media_stream_session.proxy_key IS '代理密钥';
COMMENT ON COLUMN media_stream_session.ssrc IS 'SSRC标识';
COMMENT ON COLUMN media_stream_session.dialog_id IS 'SIP对话ID';
COMMENT ON COLUMN media_stream_session.rtp_port IS 'RTP端口';
COMMENT ON COLUMN media_stream_session.viewer_count IS '观看人数';
COMMENT ON COLUMN media_stream_session.started_time IS '会话开始时间';
COMMENT ON COLUMN media_stream_session.ended_time IS '会话结束时间';
COMMENT ON COLUMN media_stream_session.status IS '会话状态(PENDING/ACTIVE/ENDED)';



CREATE INDEX IF NOT EXISTS idx_media_stream_session_channel ON media_stream_session (channel_id, status);
CREATE INDEX IF NOT EXISTS idx_media_stream_session_stream ON media_stream_session (stream_app, stream_id);


CREATE TABLE IF NOT EXISTS media_screen (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    screen_name VARCHAR(128) NOT NULL,
    layout_type VARCHAR(32) NOT NULL,
    layout_json TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE
);

COMMENT ON COLUMN media_screen.id IS '主键ID';
COMMENT ON COLUMN media_screen.version IS '版本号';
COMMENT ON COLUMN media_screen.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_screen.create_time IS '创建时间';
COMMENT ON COLUMN media_screen.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_screen.create_by IS '创建人';
COMMENT ON COLUMN media_screen.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_screen.remark IS '备注';
COMMENT ON COLUMN media_screen.screen_name IS '屏幕名称';
COMMENT ON COLUMN media_screen.layout_type IS '布局类型(1x1/2x2/3x3等)';
COMMENT ON COLUMN media_screen.layout_json IS '布局JSON配置';
COMMENT ON COLUMN media_screen.enabled IS '是否启用';
COMMENT ON COLUMN media_screen.is_default IS '是否为默认屏幕';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_screen_name ON media_screen (screen_name) WHERE del_flag = 0;

CREATE TABLE IF NOT EXISTS media_screen_slot (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    screen_id VARCHAR(64) NOT NULL,
    slot_index INTEGER NOT NULL,
    slot_name VARCHAR(64),
    x INTEGER DEFAULT 0,
    y INTEGER DEFAULT 0,
    width INTEGER DEFAULT 1,
    height INTEGER DEFAULT 1,
    channel_id VARCHAR(64),
    session_type VARCHAR(32) DEFAULT 'LIVE',
    options_json TEXT
);

COMMENT ON COLUMN media_screen_slot.id IS '主键ID';
COMMENT ON COLUMN media_screen_slot.version IS '版本号';
COMMENT ON COLUMN media_screen_slot.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_screen_slot.create_time IS '创建时间';
COMMENT ON COLUMN media_screen_slot.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_screen_slot.create_by IS '创建人';
COMMENT ON COLUMN media_screen_slot.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_screen_slot.remark IS '备注';
COMMENT ON COLUMN media_screen_slot.screen_id IS '关联的屏幕ID';
COMMENT ON COLUMN media_screen_slot.slot_index IS '槽位索引';
COMMENT ON COLUMN media_screen_slot.slot_name IS '槽位名称';
COMMENT ON COLUMN media_screen_slot.x IS 'X坐标';
COMMENT ON COLUMN media_screen_slot.y IS 'Y坐标';
COMMENT ON COLUMN media_screen_slot.width IS '宽度';
COMMENT ON COLUMN media_screen_slot.height IS '高度';
COMMENT ON COLUMN media_screen_slot.channel_id IS '关联的通道ID';
COMMENT ON COLUMN media_screen_slot.session_type IS '会话类型(LIVE/PLAYBACK)';
COMMENT ON COLUMN media_screen_slot.options_json IS '选项JSON配置';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_screen_slot ON media_screen_slot (screen_id, slot_index) WHERE del_flag = 0;

CREATE TABLE IF NOT EXISTS media_cascade_platform (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    server_id VARCHAR(64),
    gateway_id VARCHAR(64),
    platform_name VARCHAR(128) NOT NULL,
    platform_code VARCHAR(64) NOT NULL,
    sip_id VARCHAR(64) NOT NULL,
    sip_domain VARCHAR(64) NOT NULL,
    sip_password VARCHAR(128),
    host VARCHAR(128) NOT NULL,
    port INTEGER NOT NULL DEFAULT 5060,
    transport VARCHAR(16) DEFAULT 'UDP',
    manufacturer VARCHAR(64),
    enabled BOOLEAN DEFAULT TRUE,
    online_status VARCHAR(32) DEFAULT 'OFFLINE',
    heartbeat_interval_seconds INTEGER DEFAULT 60,
    register_expires_seconds INTEGER DEFAULT 3600,
    last_register_time TIMESTAMP,
    last_keepalive_time TIMESTAMP,
    last_error TEXT
);

COMMENT ON COLUMN media_cascade_platform.id IS '主键ID';
COMMENT ON COLUMN media_cascade_platform.version IS '版本号';
COMMENT ON COLUMN media_cascade_platform.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_cascade_platform.create_time IS '创建时间';
COMMENT ON COLUMN media_cascade_platform.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_cascade_platform.create_by IS '创建人';
COMMENT ON COLUMN media_cascade_platform.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_cascade_platform.remark IS '备注';
COMMENT ON COLUMN media_cascade_platform.server_id IS '关联的媒体服务器ID';
COMMENT ON COLUMN media_cascade_platform.gateway_id IS '关联的网关ID';
COMMENT ON COLUMN media_cascade_platform.platform_name IS '上级平台名称';
COMMENT ON COLUMN media_cascade_platform.platform_code IS '上级平台编码';
COMMENT ON COLUMN media_cascade_platform.sip_id IS 'SIP平台ID';
COMMENT ON COLUMN media_cascade_platform.sip_domain IS 'SIP域名';
COMMENT ON COLUMN media_cascade_platform.sip_password IS 'SIP密码';
COMMENT ON COLUMN media_cascade_platform.host IS '平台主机地址';
COMMENT ON COLUMN media_cascade_platform.port IS '平台端口';
COMMENT ON COLUMN media_cascade_platform.transport IS '传输协议(UDP/TCP)';
COMMENT ON COLUMN media_cascade_platform.manufacturer IS '生产厂商';
COMMENT ON COLUMN media_cascade_platform.enabled IS '是否启用';
COMMENT ON COLUMN media_cascade_platform.online_status IS '在线状态(ONLINE/OFFLINE)';
COMMENT ON COLUMN media_cascade_platform.heartbeat_interval_seconds IS '心跳间隔秒数';
COMMENT ON COLUMN media_cascade_platform.register_expires_seconds IS '注册过期秒数';
COMMENT ON COLUMN media_cascade_platform.last_register_time IS '最后注册时间';
COMMENT ON COLUMN media_cascade_platform.last_keepalive_time IS '最后心跳时间';
COMMENT ON COLUMN media_cascade_platform.last_error IS '最后错误信息';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_cascade_platform_code ON media_cascade_platform (platform_code) WHERE del_flag = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_media_cascade_platform_sip_id ON media_cascade_platform (sip_id) WHERE del_flag = 0;

CREATE TABLE IF NOT EXISTS media_cascade_binding (
    id VARCHAR(64) PRIMARY KEY,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark TEXT,
    platform_id VARCHAR(64) NOT NULL,
    channel_id VARCHAR(64) NOT NULL,
    gb_channel_code VARCHAR(64) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    live_enabled BOOLEAN DEFAULT TRUE,
    playback_enabled BOOLEAN DEFAULT TRUE
);

COMMENT ON COLUMN media_cascade_binding.id IS '主键ID';
COMMENT ON COLUMN media_cascade_binding.version IS '版本号';
COMMENT ON COLUMN media_cascade_binding.del_flag IS '删除标志(0:未删除,1:已删除)';
COMMENT ON COLUMN media_cascade_binding.create_time IS '创建时间';
COMMENT ON COLUMN media_cascade_binding.last_modify_time IS '最后修改时间';
COMMENT ON COLUMN media_cascade_binding.create_by IS '创建人';
COMMENT ON COLUMN media_cascade_binding.last_modify_by IS '最后修改人';
COMMENT ON COLUMN media_cascade_binding.remark IS '备注';
COMMENT ON COLUMN media_cascade_binding.platform_id IS '关联的上级平台ID';
COMMENT ON COLUMN media_cascade_binding.channel_id IS '关联的本级通道ID';
COMMENT ON COLUMN media_cascade_binding.gb_channel_code IS '国标通道编码';
COMMENT ON COLUMN media_cascade_binding.enabled IS '是否启用';
COMMENT ON COLUMN media_cascade_binding.live_enabled IS '是否启用直播';
COMMENT ON COLUMN media_cascade_binding.playback_enabled IS '是否启用回放';

CREATE UNIQUE INDEX IF NOT EXISTS uk_media_cascade_binding ON media_cascade_binding (platform_id, channel_id) WHERE del_flag = 0;
