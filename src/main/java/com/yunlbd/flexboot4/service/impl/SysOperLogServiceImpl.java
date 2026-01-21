package com.yunlbd.flexboot4.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.mapper.SysOperLogMapper;
import com.yunlbd.flexboot4.service.SysOperLogService;
import org.springframework.stereotype.Service;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

}
