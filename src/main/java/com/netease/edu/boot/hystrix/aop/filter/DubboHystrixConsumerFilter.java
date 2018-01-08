package com.netease.edu.boot.hystrix.aop.filter;/**
 * Created by hzfjd on 18/1/2.
 */

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.netease.edu.boot.hystrix.core.FallbackFactory;
import com.netease.edu.boot.hystrix.core.constants.HystrixKeyConstants;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import com.netease.edu.boot.hystrix.support.DubboHystrixFilterSupport;
import com.netease.edu.boot.hystrix.support.HystrixKeyUtils;
import com.netflix.hystrix.*;
import org.springframework.core.env.Environment;

/**
 * @author hzfjd
 * @create 18/1/2
 */

@Activate(group = Constants.CONSUMER, value = "dubboHystrixConsumerFilter")
public class DubboHystrixConsumerFilter implements Filter {

    FallbackFactory fallbackFactory;

    public void setFallbackFactory(FallbackFactory fallbackFactory) {
        this.fallbackFactory = fallbackFactory;
    }

    Environment environment;

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {

        invocation.getAttachments().put(OriginApplicationConstants.HEADER_NAME,
                                        environment.getProperty("spring.application.name"));

        String groupKey = invoker.getInterface().getName();
        String rawCommandKey = HystrixKeyUtils.getMethodSignature(invoker.getInterface(), invocation.getMethodName(),
                                                                  invocation.getParameterTypes());
        String commandKey = HystrixKeyUtils.getCommandKey(HystrixKeyConstants.CONSUMER_PREFIX, rawCommandKey);
        String threadPoolKey = commandKey;

        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(groupKey)).andCommandKey(
                HystrixCommandKey.Factory.asKey(commandKey)).andThreadPoolKey(
                HystrixThreadPoolKey.Factory.asKey(threadPoolKey)).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                        HystrixCommandProperties.ExecutionIsolationStrategy.THREAD));


        Object fallback = null;
        if (fallbackFactory != null) {
            fallback = fallbackFactory.getFallback(invoker.getInterface());
        }

        return DubboHystrixFilterSupport.invokeWithHystrix(invoker, invocation, setter, fallback, false);
    }





}
