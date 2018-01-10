package com.netease.edu.boot.hystrix.aop.aspectj;/**
 * Created by hzfjd on 17/12/21.
 */

import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
import com.netease.edu.boot.hystrix.support.HystrixCommandAspectSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author hzfjd
 * @create 17/12/21
 */
@Aspect
public class DefaultHystrixCommandDwrAspect extends HystrixCommandAspectSupport {


    @Pointcut("execution(public * com.netease.edu..web.dwr..*.*(..))")
    public void eduDwr() {
    }

    @Pointcut("execution(public * com.netease.study..web.dwr..*.*(..))")
    public void studyDwr() {
    }

    @Pointcut("execution(public * com.netease.yooc..web.dwr..*.*(..))")
    public void yoocDwr() {
    }


    @Pointcut("execution(public * com.netease.mooc..web.dwr..*.*(..))")
    public void moocDwr() {
    }

    @Pointcut("eduDwr()||studyDwr()||yoocDwr()||moocDwr()")
    public void allDwr() {
    }

    @Around("allDwr()")
    public Object methodsWithHystrixCommand(final ProceedingJoinPoint joinPoint) throws Throwable {
        return super.methodsWithHystrixSupport(joinPoint, HystrixKeyPrefixEnum.UI_PROVIDER.getPrefix());
    }

}
