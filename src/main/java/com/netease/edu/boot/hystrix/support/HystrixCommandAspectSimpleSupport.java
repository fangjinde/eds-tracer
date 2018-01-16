package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/15.
 */

import com.google.common.base.Optional;
import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netease.edu.boot.hystrix.core.*;
import com.netease.edu.boot.hystrix.core.constants.HystrixBeanNameContants;
import com.netease.edu.boot.hystrix.core.exception.CommandExecuteException;
import com.netflix.hystrix.*;
import com.netflix.hystrix.contrib.javanica.exception.FallbackDefinitionException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.netflix.hystrix.contrib.javanica.utils.AopUtils.getMethodFromTarget;

/**
 * @author hzfjd
 * @create 18/1/15
 */
public class HystrixCommandAspectSimpleSupport {

    private String sidePrefix;

    public String getSidePrefix() {
        return sidePrefix;
    }

    public void setSidePrefix(String sidePrefix) {
        this.sidePrefix = sidePrefix;
    }

    public OriginApplicationNameResolver getOriginApplicationNameResolver() {
        return originApplicationNameResolver;
    }

    private OriginApplicationNameResolver originApplicationNameResolver;

    public void setOriginApplicationNameResolver(OriginApplicationNameResolver originApplicationNameResolver) {
        this.originApplicationNameResolver = originApplicationNameResolver;
    }

    ResultExceptionChecker<Object> resultChecker;

    public ResultExceptionChecker<Object> getResultChecker() {
        return resultChecker;
    }

    @Resource(name = HystrixBeanNameContants.HYSTRIX_COMMAND_ASPECT_RESULT_EXCEPTION_CHECKERS)
    public void setResultChecker(ResultExceptionChecker<Object> resultChecker) {
        this.resultChecker = resultChecker;
    }

    public Object methodsWithHystrix(final ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethodFromTarget(joinPoint);
        return invokeWithHystrix(method, joinPoint.getTarget(), joinPoint.getArgs(),
                                 getCommandActionFromJoinPoint(joinPoint), null);
    }

    private Object invokeWithHystrix(final Method method, final Object target, final Object[] args,
                                     CommandAction<Object> methodCommandWrapper, FallbackMethod lastestFallbackMethod) {
        //ThreadPoolKey默认：RemoteAppcationName+Class+Method+ArgumentTypes
        String defaultCommandGroupKey = target.getClass().getName();
        String methodSignature = HystrixKeyUtils.getMethodSignature(method, target);

        HystrixKeyParam hystrixKeyParam = new HystrixKeyParam(sidePrefix, methodSignature);
        String defaultCommandKey = hystrixKeyParam.generateCommandKey();
        String defaultThreadPoolKey = defaultCommandKey;

        if (originApplicationNameResolver != null) {
            String originApplicationName = originApplicationNameResolver.getOriginApplicationName();
            if (StringUtils.isNotBlank(originApplicationName)) {
                hystrixKeyParam.setOriginApplicationName(originApplicationName);
                defaultThreadPoolKey = hystrixKeyParam.generateThreadPoolKey();
            }
        }

        //TODO 增加DefaultProperties的支持,看需要吧.

        HystrixCommand.Setter setter = HystrixCommand.Setter.withGroupKey(
                HystrixCommandGroupKey.Factory.asKey(defaultCommandGroupKey)).andCommandKey(
                HystrixCommandKey.Factory.asKey(defaultCommandKey)).andThreadPoolKey(
                HystrixThreadPoolKey.Factory.asKey(defaultThreadPoolKey)).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(
                        HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE));

        // reflect to invoke in normal
        if (methodCommandWrapper == null) {
            methodCommandWrapper = new CommandAction<Object>() {

                @Override
                public Object execute() throws CommandExecuteException {
                    try {
                        return method.invoke(target, args);
                    } catch (IllegalAccessException e) {
                        throw new CommandExecuteException(e);
                    } catch (InvocationTargetException e) {
                        throw new CommandExecuteException(e.getTargetException());
                    }
                }
            };
        }

