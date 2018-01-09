package com.netease.edu.boot.hystrix.support;

import com.netease.edu.boot.hystrix.annotation.EduHystrixCommand;
import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.command.*;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import static com.netflix.hystrix.contrib.javanica.cache.CacheInvocationContextFactory.createCacheRemoveInvocationContext;
import static com.netflix.hystrix.contrib.javanica.cache.CacheInvocationContextFactory.createCacheResultInvocationContext;
import static com.netflix.hystrix.contrib.javanica.utils.EnvUtils.isCompileWeaving;
import static com.netflix.hystrix.contrib.javanica.utils.ajc.AjcUtils.getAjcMethodAroundAdvice;

/**
 * Created by dmgcodevil.
 * @author hzfjd
 */
public class EduHystrixCommandBuilderFactory {

    // todo Add Cache

    private static final EduHystrixCommandBuilderFactory INSTANCE = new EduHystrixCommandBuilderFactory();

    public static EduHystrixCommandBuilderFactory getInstance() {
        return INSTANCE;
    }

    private EduHystrixCommandBuilderFactory() {

    }

    public HystrixCommandBuilder create(MetaHolder metaHolder) {
        return create(metaHolder, Collections.<HystrixCollapser.CollapsedRequest<Object, Object>>emptyList());
    }

    public <ResponseType> HystrixCommandBuilder create(MetaHolder metaHolder, Collection<HystrixCollapser.CollapsedRequest<ResponseType, Object>> collapsedRequests) {
        validateMetaHolder(metaHolder);

        return HystrixCommandBuilder.builder()
                                    .setterBuilder(createGenericSetterBuilder(metaHolder))
                                    .commandActions(createCommandActions(metaHolder))
                                    .collapsedRequests(collapsedRequests)
                                    .cacheResultInvocationContext(createCacheResultInvocationContext(metaHolder))
                                    .cacheRemoveInvocationContext(createCacheRemoveInvocationContext(metaHolder))
                                    .ignoreExceptions(metaHolder.getCommandIgnoreExceptions())
                                    .executionType(metaHolder.getExecutionType())
                                    .build();
    }

    private void validateMetaHolder(MetaHolder metaHolder) {
        Validate.notNull(metaHolder, "metaHolder is required parameter and cannot be null");
        Validate.isTrue(metaHolder.isCommandAnnotationPresent(), "hystrixCommand annotation is absent");
    }

    private GenericSetterBuilder createGenericSetterBuilder(MetaHolder metaHolder) {
        GenericSetterBuilder.Builder setterBuilder = GenericSetterBuilder.builder()
                                                                         .groupKey(metaHolder.getCommandGroupKey())
                                                                         .threadPoolKey(metaHolder.getThreadPoolKey())
                                                                         .commandKey(metaHolder.getCommandKey())
                                                                         .collapserKey(metaHolder.getCollapserKey())
                                                                         .commandProperties(metaHolder.getCommandProperties())
                                                                         .threadPoolProperties(metaHolder.getThreadPoolProperties())
                                                                         .collapserProperties(metaHolder.getCollapserProperties());
        if (metaHolder.isCollapserAnnotationPresent()) {
            setterBuilder.scope(metaHolder.getHystrixCollapser().scope());
        }
        return setterBuilder.build();
    }

    private CommandActions createCommandActions(MetaHolder metaHolder) {
        CommandAction commandAction = createCommandAction(metaHolder);
        CommandAction fallbackAction = createFallbackAction(metaHolder);
        return CommandActions.builder().commandAction(commandAction)
                             .fallbackAction(fallbackAction).build();
    }

    private CommandAction createCommandAction(MetaHolder metaHolder) {
        return new MethodExecutionAction(metaHolder.getObj(), metaHolder.getMethod(), metaHolder.getArgs(), metaHolder);
    }

    private CommandAction createFallbackAction(MetaHolder metaHolder) {

        FallbackMethod fallbackMethod = EduMethodProvider.getInstance().getFallbackMethod(metaHolder.getObj().getClass(),
                                                                                       metaHolder.getMethod(), metaHolder.isExtendedFallback());
        fallbackMethod.validateReturnType(metaHolder.getMethod());
        CommandAction fallbackAction = null;
        if (fallbackMethod.isPresent()) {

            Method fMethod = fallbackMethod.getMethod();
            if (fallbackMethod.isCommand()) {
                fMethod.setAccessible(true);
                EduHystrixCommand hystrixCommand = fMethod.getAnnotation(EduHystrixCommand.class);


                //独立支持隔离版本的fallback method的command key和thread pool key
               String fallbackMethodDefaultCommandKey= HystrixKeyUtils.getMethodSignature(fMethod,
                                                                                          metaHolder.getObj());
               String hystrixFallbackThreadPoolKey= HystrixKeyUtils.getHystrixFallbackThreadPoolKey(
                       metaHolder.getDefaultThreadPoolKey(), fallbackMethodDefaultCommandKey);

                MetaHolder fmMetaHolder = MetaHolder.builder()
                                                    .obj(metaHolder.getObj())
                                                    .method(fMethod)
                                                    .ajcMethod(getAjcMethod(metaHolder.getObj(), fMethod))
                                                    .args(metaHolder.getArgs())
                                                    .fallback(true)
                                                    .defaultCollapserKey(metaHolder.getDefaultCollapserKey())
                                                    .fallbackMethod(fMethod)
                                                    .extendedFallback(fallbackMethod.isExtended())
                                                    .fallbackExecutionType(fallbackMethod.getExecutionType())
                                                    .extendedParentFallback(metaHolder.isExtendedFallback())
                                                    .observable(ExecutionType.OBSERVABLE == fallbackMethod.getExecutionType())
                                                    .defaultCommandKey(fallbackMethodDefaultCommandKey)
                                                    .defaultGroupKey(metaHolder.getDefaultGroupKey())
                                                    .defaultThreadPoolKey(hystrixFallbackThreadPoolKey)
                                                    .defaultProperties(metaHolder.getDefaultProperties().orNull())
                                                    .hystrixCollapser(metaHolder.getHystrixCollapser())
                                                    .observableExecutionMode(hystrixCommand.observableExecutionMode())
                                                    .hystrixCommand(new HystrixCommandAnnotationAdapter(hystrixCommand)).build();
                fallbackAction = new EduLazyCommandExecutionAction(fmMetaHolder);
            } else {
                MetaHolder fmMetaHolder = MetaHolder.builder()
                                                    .obj(metaHolder.getObj())
                                                    .method(fMethod)
                                                    .fallbackExecutionType(ExecutionType.SYNCHRONOUS)
                                                    .extendedFallback(fallbackMethod.isExtended())
                                                    .extendedParentFallback(metaHolder.isExtendedFallback())
                        .ajcMethod(null) // if fallback method isn't annotated with command annotation then we don't need to get ajc method for this
                        .args(metaHolder.getArgs()).build();

                fallbackAction = new MethodExecutionAction(fmMetaHolder.getObj(), fMethod, fmMetaHolder.getArgs(), fmMetaHolder);
            }

        }
        return fallbackAction;
    }

    private Method getAjcMethod(Object target, Method fallback) {
        if (isCompileWeaving()) {
            return getAjcMethodAroundAdvice(target.getClass(), fallback);
        }
        return null;
    }

}
