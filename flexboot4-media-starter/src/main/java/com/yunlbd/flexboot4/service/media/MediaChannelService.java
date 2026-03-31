package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.media.dto.ChannelLiveRequest;
import com.yunlbd.flexboot4.media.dto.MediaPlayResponse;
import com.yunlbd.flexboot4.media.dto.PlaybackQueryRequest;
import com.yunlbd.flexboot4.media.dto.PlaybackRecordItem;
import com.yunlbd.flexboot4.media.dto.PlaybackStartRequest;
import com.yunlbd.flexboot4.media.dto.PtzControlRequest;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.time.LocalDateTime;
import java.util.List;

public interface MediaChannelService extends IExtendedService<MediaChannel> {

    List<MediaChannel> listByDeviceId(String deviceId);

    MediaChannel upsertChannel(MediaChannel channel);

    void markChannelStatus(String channelCode, String status, LocalDateTime time);

    MediaPlayResponse startLive(ChannelLiveRequest request);

    boolean stopLive(String sessionId);

    List<PlaybackRecordItem> queryPlayback(PlaybackQueryRequest request);

    MediaPlayResponse startPlayback(PlaybackStartRequest request);

    boolean stopPlayback(String sessionId);

    boolean ptzControl(PtzControlRequest request);
}
