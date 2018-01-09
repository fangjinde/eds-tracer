package com.netease.edu.boot.hystrix.aop.filter;/**
 * Created by hzfjd on 18/1/2.
 */

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import com.netease.edu.boot.hystrix.support.DubboHystrixFilterSupport;
import com.netease.edu.boot.hystrix.support.HystrixKeyUtils;
import com.netflix.hystrix.*;

/**
 * @author hzfjd
 * @create 18/1/2
 */

@Activate(group = Constants.PROVIDER)
public class DubboHystrixProviderFilter implements Filter {

    @Override
    public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {
        String originApplicationName = invocation.getAttachment(OriginApplicationConstants.HEADER_NAME);

        String groupKey = invoker.getInterface().getName();
        String rawCommandKey = HystrixKeyUtils.getMethodSignature(invoker.getInterface(), invocation.getMethodName(),
                                                                  invocation.getParameterTypes());
        String commandKey = HystrixKeyUtils.getCommandKey(HystrixKeyPrefixEnum.API_PROVIDER.getPrefix(), rawCommandKey);
        String threadPoolKey = HystrixKeyUtils.getThreadPoolKey(HystrixKeyPrefixEnum.API_PROVIDER.getPrefix(), rawCommandKey,
                                                                originApplicationName);

        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(groupKey)).andCommandKey(
                HystrixCommandKey.Factory.asKey(commandKey)).andThreadPoolKey(
                HystrixThreadPoolKey.Factory.asKey(threadPoolKey)).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                        HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE));


        HystrixCommand<Result> hystrixCommand = new HystrixCommand<Result>(setter) {

            @Override
            protected Result run() throws Exception {
                    Result result=invoker.invoke(invocation);
                    if(result.hasException()&&!DubboHystrixFilterSupport.isUserBadRequestException(
                            result.getException())){
                        //just let Hystrix to record
                        throw new DubboProviderInvokeException(result.getException());
                    }
                    return result;
            }

            @Override
            protected Result getFallback() {
                    Throwable executionException = getFailedExecutionException();
                    if (executionException instanceof DubboProviderInvokeException){
                        //wrapper it back to RpcResult
                        return new RpcResult(executionException.getCause());
                    }
                    //so far i have no idea why it will happen
                    throw new RuntimeException("",executionException);
            }
        };

        return hystrixCommand.execute();
    }


    static class DubboProviderInvokeException extends Exception{
        DubboProviderInvokeException(Throwable cause){
            super(cause);
        }
    }
}
