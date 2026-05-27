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
        false, true, 'menu', 'media:cascade:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_server_test_btn', 'media_server_menu', '', 'MediaServerTest', '',
        'media.server.test', '', 90,
        true, false, 'button', 'media:server:test', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_gateway_reload_btn', 'media_gateway_menu', '', 'MediaGatewayReload', '',
        'media.gateway.reload', '', 91,
        true, false, 'button', 'media:gateway:reload', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_channel_live_btn', 'media_device_menu', '', 'MediaChannelLive', '',
        'media.device.live', '', 92,
        true, false, 'button', 'media:channel:live', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_channel_playback_btn', 'media_device_menu', '', 'MediaChannelPlayback', '',
        'media.device.playback', '', 93,
        true, false, 'button', 'media:channel:playback', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_channel_ptz_btn', 'media_device_menu', '', 'MediaChannelPtz', '',
        'media.device.ptz', '', 94,
        true, false, 'button', 'media:channel:ptz', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_screen_save_btn', 'media_screen_menu', '', 'MediaScreenSave', '',
        'media.screen.save', '', 95,
        true, false, 'button', 'media:screen:save', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('media_cascade_bind_btn', 'media_cascade_menu', '', 'MediaCascadeBind', '',
        'media.cascade.bind', '', 96,
        true, false, 'button', 'media:cascade:bind', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;
