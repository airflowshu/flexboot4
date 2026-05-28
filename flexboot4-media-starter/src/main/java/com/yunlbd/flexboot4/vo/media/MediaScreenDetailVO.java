package com.yunlbd.flexboot4.vo.media;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaScreenDetailVO extends MediaScreenListVO {
    private List<MediaScreenSlotVO> slots;
}
