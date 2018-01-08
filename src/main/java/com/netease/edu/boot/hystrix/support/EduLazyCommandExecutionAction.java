package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 17/12/27.
 */

import com.netflix.hystrix.HystrixInvokable;
import com.netflix.hystrix.contrib.javanica.command.CommandAction;
import com.netflix.hystrix.contrib.javanica.command.CommandExecutionAction;
import com.netflix.hystrix.contrib.javanica.command.ExecutionType;
import com.netflix.hystrix.contrib.javanica.command.MetaHolder;
import com.netflix.hystrix.contrib.javanica.exception.CommandActionExecutionException;
import org.apache.commons.lang3.StringUtils;

/**
 * @author hzfjd
 * @create 17/12/27
 */
public class EduLazyCommandExecutionAction implements CommandAction {

    private MetaHolder originalMetaHolder;

    public EduLazyCommandExecutionAction(MetaHolder metaHolder) {
        this.originalMetaHolder = metaHolder;
    }

    @Override
    public MetaHolder getMetaHolder() {
        return originalMetaHolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(ExecutionType executionType) throws CommandActionExecutionException {
        HystrixInvokable command = EduHystrixCommandFactory.getInstance().createDelayed(createCopy(originalMetaHolder, executionType));
        return new CommandExecutionAction(command, originalMetaHolder).execute(executionType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeWithArgs(ExecutionType executionType, Object[] args) throws CommandActionExecutionException {
        HystrixInvokable command = EduHystrixCommandFactory.getInstance().createDelayed(createCopy(originalMetaHolder, executionType, args));
        return new CommandExecutionAction(command, originalMetaHolder).execute(executionType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActionName() {
        return StringUtils.isNotEmpty(originalMetaHolder.getHystrixCommand().commandKey()) ?
                originalMetaHolder.getHystrixCommand().commandKey()
                : originalMetaHolder.getDefaultCommandKey();
    }

    // todo dmgcodevil: move it to MetaHolder class ?
    private MetaHolder createCopy(MetaHolder source, ExecutionType executionType) {
        return MetaHolder.builder()
                         .obj(source.getObj())
                         .method(source.getMethod())
                         .ajcMethod(source.getAjcMethod())
                         .fallbackExecutionType(source.getFallbackExecutionType())
                         .extendedFallback(source.isExtendedFallback())
                         .extendedParentFallback(source.isExtendedParentFallback())
                         .executionType(executionType)
                         .args(source.getArgs())
                         .observable(source.isObservable())
                         .observableExecutionMode(source.getObservableExecutionMode())
                         .defaultCollapserKey(source.getDefaultCollapserKey())
                         .defaultCommandKey(source.getDefaultCommandKey())
                         .defaultGroupKey(source.getDefaultGroupKey())
                         .defaultThreadPoolKey(source.getDefaultThreadPoolKey())
                         .defaultProperties(source.getDefaultProperties().orNull())
                         .hystrixCollapser(source.getHystrixCollapser())
                         .hystrixCommand(source.getHystrixCommand()).build();
    }

    private MetaHolder createCopy(MetaHolder source, ExecutionType executionType, Object[] args) {
        return MetaHolder.builder()
                         .obj(source.getObj())
                         .method(source.getMethod())
                         .executionType(executionType)
                         .ajcMethod(source.getAjcMethod())
                         .fallbackExecutionType(source.getFallbackExecutionType())
                         .extendedParentFallback(source.isExtendedParentFallback())
                         .extendedFallback(source.isExtendedFallback())
                         .args(args)
                         .observable(source.isObservable())
                         .observableExecutionMode(source.getObservableExecutionMode())
                         .defaultCollapserKey(source.getDefaultCollapserKey())
                         .defaultCommandKey(source.getDefaultCommandKey())
                         .defaultGroupKey(source.getDefaultGroupKey())
                         .defaultThreadPoolKey(source.getDefaultThreadPoolKey())
                         .defaultProperties(source.getDefaultProperties().orNull())
                         .hystrixCollapser(source.getHystrixCollapser())
                         .hystrixCommand(source.getHystrixCommand()).build();
    }

}
