package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaCascadePlatform;
import com.yunlbd.flexboot4.media.dto.CascadeBindRequest;
import com.yunlbd.flexboot4.media.dto.CascadeBindingView;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.List;

public interface MediaCascadePlatformService extends IExtendedService<MediaCascadePlatform> {

    List<CascadeBindingView> listBindings(String platformId);

    List<CascadeBindingView> bindChannels(CascadeBindRequest request);

    boolean registerPlatform(String platformId);

    boolean stopPlatform(String platformId);

    void markRegisterStatus(String platformId, boolean online, String error);
}
