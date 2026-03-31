package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaScreen;
import com.yunlbd.flexboot4.media.dto.MediaScreenDetail;
import com.yunlbd.flexboot4.media.dto.ScreenSaveRequest;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

public interface MediaScreenService extends IExtendedService<MediaScreen> {

    MediaScreenDetail getDetail(String screenId);

    MediaScreenDetail saveScreen(ScreenSaveRequest request);
}
