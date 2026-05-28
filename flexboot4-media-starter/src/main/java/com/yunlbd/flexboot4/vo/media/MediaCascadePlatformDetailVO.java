package com.yunlbd.flexboot4.vo.media;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaCascadePlatformDetailVO extends MediaCascadePlatformListVO {
    private List<MediaCascadeBindingVO> bindings;
}
