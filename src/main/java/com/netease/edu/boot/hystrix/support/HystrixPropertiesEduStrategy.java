package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/9.
 */

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

/**
 * @author hzfjd
 * @create 18/1/9
 */
public class HystrixPropertiesEduStrategy extends HystrixPropertiesStrategy {

    @Override
    public HystrixCommandProperties getCommandProperties(HystrixCommandKey commandKey,
                                                         HystrixCommandProperties.Setter builder) {
        return new EduHystrixCommandProperties(commandKey, builder);
    }

    @Override
    public HystrixThreadPoolProperties getThreadPoolProperties(HystrixThreadPoolKey threadPoolKey,
                                                               HystrixThreadPoolProperties.Setter builder) {
        return new EduHystrixThreadPoolProperties(threadPoolKey, builder);
    }
}
