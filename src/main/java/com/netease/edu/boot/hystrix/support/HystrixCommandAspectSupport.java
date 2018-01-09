package com.netease.edu.boot.hystrix.support;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.netease.edu.boot.hystrix.annotation.EduHystrixCollapser;
import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netease.edu.boot.hystrix.core.EduHystrixCommandProperties;
import com.netease.edu.boot.hystrix.core.OriginApplicationNameResolver;
import com.netflix.hystrix.HystrixInvokable;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.command.CommandExecutor;
import com.netflix.hystrix.contrib.javanica.command.ExecutionType;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import com.netflix.hystrix.contrib.javanica.exception.CommandActionExecutionException;
import com.netflix.hystrix.contrib.javanica.utils.AopUtils;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import rx.functions.Func1;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static com.netflix.hystrix.contrib.javanica.utils.AopUtils.getDeclaredMethod;
import static com.netflix.hystrix.contrib.javanica.utils.AopUtils.getMethodFromTarget;
import static com.netflix.hystrix.contrib.javanica.utils.EnvUtils.isCompileWeaving;
import static com.netflix.hystrix.contrib.javanica.utils.ajc.AjcUtils.getAjcMethodAroundAdvice;

/**
 * @author hzfjd
 * @create 17/12/24
 */
public class HystrixCommandAspectSupport {

    private OriginApplicationNameResolver originApplicationNameResolver;

    public void setOriginApplicationNameResolver(OriginApplicationNameResolver originApplicationNameResolver) {
        this.originApplicationNameResolver = originApplicationNameResolver;
    }

    public EduHystrixCommandProperties getEduHystrixCommandProperties() {
        return eduHystrixCommandProperties;
    }

    private EduHystrixCommandProperties eduHystrixCommandProperties;

    @Autowired
    public void setEduHystrixCommandProperties(EduHystrixCommandProperties eduHystrixCommandProperties) {
        this.eduHystrixCommandProperties = eduHystrixCommandProperties;
    }

    private static final Map<HystrixPointcutType, MetaHolderFactory> META_HOLDER_FACTORY_MAP;

    static {
        META_HOLDER_FACTORY_MAP = ImmutableMap.<HystrixPointcutType, MetaHolderFactory>builder()
                                              .put(HystrixPointcutType.COMMAND, new CommandMetaHolderFactory())
                                              .put(HystrixPointcutType.COLLAPSER, new CollapserMetaHolderFactory())
                                              .build();
    }

    public Object methodsWithHystrixSupport(final ProceedingJoinPoint joinPoint,String sidePrefix) throws Throwable {
        Method method = getMethodFromTarget(joinPoint);
        Validate.notNull(method, String.format("failed to get method from joinPoint: %s", joinPoint));
        if (method.isAnnotationPresent(EduHystrixCommand.class) && method.isAnnotationPresent(
                EduHystrixCollapser.class)) {
            throw new IllegalStateException(
                    "method cannot be annotated with EduHystrixCommand and EduHystrixCollapser " +
                    "annotations at the same time");
        }
        MetaHolderFactory metaHolderFactory = META_HOLDER_FACTORY_MAP.get(HystrixPointcutType.of(method));
        MetaHolder metaHolder = metaHolderFactory.create(joinPoint,
                                                         eduHystrixCommandProperties.isIsolatedByOriginEnable() ? originApplicationNameResolver : null, sidePrefix);
        HystrixInvokable invokable = EduHystrixCommandFactory.getInstance().create(metaHolder);
        ExecutionType executionType = metaHolder.isCollapserAnnotationPresent() ?
                metaHolder.getCollapserExecutionType() : metaHolder.getExecutionType();

        Object result;
        try {
            if (!metaHolder.isObservable()) {
                result = CommandExecutor.execute(invokable, executionType, metaHolder);
            } else {
                result = executeObservable(invokable, executionType, metaHolder);
            }
        } catch (HystrixBadRequestException e) {
            throw e.getCause();
        } catch (HystrixRuntimeException e) {
            throw getCauseOrDefault(e, e);
        }
        return result;
    }

