package com.netease.edu.boot.hystrix.aop.aspectj;/**
 * Created by hzfjd on 17/12/21.
 */

import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
import com.netease.edu.boot.hystrix.support.HystrixCommandAspectSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author hzfjd
 * @create 17/12/21
 */
@Aspect
public class DefaultHystrixCommandApiControllerAspect extends HystrixCommandAspectSupport implements InitializingBean {


    @Pointcut("execution(public * com.netease.edu..api.controller..*.*(..))")
    public void eduController() {
    }

    @Pointcut("execution(public * com.netease.study..*.api.controller..*.*.*(..))")
    public void studyController() {
    }

    @Pointcut("execution(public * com.netease.yooc..*.api.controller..*.*.*(..))")
    public void yoocController() {
    }


    @Pointcut("execution(public * com.netease.mooc..*.api.controller..*.*.*(..))")
    public void moocController() {
    }

    @Pointcut("eduController()||studyController()||yoocController()||moocController()")
    public void allController() {
    }

    @Around("allController()")
    public Object methodsWithHystrixCommand(final ProceedingJoinPoint joinPoint) throws Throwable {
        return super.methodsWithHystrixSupport(joinPoint, HystrixKeyPrefixEnum.API_PROVIDER.getPrefix());
    }

    @Override public void afterPropertiesSet() throws Exception {
        this.getEduHystrixCommandProperties().setIsolatedByOriginEnable(true);
    }
}
