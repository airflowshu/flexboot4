INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_root', null, '/media', 'Media', 'BasicLayout', 'media.title', 'ant-design:video-camera-outlined', 96,
        false, false, 'catalog', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_server_menu', 'media_root', '/media/server', 'MediaServer', '/media/server/index',
        'media.server.title', 'ant-design:cloud-server-outlined', 1,
        false, true, 'menu', 'media:server:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_gateway_menu', 'media_root', '/media/gateway', 'MediaGateway', '/media/gateway/index',
        'media.gateway.title', 'ant-design:api-outlined', 2,
        false, true, 'menu', 'media:gateway:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_device_menu', 'media_root', '/media/device', 'MediaDevice', '/media/device/index',
        'media.device.title', 'ant-design:deployment-unit-outlined', 3,
        false, true, 'menu', 'media:device:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_screen_menu', 'media_root', '/media/screen', 'MediaScreen', '/media/screen/index',
        'media.screen.title', 'ant-design:appstore-outlined', 4,
        false, true, 'menu', 'media:screen:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_cascade_menu', 'media_root', '/media/cascade', 'MediaCascade', '/media/cascade/index',
        'media.cascade.title', 'ant-design:branches-outlined', 5,
        false, true, 'menu', 'media:cascade:platform:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES
('media_server_add_btn', 'media_server_menu', '', 'MediaServerAdd', '', 'common.add', '', 10, true, false, 'button', 'media:server:add', 1, now(), now(), 0, 0),
('media_server_edit_btn', 'media_server_menu', '', 'MediaServerEdit', '', 'common.edit', '', 20, true, false, 'button', 'media:server:edit', 1, now(), now(), 0, 0),
('media_server_delete_btn', 'media_server_menu', '', 'MediaServerDelete', '', 'common.delete', '', 30, true, false, 'button', 'media:server:delete', 1, now(), now(), 0, 0),
('media_server_export_btn', 'media_server_menu', '', 'MediaServerExport', '', 'common.export', '', 40, true, false, 'button', 'media:server:export', 1, now(), now(), 0, 0),
('media_server_import_btn', 'media_server_menu', '', 'MediaServerImport', '', 'common.import', '', 50, true, false, 'button', 'media:server:import', 1, now(), now(), 0, 0),
('media_server_test_btn', 'media_server_menu', '', 'MediaServerTest', '', 'media.server.test', '', 90, true, false, 'button', 'media:server:test', 1, now(), now(), 0, 0),
('media_gateway_add_btn', 'media_gateway_menu', '', 'MediaGatewayAdd', '', 'common.add', '', 10, true, false, 'button', 'media:gateway:add', 1, now(), now(), 0, 0),
('media_gateway_edit_btn', 'media_gateway_menu', '', 'MediaGatewayEdit', '', 'common.edit', '', 20, true, false, 'button', 'media:gateway:edit', 1, now(), now(), 0, 0),
('media_gateway_delete_btn', 'media_gateway_menu', '', 'MediaGatewayDelete', '', 'common.delete', '', 30, true, false, 'button', 'media:gateway:delete', 1, now(), now(), 0, 0),
('media_gateway_export_btn', 'media_gateway_menu', '', 'MediaGatewayExport', '', 'common.export', '', 40, true, false, 'button', 'media:gateway:export', 1, now(), now(), 0, 0),
('media_gateway_import_btn', 'media_gateway_menu', '', 'MediaGatewayImport', '', 'common.import', '', 50, true, false, 'button', 'media:gateway:import', 1, now(), now(), 0, 0),
('media_gateway_reload_btn', 'media_gateway_menu', '', 'MediaGatewayReload', '', 'media.gateway.reload', '', 91, true, false, 'button', 'media:gateway:reload', 1, now(), now(), 0, 0),
('media_device_add_btn', 'media_device_menu', '', 'MediaDeviceAdd', '', 'common.add', '', 10, true, false, 'button', 'media:device:add', 1, now(), now(), 0, 0),
('media_device_edit_btn', 'media_device_menu', '', 'MediaDeviceEdit', '', 'common.edit', '', 20, true, false, 'button', 'media:device:edit', 1, now(), now(), 0, 0),
('media_device_delete_btn', 'media_device_menu', '', 'MediaDeviceDelete', '', 'common.delete', '', 30, true, false, 'button', 'media:device:delete', 1, now(), now(), 0, 0),
('media_device_export_btn', 'media_device_menu', '', 'MediaDeviceExport', '', 'common.export', '', 40, true, false, 'button', 'media:device:export', 1, now(), now(), 0, 0),
('media_device_import_btn', 'media_device_menu', '', 'MediaDeviceImport', '', 'common.import', '', 50, true, false, 'button', 'media:device:import', 1, now(), now(), 0, 0),
('media_channel_list_btn', 'media_device_menu', '', 'MediaChannelList', '', 'media.channel.list', '', 60, true, false, 'button', 'media:channel:list', 1, now(), now(), 0, 0),
('media_channel_add_btn', 'media_device_menu', '', 'MediaChannelAdd', '', 'common.add', '', 61, true, false, 'button', 'media:channel:add', 1, now(), now(), 0, 0),
('media_channel_edit_btn', 'media_device_menu', '', 'MediaChannelEdit', '', 'common.edit', '', 62, true, false, 'button', 'media:channel:edit', 1, now(), now(), 0, 0),
('media_channel_delete_btn', 'media_device_menu', '', 'MediaChannelDelete', '', 'common.delete', '', 63, true, false, 'button', 'media:channel:delete', 1, now(), now(), 0, 0),
('media_channel_export_btn', 'media_device_menu', '', 'MediaChannelExport', '', 'common.export', '', 64, true, false, 'button', 'media:channel:export', 1, now(), now(), 0, 0),
('media_channel_import_btn', 'media_device_menu', '', 'MediaChannelImport', '', 'common.import', '', 65, true, false, 'button', 'media:channel:import', 1, now(), now(), 0, 0),
('media_channel_live_btn', 'media_device_menu', '', 'MediaChannelLive', '', 'media.device.live', '', 92, true, false, 'button', 'media:channel:live', 1, now(), now(), 0, 0),
('media_channel_playback_btn', 'media_device_menu', '', 'MediaChannelPlayback', '', 'media.device.playback', '', 93, true, false, 'button', 'media:channel:playback', 1, now(), now(), 0, 0),
('media_channel_ptz_btn', 'media_device_menu', '', 'MediaChannelPtz', '', 'media.device.ptz', '', 94, true, false, 'button', 'media:channel:ptz', 1, now(), now(), 0, 0),
('media_screen_add_btn', 'media_screen_menu', '', 'MediaScreenAdd', '', 'common.add', '', 10, true, false, 'button', 'media:screen:add', 1, now(), now(), 0, 0),
('media_screen_edit_btn', 'media_screen_menu', '', 'MediaScreenEdit', '', 'common.edit', '', 20, true, false, 'button', 'media:screen:edit', 1, now(), now(), 0, 0),
('media_screen_delete_btn', 'media_screen_menu', '', 'MediaScreenDelete', '', 'common.delete', '', 30, true, false, 'button', 'media:screen:delete', 1, now(), now(), 0, 0),
('media_screen_export_btn', 'media_screen_menu', '', 'MediaScreenExport', '', 'common.export', '', 40, true, false, 'button', 'media:screen:export', 1, now(), now(), 0, 0),
('media_screen_import_btn', 'media_screen_menu', '', 'MediaScreenImport', '', 'common.import', '', 50, true, false, 'button', 'media:screen:import', 1, now(), now(), 0, 0),
('media_screen_save_btn', 'media_screen_menu', '', 'MediaScreenSave', '', 'media.screen.save', '', 95, true, false, 'button', 'media:screen:save', 1, now(), now(), 0, 0),
('media_cascade_platform_add_btn', 'media_cascade_menu', '', 'MediaCascadePlatformAdd', '', 'common.add', '', 10, true, false, 'button', 'media:cascade:platform:add', 1, now(), now(), 0, 0),
('media_cascade_platform_edit_btn', 'media_cascade_menu', '', 'MediaCascadePlatformEdit', '', 'common.edit', '', 20, true, false, 'button', 'media:cascade:platform:edit', 1, now(), now(), 0, 0),
('media_cascade_platform_delete_btn', 'media_cascade_menu', '', 'MediaCascadePlatformDelete', '', 'common.delete', '', 30, true, false, 'button', 'media:cascade:platform:delete', 1, now(), now(), 0, 0),
('media_cascade_platform_export_btn', 'media_cascade_menu', '', 'MediaCascadePlatformExport', '', 'common.export', '', 40, true, false, 'button', 'media:cascade:platform:export', 1, now(), now(), 0, 0),
('media_cascade_platform_import_btn', 'media_cascade_menu', '', 'MediaCascadePlatformImport', '', 'common.import', '', 50, true, false, 'button', 'media:cascade:platform:import', 1, now(), now(), 0, 0),
('media_cascade_bindings_btn', 'media_cascade_menu', '', 'MediaCascadeBindings', '', 'media.cascade.bindings', '', 90, true, false, 'button', 'media:cascade:list', 1, now(), now(), 0, 0),
('media_cascade_bind_btn', 'media_cascade_menu', '', 'MediaCascadeBind', '', 'media.cascade.bind', '', 96, true, false, 'button', 'media:cascade:bind', 1, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    path = '',
    name = EXCLUDED.name,
    component = '',
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    order_no = EXCLUDED.order_no,
    hide_in_menu = true,
    keep_alive = false,
    type = 'button',
    auth_code = EXCLUDED.auth_code,
    status = EXCLUDED.status,
    last_modify_time = now();