        CommandAction<Object> fallbackAction = getFallbackAction(method, target, args, methodCommandWrapper,
                                                                 lastestFallbackMethod);
        return EduHystrixExecutor.executeWithHystrix(methodCommandWrapper, fallbackAction, setter, resultChecker);

    }

    private CommandAction<Object> getFallbackAction(final Method method, final Object target, final Object[] args,
                                                    final CommandAction<Object> methodCommandAction,
                                                    FallbackMethod lastestFallbackMethod) {

        final FallbackMethod fallbackMethod = getFallbackMethod(target.getClass(), method,
                                                                (lastestFallbackMethod != null
                                                                 && lastestFallbackMethod.isExtended()) ? true : false);

        CommandAction<Object> fallbackAction = null;
        if (fallbackMethod.isPresent()) {

            final Method fMethod = fallbackMethod.getMethod();
            if (fMethod.isAnnotationPresent(EduHystrixCommand.class)) {
                fMethod.setAccessible(true);
                return getCompositeFallbackCommandAction(target,args,fallbackMethod,fMethod);

            } else {

                return getPlainFallbackCommandAction(fallbackMethod, target, args);

            }

        }
        return fallbackAction;
    }

    private CommandAction<Object> getCompositeFallbackCommandAction(final Object target, final Object[] args,
                                                                    final FallbackMethod fallbackMethod,
                                                                    final Method fMethod) {
        return new CommandAction<Object>() {

            @Override
            public Object execute() throws CommandExecuteException {
                try {
                    return invokeWithHystrix(fMethod, target, args,
                                             getPlainFallbackCommandAction(fallbackMethod, target, args),
                                             fallbackMethod);
                } catch (RuntimeException e) {
                    throw new CommandExecuteException(e);
                }
            }
        };
    }

    private CommandAction<Object> getPlainFallbackCommandAction(final FallbackMethod fallbackMethod,
                                                                final Object target,
                                                                final Object[] originArgs) {
        return new CommandAction<Object>() {

            @Override
            public Object execute() throws CommandExecuteException {
                try {
                    Throwable executeException = HystrixExecutionContext.getExecutionException();
                    Object result = null;
                    if (fallbackMethod.isExtended()) {
                        Object[] extendedArgs = new Object[originArgs.length + 1];
                        for (int i = 0; i < originArgs.length; i++) {
                            extendedArgs[i] = originArgs[i];
                        }
                        extendedArgs[originArgs.length] = executeException;

                        result = fallbackMethod.getMethod().invoke(target, extendedArgs);
                    } else {
                        result = fallbackMethod.getMethod().invoke(target, originArgs);
                    }

                    return result;
                } catch (InvocationTargetException e) {
                    throw new CommandExecuteException(e.getTargetException());
                } catch (IllegalAccessException e) {
                    throw new CommandExecuteException(e);
                } catch (Throwable e) {
                    throw new CommandExecuteException(e);
                }
            }
        };
    }

    private FallbackMethod getFallbackMethod(Class<?> type, Method commandMethod, boolean extended) {
        if (commandMethod.isAnnotationPresent(EduHystrixCommand.class)) {
            EduHystrixCommand hystrixCommand = commandMethod.getAnnotation(EduHystrixCommand.class);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(hystrixCommand.fallbackMethod())) {
                Class<?>[] parameterTypes = commandMethod.getParameterTypes();
                if (extended && parameterTypes[parameterTypes.length - 1] == Throwable.class) {
                    parameterTypes = ArrayUtils.remove(parameterTypes, parameterTypes.length - 1);
                }
                Class<?>[] exParameterTypes = Arrays.copyOf(parameterTypes, parameterTypes.length + 1);
                exParameterTypes[parameterTypes.length] = Throwable.class;
                Optional<Method> exFallbackMethod = EduMethodProvider.getInstance().getMethod(type,
                                                                                              hystrixCommand.fallbackMethod(),
                                                                                              exParameterTypes);
                Optional<Method> fMethod = EduMethodProvider.getInstance().getMethod(type,
                                                                                     hystrixCommand.fallbackMethod(),
                                                                                     parameterTypes);
                Method method = exFallbackMethod.or(fMethod).orNull();
                if (method == null) {
                    throw new FallbackDefinitionException(
                            "fallback method wasn't found: " + hystrixCommand.fallbackMethod() + "(" + Arrays.toString(
                                    parameterTypes) + ")");
                }
                return new FallbackMethod(method, exFallbackMethod.isPresent());
            }
        }
        return FallbackMethod.ABSENT;
    }

    private CommandAction<Object> getCommandActionFromJoinPoint(final ProceedingJoinPoint joinPoint) {
        return new CommandAction<Object>() {

            @Override
            public Object execute() throws CommandExecuteException {

                Object result = null;
                try {
                    result = joinPoint.proceed();
                } catch (Throwable e) {
                    throw new CommandExecuteException(e);
                }
                return result;

            }
        };

    }

}
