package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaScreenSlot;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.List;

public interface MediaScreenSlotService extends IExtendedService<MediaScreenSlot> {

    List<MediaScreenSlot> listByScreenId(String screenId);

    boolean deleteByScreenId(String screenId);
}
