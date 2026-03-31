package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.media.core.ZlmClient;
import com.yunlbd.flexboot4.media.dto.MediaServerTestRequest;
import com.yunlbd.flexboot4.media.dto.MediaServerTestResult;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MediaServerService extends IExtendedService<MediaServer> {

    MediaServerTestResult testConnection(MediaServerTestRequest request);

    List<Map<String, Object>> listStreams(String serverId, String app, String stream);

    boolean closeStream(String serverId, String app, String stream, boolean force);

    Map<String, String> buildPlayUrls(String serverId, String app, String stream);

    void markHookAlive(String mediaServerId, LocalDateTime hookTime);

    ZlmClient createClient(MediaServer server);
}
