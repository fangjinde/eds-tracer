package com.netease.edu.boot.hystrix.aop.aspectj;/**
 * Created by hzfjd on 17/12/21.
 */

import com.netease.edu.boot.hystrix.support.HystrixCommandAspectSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author hzfjd
 * @create 17/12/21
 */
//@Aspect
public class DefaultHystrixCommandFrontControllerAspect extends HystrixCommandAspectSupport {


    @Pointcut("execution(public * com.netease.edu..*.web.controller..*.*.*(..))||execution(public * com.netease.edu..*.mobile.controller..*.*.*(..))||execution(public * com.netease.edu..*.wap.controller..*.*.*(..))")
    public void eduController() {
    }

    @Pointcut("execution(public * com.netease.study..*.web.controller..*.*.*(..))||execution(public * com.netease.study..*.mobile.controller..*.*.*(..))||execution(public * com.netease.study..*.wap.controller..*.*.*(..))")
    public void studyController() {
    }

    @Pointcut("execution(public * com.netease.yooc..*.web.controller..*.*.*(..))||execution(public * com.netease.yooc..*.mobile.controller..*.*.*(..))||execution(public * com.netease.yooc..*.wap.controller..*.*.*(..))")
    public void yoocController() {
    }


    @Pointcut("execution(public * com.netease.mooc..*.web.controller..*.*.*(..))||execution(public * com.netease.mooc..*.mobile.controller..*.*.*(..))||execution(public * com.netease.mooc..*.wap.controller..*.*.*(..))")
    public void moocController() {
    }

    @Pointcut("eduController()||studyController()||yoocController()||moocController()")
    public void allController() {
    }

    @Around("allController()")
    public Object methodsWithHystrixCommand(final ProceedingJoinPoint joinPoint) throws Throwable {
        return super.methodsWithHystrixSupport(joinPoint);
    }


}
