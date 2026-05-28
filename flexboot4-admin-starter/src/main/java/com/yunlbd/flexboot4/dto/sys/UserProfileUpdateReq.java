package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateReq {
    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String realName;

    @Size(max = 64, message = "头像文件ID长度不能超过64个字符")
    private String profileFileId;

    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String remark;
}
