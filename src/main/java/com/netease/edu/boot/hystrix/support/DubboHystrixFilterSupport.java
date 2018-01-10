package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/8.
 */

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import com.netease.edu.boot.hystrix.core.CommandAction;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
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


    public static CommandAction<Result> getCommandAction(final Invoker<?> invoker, final Invocation invocation) {
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


    public static CommandAction<Result> getFallbackAction(final Object fallbackFinal, final Invocation invocation) {
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
}
