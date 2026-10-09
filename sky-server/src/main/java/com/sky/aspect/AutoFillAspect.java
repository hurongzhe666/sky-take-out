package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static com.sky.constant.AutoFillConstant.*;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //切入点
    @Pointcut("execution(* com.sky.mapper.*.*(..))&&@annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {

    }

    /**
     * 前置通知，在通知中给公共字段赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段自动填充");

        //获取到当前被拦截数据库的操作的类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获得方法上的注解
        OperationType operationType = autoFill.value();//获得数据操作类型型

        //获取到当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs();//获取所有方法参数
        if (args == null || args.length == 0)
            return;

        Object entity = args[0];

        //准备赋值的数据

        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //给获取到的实体对象的对应公共参数赋值
        if (operationType == OperationType.INSERT){
            //为4个公共字段赋值
            try {
                //通过反射记录set方法
            Method createTimeMethod = entity.getClass().getDeclaredMethod(SET_CREATE_TIME, LocalDateTime.class);

            Method updateTimeMethod = entity.getClass().getDeclaredMethod(SET_UPDATE_TIME, LocalDateTime.class);

            Method createUserMethod = entity.getClass().getDeclaredMethod(SET_CREATE_USER, Long.class);

            Method updateUserMethod = entity.getClass().getDeclaredMethod(SET_UPDATE_USER, Long.class);

            //通过反射为对象属性赋值
                createTimeMethod.invoke(entity, now);
                updateTimeMethod.invoke(entity, now);
                createUserMethod.invoke(entity, currentId);
                updateUserMethod.invoke(entity, currentId);
            } catch (Exception e) {
                log.error("公共字段自动填充失败", e);
            }
        }

        if (operationType == OperationType.UPDATE){
            //为2个公共字段赋值
            try {
            Method updateTimeMethod = entity.getClass().getDeclaredMethod(SET_UPDATE_TIME, LocalDateTime.class);
            Method updateUserMethod = entity.getClass().getDeclaredMethod(SET_UPDATE_USER, Long.class);

            //通过反射为对象属性赋值
            updateTimeMethod.invoke(entity, now);
            updateUserMethod.invoke(entity, currentId);
            } catch (Exception e) {
                log.error("公共字段自动填充失败", e);
            }
        }

    }
}
