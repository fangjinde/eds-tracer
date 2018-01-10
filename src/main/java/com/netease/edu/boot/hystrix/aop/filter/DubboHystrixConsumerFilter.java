package com.netease.edu.boot.hystrix.aop.filter;/**
 * Created by hzfjd on 18/1/2.
 */

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.edu.boot.hystrix.core.CommandAction;
import com.netease.edu.boot.hystrix.core.EduBadRequestExceptionIdentifier;
import com.netease.edu.boot.hystrix.core.FallbackFactory;
import com.netease.edu.boot.hystrix.core.HystrixExecutionContext;
import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
import com.netease.edu.boot.hystrix.support.DubboHystrixFilterSupport;
import com.netease.edu.boot.hystrix.support.HystrixKeyUtils;
import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.springframework.core.env.Environment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        String commandKey = HystrixKeyUtils.getCommandKey(HystrixKeyPrefixEnum.CONSUMER.getPrefix(), methodSignature);
        String threadPoolKey = commandKey;

        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(groupKey)).andCommandKey(
                HystrixCommandKey.Factory.asKey(commandKey)).andThreadPoolKey(
                HystrixThreadPoolKey.Factory.asKey(threadPoolKey)).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                        HystrixCommandProperties.ExecutionIsolationStrategy.THREAD));

        CommandAction<Result> commandAction = getCommandAction(invoker, invocation);

        ResultChecker<Result> resultChecker = new ResultChecker<Result>() {

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
                fallbackAction = getFallbackAction(fallback, invocation);
            }
        }

        return DubboHystrixFilterSupport.invokeWithHystrix(invoker, invocation, setter, fallback, false);
    }

    public CommandAction<Result> getCommandAction(final Invoker<?> invoker, final Invocation invocation) {
        return new CommandAction<Result>() {

            @Override
            public Result execute() throws CommandExecuteException {
                try {
                    Result result = invoker.invoke(invocation);
                    return result;
                } catch (Throwable e) {
                    throw new CommandExecuteException(e);
                }
            }
        };

    }

    public static Result invokeWithHystrix(final CommandAction<Result> commandAction,
                                           final CommandAction<Result> fallbackAction,
                                           HystrixCommand.Setter setter, final ResultChecker<Result> resultChecker)
            throws
            RpcException {
        HystrixCommand<Result> hystrixCommand = new HystrixCommand<Result>(setter) {

            @Override
            protected Result run() throws Exception {
                Result result = null;
                try {
                    result = commandAction.execute();
                } catch (CommandExecuteException e) {
                    if (EduBadRequestExceptionIdentifier.isIgnorable(
                            e.getCause())) {
                        throw new HystrixBadRequestException("", e.getCause());
                    }
                    throw e;
                }

                //if there is implicit exception in result, throw CommandExecuteException with origin result
                if (resultChecker != null) {
                    resultChecker.check(result);
                }
                return result;
            }


            @Override
            protected Result getFallback() {

                Throwable commandExecutionException = getFailedExecutionException();
                Throwable targetException = commandExecutionException;
                Result originResult = null;
                if (commandExecutionException instanceof CommandExecuteException) {
                    targetException = commandExecutionException.getCause();
                    originResult = ((CommandExecuteException) commandExecutionException).getResult();
                }

                if (fallbackAction == null) {
                    //no fallback, use the origin result if exists
                    if (originResult != null) {
                        return originResult;
                    }
                    //hystrix will ignore this fallback exception, so whatever it is.
                    throw new RuntimeException("No fallback available.", commandExecutionException);
                }

                Result fallbackResult = null;
                try {
                    HystrixExecutionContext.setExecutionException(targetException);
                    fallbackResult = fallbackAction.execute();
                    return fallbackResult;
                } catch (RuntimeException fallbackExecuteException) {
                    //fallback failed(that means throw exception), use the origin result if exists
                    if (originResult != null) {
                        return originResult;
                    }
                    //hystrix will ignore this fallback exception, so whatever it is.
                    throw fallbackExecuteException;

                } finally {
                    HystrixExecutionContext.resetExecutionException();
                }

            }
        };

        return hystrixCommand.execute();
    }

    public interface ResultChecker<R> {

        void check(R result) throws CommandExecuteException;
    }

    public CommandAction<Result> getFallbackAction(final Object fallbackFinal, final Invocation invocation) {
        return new CommandAction<Result>() {

            @Override
            public Result execute() throws CommandExecuteException {
                try {
                    Method method = fallbackFinal.getClass().getMethod(
                            RpcUtils.getMethodName(invocation),
                            RpcUtils.getParameterTypes(
                                    invocation));
                    Object fallbackResult = method.invoke(fallbackFinal, RpcUtils.getArguments(invocation));
                    return new RpcResult(fallbackResult);
                } catch (NoSuchMethodException e) {
                    throw new CommandExecuteException(e);
                } catch (InvocationTargetException e) {
                    throw new CommandExecuteException(e);
                } catch (IllegalAccessException e) {
                    throw new CommandExecuteException(e);
                }

            }
        };
    }

    static class DubboExceptionResultAdapterException extends RuntimeException {

        Result result = null;

        DubboExceptionResultAdapterException(Result result) {
            this.result = result;
        }

        public Result getResult() {
            return result;
        }

    }

}
