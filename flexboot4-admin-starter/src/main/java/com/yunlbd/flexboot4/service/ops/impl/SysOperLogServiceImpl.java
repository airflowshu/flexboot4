package com.yunlbd.flexboot4.service.ops.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yunlbd.flexboot4.entity.ops.SysOperLog;
import com.yunlbd.flexboot4.mapper.SysOperLogMapper;
import com.yunlbd.flexboot4.service.ops.SysOperLogService;
import org.springframework.stereotype.Service;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

}
