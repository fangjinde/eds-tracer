package com.netease.edu.eds.shuffle.instrument.jobservice;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.shuffle.core.ShuffleSwitch;
import com.netease.edu.eds.trace.core.Invoker;
import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.job.share.dto.TaskIdDTO;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/10/24
 **/
public class JobServiceShuffleInstrument extends AbstractTraceAgentInstrumetation {

    private static final String SUBMIT_DELAY_TASK_METHOD = "submitDelayTask";
    private static final String UPDATE_DELAY_TASK_METHOD = "updateDelayTask";
    private static final String CANCEL_DELAY_TASK_METHOD = "cancelDelayTask";
    private static final String UPSERT_DELAY_TASK_METHOD = "upsertDelayTask";

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return ElementMatchers.namedIgnoreCase("com.netease.edu.job.service.impl.JobServiceImpl");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {

        // public ResultDTO<Long> submitDelayTask(TaskIdDTO idDTO, String params, Long triggerTime)
        ElementMatcher.Junction submitDelayTask3 = isDeclaredBy(typeDescription).and(isPublic()).and(namedIgnoreCase(SUBMIT_DELAY_TASK_METHOD)).and(takesArguments(3));
        // public ResultDTO updateDelayTask(TaskIdDTO idDTO, String params, Long triggerTime)
        ElementMatcher.Junction updateDelayTask3 = namedIgnoreCase(UPDATE_DELAY_TASK_METHOD).and(takesArguments(3)).and(isDeclaredBy(typeDescription)).and(isPublic());
        // public ResultDTO upsertDelayTask(TaskIdDTO idDTO, String params, Long triggerTime)
        ElementMatcher.Junction upsertDelayTask3 = namedIgnoreCase(UPSERT_DELAY_TASK_METHOD).and(takesArguments(3)).and(isDeclaredBy(typeDescription)).and(isPublic());
        // public ResultDTO cancelDelayTask(TaskIdDTO idDTO)
        ElementMatcher.Junction cancelDelayTask1 = namedIgnoreCase(CANCEL_DELAY_TASK_METHOD).and(takesArguments(1)).and(isDeclaredBy(typeDescription)).and(isPublic());
        return submitDelayTask3.or(updateDelayTask3).or(upsertDelayTask3).or(cancelDelayTask1);
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return Interceptor.class;
    }

    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@AllArguments Object[] args, @Morph Invoker invoker, @Origin Method method,
                                       @This Object proxy) {

            if (!ShuffleSwitch.isTurnOn()) {
                return invoker.invoke(args);
            }

            // do for "send" method
            if (method.getName().equals(SUBMIT_DELAY_TASK_METHOD) || method.getName().equals(UPDATE_DELAY_TASK_METHOD)
                || method.getName().equals(CANCEL_DELAY_TASK_METHOD)
                || method.getName().equals(UPSERT_DELAY_TASK_METHOD)) {
                return shuffleEnvForOpsOnDelayTask(args, invoker, method, proxy);
            } else {
                return invoker.invoke(args);
            }

        }

        // 外层有dubbo或者controller服务暴露，复用span,更改环境即可。
        private static Object shuffleEnvForOpsOnDelayTask(Object[] args, Invoker invoker, Method method, Object proxy) {
            Tracer tracer = Tracing.currentTracer();
            Span span = null;
            if (tracer != null) {
                span = tracer.currentSpan();
            }
            if (span == null) {
                return invoker.invoke(args);
            }

            Object arg0 = args[0];
            if (!(arg0 instanceof TaskIdDTO)) {
                return invoker.invoke(args);
            }

            TaskIdDTO taskIdDTO = (TaskIdDTO) arg0;
            String originEnv = PropagationUtils.getOriginEnv();
            if (StringUtils.isNotBlank(originEnv)) {
                taskIdDTO.setEnvironment(originEnv);
            }
            return invoker.invoke(args);

        }

    }

}
