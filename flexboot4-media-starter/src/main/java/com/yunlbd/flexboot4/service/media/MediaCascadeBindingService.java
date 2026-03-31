package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaCascadeBinding;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.List;

public interface MediaCascadeBindingService extends IExtendedService<MediaCascadeBinding> {

    List<MediaCascadeBinding> listByPlatformId(String platformId);

    boolean deleteByPlatformId(String platformId);
}