    private Observable executeObservable(HystrixInvokable invokable, ExecutionType executionType,
                                         MetaHolder metaHolder) {
        return ((Observable) CommandExecutor.execute(invokable, executionType, metaHolder))
                .onErrorResumeNext(new Func1<Throwable, Observable>() {

                    @Override
                    public Observable call(Throwable throwable) {
                        if (throwable instanceof HystrixBadRequestException) {
                            return Observable.error(throwable.getCause());
                        } else if (throwable instanceof HystrixRuntimeException) {
                            HystrixRuntimeException hystrixRuntimeException = (HystrixRuntimeException) throwable;
                            return Observable.error(
                                    getCauseOrDefault(hystrixRuntimeException, hystrixRuntimeException));
                        }
                        return Observable.error(throwable);
                    }
                });
    }

    private Throwable getCauseOrDefault(RuntimeException e, RuntimeException defaultException) {
        if (e.getCause() == null) return defaultException;
        if (e.getCause() instanceof CommandActionExecutionException) {
            CommandActionExecutionException commandActionExecutionException = (CommandActionExecutionException) e.getCause();
            return Optional.fromNullable(commandActionExecutionException.getCause()).or(defaultException);
        }
        return e.getCause();
    }

    /**
     * A factory to create MetaHolder depending on {@link HystrixPointcutType}.
     */
    private static abstract class MetaHolderFactory {

        public MetaHolder create(final ProceedingJoinPoint joinPoint,
                                 OriginApplicationNameResolver originApplicationNameResolver,String sidePrefix) {
            Method method = getMethodFromTarget(joinPoint);
            Object obj = joinPoint.getTarget();
            Object[] args = joinPoint.getArgs();
            Object proxy = joinPoint.getThis();
            return create(proxy, method, obj, args, joinPoint, originApplicationNameResolver, sidePrefix);
        }

        public abstract MetaHolder create(Object proxy, Method method, Object obj, Object[] args,
                                          final ProceedingJoinPoint joinPoint,
                                          OriginApplicationNameResolver originApplicationNameResolver,String sidePrefix);

        MetaHolder.Builder metaHolderBuilder(Object proxy, Method method, Object obj, Object[] args,
                                             final ProceedingJoinPoint joinPoint,
                                             OriginApplicationNameResolver originApplicationNameResolver,String sidePrefix) {
            MetaHolder.Builder builder = MetaHolder.builder()
                                                   .args(args).method(method).obj(obj).proxyObj(proxy)
                                                   .joinPoint(joinPoint);

            setFallbackMethod(builder, obj.getClass(), method);

            setDefaultKeyByJoinPoint(builder,proxy,method,obj,args,joinPoint,originApplicationNameResolver, sidePrefix);

            builder = setDefaultProperties(builder, obj.getClass(), joinPoint);

            // String originalApplicationName=originApplicationNameResolver.getOriginApplicationName();
            return builder;
        }

        /**
         * 根据方法签名以及应用来源,确定默认的group key, command key, thread pool key
         * @param builder
         * @param proxy
         * @param method
         * @param obj
         * @param args
         * @param joinPoint
         * @param originApplicationNameResolver
         * @return
         */
        private MetaHolder.Builder setDefaultKeyByJoinPoint(MetaHolder.Builder builder,Object proxy, Method method, Object obj, Object[] args,
                                                            final ProceedingJoinPoint joinPoint,OriginApplicationNameResolver originApplicationNameResolver,String sidePrefix){
            return HystrixKeyUtils.setDefaultKeyBySignatureAndOrigin(builder, method, obj,
                                                                     originApplicationNameResolver, sidePrefix);
        }

    }



    private static class CollapserMetaHolderFactory extends MetaHolderFactory {

