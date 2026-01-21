package com.yunlbd.flexboot4.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.constant.SysConstant;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.event.SysOperLogEvent;
import com.yunlbd.flexboot4.util.IpUtils;
import com.yunlbd.flexboot4.util.SecurityUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class LogAspect {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    private static final ScopedValue<LocalDateTime> START_TIME = ScopedValue.newInstance();

    private LogAspect(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 环绕通知 - 记录方法开始时间
     *
     * @param joinPoint 切点
     * @param controllerLog 操作日志注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(controllerLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, OperLog controllerLog) throws Throwable {
        return ScopedValue.where(START_TIME, LocalDateTime.now())
                .call(joinPoint::proceed);
    }

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, OperLog controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     * 
     * @param joinPoint 切点
     * @param e 异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, OperLog controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, OperLog controllerLog, final Exception e, Object jsonResult) {
        try {
            // 获取当前请求对象
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // *========数据库日志=========*//
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0);
            
            // 请求的地址
            String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
            operLog.setOperIp(ip);
            operLog.setOperLocation(IpUtils.getRegion(ip));
            if (request != null) {
                operLog.setOperUrl(request.getRequestURI());
                operLog.setRequestMethod(request.getMethod());
            }

            if (e != null) {
                operLog.setStatus(SysConstant.SYS_ENUM_ERROR);
                operLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }

            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");

            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);

            // 参数长度限制，防止 Base64 等大字段导致日志膨胀
            if (operLog.getOperParam() != null) {
                String paramStr = operLog.getOperParam().toString();
                if (paramStr.length() > 5000) {
                    operLog.setOperParam(Map.of("note", "参数过长已省略，长度: " + paramStr.length()));
                }
            }

            // 设置操作人类别
            operLog.setOperatorType(controllerLog.operatorType().ordinal());

            // 设置当前用户
            try {
                String username = SecurityUtils.getSysUser() != null ? SecurityUtils.getSysUser().getUsername() : "未知";
                String userId = SecurityUtils.getSysUser() != null ? SecurityUtils.getSysUser().getId() : "未知";
                String deptId = SecurityUtils.getSysUser() != null ? SecurityUtils.getSysUser().getDeptId() : "未知";
                operLog.setOperName(username);
                operLog.setOperUserId(userId);
                operLog.setDeptId(deptId);
            } catch (Exception ex) {
                operLog.setOperName("未知");
            }

            // 设置操作时间
            LocalDateTime endTime = LocalDateTime.now();
            operLog.setOperTime(endTime);

            // 计算方法执行耗时（毫秒）
            LocalDateTime startTime = START_TIME.isBound() ? START_TIME.get() : null;
            if (startTime != null) {
                long costTimeMillis = ChronoUnit.MILLIS.between(startTime, endTime);
                operLog.setCostTime(costTimeMillis);
            } else {
                // 如果没有开始时间，设置为 0
                operLog.setCostTime(0L);
            }

            // 发布事件
            applicationEventPublisher.publishEvent(new SysOperLogEvent(operLog));
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, OperLog operLog, SysOperLog sysOperLog, Object jsonResult) {
        // 设置action动作
        sysOperLog.setBusinessType(operLog.businessType().ordinal());
        
        // 设置标题
        String title = operLog.title();
        if (StringUtils.isEmpty(title)) {
            // 动态解析
            title = parseTitle(joinPoint);
        }
        sysOperLog.setTitle(title);
        
        // 是否需要保存request，参数和值
        if (operLog.isSaveRequestData()) {
            setRequestValue(joinPoint, sysOperLog, operLog.excludeParamNames());
        }
        
        // 是否需要保存response，参数和值
        if (operLog.isSaveResponseData() && jsonResult != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = jsonResult instanceof Map
                        ? (Map<String, Object>) jsonResult
                        : objectMapper.convertValue(jsonResult, Map.class);
                sysOperLog.setJsonResult(resultMap);
            } catch (Exception e) {
                log.warn("json result serialize failed");
            }
        }
    }
    
    /**
     * 动态解析模块标题
     */
    private String parseTitle(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        Class<?> targetClass = target.getClass();
        
        // 1. 尝试获取 @Tag
        Tag tag = targetClass.getAnnotation(Tag.class);
        if (tag != null && StringUtils.isNotEmpty(tag.name())) {
            return tag.name();
        }
        
        // 2. 尝试获取 BaseController 的 Entity Class
        if (target instanceof BaseController<?, ?, ?> baseController) {
            try {
                Class<?> entityClass = baseController.getEntityClass();
                if (entityClass != null) {
                    Schema schema = entityClass.getAnnotation(Schema.class);
                    if (schema != null && StringUtils.isNotEmpty(schema.title())) {
                        return schema.title();
                    }
                    if (schema != null && StringUtils.isNotEmpty(schema.name())) {
                         return schema.name();
                    }
                    return entityClass.getSimpleName();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        
        return targetClass.getSimpleName();
    }

    /**
     * 获取请求的参数，放到log中
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog, String[] excludeParamNames) {
        Object[] args = joinPoint.getArgs();
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (!isFilterObject(args[i])) {
                arguments[i] = args[i];
            }
        }
        try {
             if (arguments.length > 0) {
                 // 简单处理：将参数数组转换为 Map 存储，key 为 "args"
                 // 实际生产中可能需要结合参数名进行更精细的映射
                 operLog.setOperParam(Map.of("args", arguments));
             }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection<?> collection = (Collection<?>) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map<?, ?> map = (Map<?, ?>) o;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
