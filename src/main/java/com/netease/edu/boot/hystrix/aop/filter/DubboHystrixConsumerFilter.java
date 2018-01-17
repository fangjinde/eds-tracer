package com.netease.edu.boot.hystrix.aop.filter;/**
 * Created by hzfjd on 18/1/2.
 */

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.netease.edu.boot.hystrix.core.*;
import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
import com.netease.edu.boot.hystrix.support.DubboHystrixFilterSupport;
import com.netease.edu.boot.hystrix.support.HystrixKeyUtils;
import com.netflix.hystrix.*;
import org.springframework.core.env.Environment;

/**
 * @author hzfjd
 * @create 18/1/2
 */

@Activate(group = Constants.CONSUMER, order = -9000)
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
        String methodSignature = HystrixKeyUtils.getMethodSignature(invoker.getInterface(), invocation.getMethodName(),
                                                                    invocation.getParameterTypes());
        String commandKey = HystrixKeyUtils.getCommandKey(HystrixKeyPrefixEnum.CONSUMER.getPrefix(), methodSignature,null);
        String threadPoolKey = commandKey;

        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(groupKey)).andCommandKey(
                HystrixCommandKey.Factory.asKey(commandKey)).andThreadPoolKey(
                HystrixThreadPoolKey.Factory.asKey(threadPoolKey)).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                        HystrixCommandProperties.ExecutionIsolationStrategy.THREAD));

        CommandAction<Result> commandAction = DubboHystrixFilterSupport.getCommandAction(invoker, invocation);

        ResultExceptionChecker<Result> resultChecker = new ResultExceptionChecker<Result>() {
            @Override
            public void check(Result result) throws CommandExecuteException {
                if (result.hasException() && !EduBadRequestExceptionIdentifier.isIgnorable(
                        result.getException())) {
                    //just let Hystrix to record
                    throw new CommandExecuteException().withResult(result);
                }
            }
        };

        Object fallback = null;
        CommandAction<Result> fallbackAction = null;
        if (fallbackFactory != null) {
            fallback = fallbackFactory.getFallback(invoker.getInterface());
            if (fallback != null) {
                fallbackAction = DubboHystrixFilterSupport.getFallbackAction(fallback, invocation);
            }
        }

       return EduHystrixExecutor.executeWithHystrix(commandAction,fallbackAction,setter,resultChecker);
    }




}
