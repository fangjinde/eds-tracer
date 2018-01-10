package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/8.
 */

import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.edu.boot.hystrix.core.EduBadRequestExceptionIdentifier;
import com.netease.edu.boot.hystrix.core.HystrixExecutionContext;
import com.netflix.hystrix.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author hzfjd
 * @create 18/1/8
 */
public class DubboHystrixFilterSupport {

    private static final Logger logger = LoggerFactory.getLogger(DubboHystrixFilterSupport.class);


    public static Result invokeWithHystrix(final Invoker<?> invoker, final Invocation invocation,
                                           HystrixCommand.Setter setter, final Object fallbackFinal,
                                           final boolean providerSide) throws
                                                                       RpcException {
        HystrixCommand<Result> hystrixCommand = new HystrixCommand<Result>(setter) {

            @Override
            protected Result run() throws Exception {
                Result result = invoker.invoke(invocation);
                if (result.hasException() && !EduBadRequestExceptionIdentifier.isIgnorable(
                        result.getException())) {
                    //just let Hystrix to record
                    throw new DubboExceptionResultAdapterException(result);
                }
                return result;
            }

            private Result returnResultOrgetSuperFallback(Result result) {
                if (result != null) {
                    return result;
                }
                return super.getFallback();
            }

            private Result returnResultOrThrowException(Result result,Throwable e) {
                if (result != null) {
                    return result;
                }
                if (e instanceof RuntimeException){
                    throw (RuntimeException)e;
                }
                throw new RuntimeException("caught exception of on getFallback for dubbo, simple wrapper it cause it is useless for hystrix.",e);
            }

            @Override
            protected Result getFallback() {
                Throwable commandExecutionException = getFailedExecutionException();

                Result result = null;
                if (commandExecutionException instanceof DubboExceptionResultAdapterException) {
                    result = ((DubboExceptionResultAdapterException) commandExecutionException).getResult();
                }

                if (fallbackFinal == null) {
                    return returnResultOrgetSuperFallback(result);
                }

                Object fallbackResult = null;
                Throwable targetException = commandExecutionException;
                if (result!=null) {
                    targetException = result.getException();
                }

                HystrixExecutionContext.setExecutionException(targetException);
                try {
                    Method method = fallbackFinal.getClass().getMethod(RpcUtils.getMethodName(invocation),
                                                                       RpcUtils.getParameterTypes(
                                                                               invocation));
                    fallbackResult = method.invoke(fallbackFinal, RpcUtils.getArguments(invocation));
                } catch (NoSuchMethodException e) {
                    return returnResultOrThrowException(result, e);
                } catch (IllegalAccessException e) {
                    return returnResultOrThrowException(result, e);
                } catch (IllegalArgumentException e) {
                    return returnResultOrThrowException(result, e);
                } catch (InvocationTargetException e) {
                    return returnResultOrThrowException(result, e);
                } finally {
                    HystrixExecutionContext.resetExecutionException();
                }

                return new RpcResult(fallbackResult);

            }
        };

        return hystrixCommand.execute();
    }

    static class DubboExceptionResultAdapterException extends Exception {

        Result result = null;

        DubboExceptionResultAdapterException(Result result) {
            this.result = result;
        }

        public Result getResult() {
            return result;
        }

    }
}