        @Override
        public MetaHolder create(Object proxy, Method collapserMethod, Object obj, Object[] args,
                                 final ProceedingJoinPoint joinPoint,
                                 OriginApplicationNameResolver originApplicationNameResolver,String sidePrefix) {
            EduHystrixCollapser hystrixCollapser = collapserMethod.getAnnotation(EduHystrixCollapser.class);
            if (collapserMethod.getParameterTypes().length > 1 || collapserMethod.getParameterTypes().length == 0) {
                throw new IllegalStateException("Collapser method must have one argument: " + collapserMethod);
            }

            Method batchCommandMethod = getDeclaredMethod(obj.getClass(), hystrixCollapser.batchMethod(), List.class);

            if (batchCommandMethod == null)
                throw new IllegalStateException("batch method is absent: " + hystrixCollapser.batchMethod());

            Class<?> batchReturnType = batchCommandMethod.getReturnType();
            Class<?> collapserReturnType = collapserMethod.getReturnType();
            boolean observable = collapserReturnType.equals(Observable.class);

            if (!collapserMethod.getParameterTypes()[0]
                    .equals(getFirstGenericParameter(batchCommandMethod.getGenericParameterTypes()[0]))) {
                throw new IllegalStateException(
                        "required batch method for collapser is absent, wrong generic type: expected "
                        + obj.getClass().getCanonicalName() + "." +
                        hystrixCollapser.batchMethod() + "(java.util.List<" + collapserMethod.getParameterTypes()[0]
                        + ">), but it's " +
                        getFirstGenericParameter(batchCommandMethod.getGenericParameterTypes()[0]));
            }

            final Class<?> collapserMethodReturnType = getFirstGenericParameter(
                    collapserMethod.getGenericReturnType(),
                    Future.class.isAssignableFrom(collapserReturnType) || Observable.class.isAssignableFrom(
                            collapserReturnType) ? 1 : 0);

            Class<?> batchCommandActualReturnType = getFirstGenericParameter(batchCommandMethod.getGenericReturnType());
            if (!collapserMethodReturnType
                    .equals(batchCommandActualReturnType)) {
                throw new IllegalStateException(
                        "Return type of batch method must be java.util.List parametrized with corresponding type: expected "
                        +
                        "(java.util.List<" + collapserMethodReturnType + ">)" + obj.getClass().getCanonicalName() + "."
                        +
                        hystrixCollapser.batchMethod() + "(java.util.List<" + collapserMethod.getParameterTypes()[0]
                        + ">), but it's " +
                        batchCommandActualReturnType);
            }

            EduHystrixCommand hystrixCommand = batchCommandMethod.getAnnotation(EduHystrixCommand.class);

            //can be null now
            //            if (hystrixCommand == null) {
            //                throw new IllegalStateException("batch method must be annotated with HystrixCommand annotation");
            //            }

            // method of batch hystrix command must be passed to metaholder because basically collapser doesn't have any actions
            // that should be invoked upon intercepted method, it's required only for underlying batch command

            MetaHolder.Builder builder = metaHolderBuilder(proxy, batchCommandMethod, obj, args, joinPoint,
                                                           originApplicationNameResolver, sidePrefix);

            if (isCompileWeaving()) {
                builder.ajcMethod(getAjcMethodAroundAdvice(obj.getClass(), batchCommandMethod.getName(), List.class));
            }

            builder.hystrixCollapser(new HystrixCollapserAnnotationAdapter(hystrixCollapser));
            builder.defaultCollapserKey(collapserMethod.getName());
            builder.collapserExecutionType(ExecutionType.getExecutionType(collapserReturnType));

            builder.defaultCommandKey(batchCommandMethod.getName());
            builder.hystrixCommand(new HystrixCommandAnnotationAdapter(hystrixCommand));
            builder.executionType(ExecutionType.getExecutionType(batchReturnType));
            builder.observable(observable);
            FallbackMethod fallbackMethod = EduMethodProvider.getInstance().getFallbackMethod(obj.getClass(),
                                                                                              batchCommandMethod);
            if (fallbackMethod.isPresent()) {
                fallbackMethod.validateReturnType(batchCommandMethod);
                builder
                        .fallbackMethod(fallbackMethod.getMethod())
                        .fallbackExecutionType(
                                ExecutionType.getExecutionType(fallbackMethod.getMethod().getReturnType()));
            }
            return builder.build();
        }
    }

    private static class CommandMetaHolderFactory extends MetaHolderFactory {

