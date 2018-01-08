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

/**
 * @author hzfjd
 * @create 18/1/2
 */

@Activate(group = Constants.PROVIDER, value = "dubboHystrixProviderFilter")
public class DubboHystrixProviderFilter implements Filter {

    FallbackFactory fallbackFactory;

    public void setFallbackFactory(FallbackFactory fallbackFactory) {
        this.fallbackFactory = fallbackFactory;
    }

    @Override public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String originApplicationName = invocation.getAttachment(OriginApplicationConstants.HEADER_NAME);

        String groupKey = invoker.getInterface().getName();
        String rawCommandKey = HystrixKeyUtils.getMethodSignature(invoker.getInterface(), invocation.getMethodName(),
                                                                  invocation.getParameterTypes());
        String commandKey = HystrixKeyUtils.getCommandKey(HystrixKeyConstants.PROVIDER_API_PREFIX, rawCommandKey);
        String threadPoolKey = HystrixKeyUtils.getThreadPoolKey(HystrixKeyConstants.PROVIDER_API_PREFIX, rawCommandKey,
                                                                originApplicationName);

        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(groupKey)).andCommandKey(
                HystrixCommandKey.Factory.asKey(commandKey)).andThreadPoolKey(
                HystrixThreadPoolKey.Factory.asKey(threadPoolKey)).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                        HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE));


        Object fallback = null;
        if (fallbackFactory != null) {
            fallback = fallbackFactory.getFallback(invoker.getInterface(), true);
        }

        return DubboHystrixFilterSupport.invokeWithHystrix(invoker, invocation, setter, fallback, true);
    }
}
