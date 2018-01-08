package com.netease.edu.boot.hystrix.annotation;

import java.lang.annotation.*;

/**
 * 按照接口进行匹配的fallback bean,通过该注解标识
 * Created by hzfjd on 18/1/5.
 */
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EduFallbackBean {

    /**
     * 标识服务端还是客户端,适用于复用接口且在应用容器中同时存在的罕见情况
     * @return
     */
    boolean providerSide() default false;
}