        @Override
        public MetaHolder create(Object proxy, Method method, Object obj, Object[] args,
                                 final ProceedingJoinPoint joinPoint,
                                 OriginApplicationNameResolver originApplicationNameResolver,String sidePrefix) {
            EduHystrixCommand hystrixCommand = method.getAnnotation(EduHystrixCommand.class);
            ExecutionType executionType = ExecutionType.getExecutionType(method.getReturnType());
            MetaHolder.Builder builder = metaHolderBuilder(proxy, method, obj, args, joinPoint,
                                                           originApplicationNameResolver, sidePrefix);
            if (isCompileWeaving()) {
                builder.ajcMethod(getAjcMethodFromTarget(joinPoint));
            }
            return builder
                    .hystrixCommand(new HystrixCommandAnnotationAdapter(hystrixCommand))
                    .observableExecutionMode(hystrixCommand.observableExecutionMode())
                    .executionType(executionType)
                    .observable(ExecutionType.OBSERVABLE == executionType)
                    .build();
        }
    }

    private enum HystrixPointcutType {
        COMMAND,
        COLLAPSER;

        //默认为COMMAND,包括没有annotation的情况
        static HystrixPointcutType of(Method method) {
            return method.isAnnotationPresent(EduHystrixCollapser.class) ? COLLAPSER : COMMAND;
        }
    }

    private static Method getAjcMethodFromTarget(JoinPoint joinPoint) {
        return getAjcMethodAroundAdvice(joinPoint.getTarget().getClass(), (MethodSignature) joinPoint.getSignature());
    }

    private static Class<?> getFirstGenericParameter(Type type) {
        return getFirstGenericParameter(type, 1);
    }

    private static Class<?> getFirstGenericParameter(final Type type, final int nestedDepth) {
        int cDepth = 0;
        Type tType = type;

        for (int cDept = 0; cDept < nestedDepth; cDept++) {
            if (!(tType instanceof ParameterizedType))
                throw new IllegalStateException(
                        String.format("Sub type at nesting level %d of %s is expected to be generic", cDepth, type));
            tType = ((ParameterizedType) tType).getActualTypeArguments()[cDept];
        }

        if (tType instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) tType).getRawType();
        else if (tType instanceof Class)
            return (Class<?>) tType;

        throw new UnsupportedOperationException("Unsupported type " + tType);
    }



    private static MetaHolder.Builder setDefaultProperties(MetaHolder.Builder builder, Class<?> declaringClass,
                                                           final ProceedingJoinPoint joinPoint) {
        Optional<DefaultProperties> defaultPropertiesOpt = AopUtils.getAnnotation(joinPoint, DefaultProperties.class);



        if (defaultPropertiesOpt.isPresent()) {
            DefaultProperties defaultProperties = defaultPropertiesOpt.get();
            //默认切换为SEMAPHORE隔离策略
            builder.defaultProperties(new DefaultPropertiesAdapter(defaultProperties,HystrixPropertyForged.SEMAPHORE_HYSTRIX_PROPERTY));
            if (StringUtils.isNotBlank(defaultProperties.groupKey())) {
                builder.defaultGroupKey(defaultProperties.groupKey());
            }
            if (StringUtils.isNotBlank(defaultProperties.threadPoolKey())) {
                builder.defaultThreadPoolKey(defaultProperties.threadPoolKey());
            }
        }else{
            //默认切换为SEMAPHORE隔离策略
            builder.defaultProperties(new DefaultPropertiesForged(HystrixPropertyForged.SEMAPHORE_HYSTRIX_PROPERTY));

        }



        return builder;
    }

    private static MetaHolder.Builder setFallbackMethod(MetaHolder.Builder builder, Class<?> declaringClass,
                                                        Method commandMethod) {
        FallbackMethod fallbackMethod = EduMethodProvider.getInstance().getFallbackMethod(declaringClass,
                                                                                          commandMethod);
        if (fallbackMethod.isPresent()) {
            fallbackMethod.validateReturnType(commandMethod);
            builder
                    .fallbackMethod(fallbackMethod.getMethod())
                    .fallbackExecutionType(ExecutionType.getExecutionType(fallbackMethod.getMethod().getReturnType()));
        }
        return builder;
    }

}
