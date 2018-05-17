package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Super;
import org.springframework.beans.factory.BeanFactory;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static net.bytebuddy.matcher.ElementMatchers.isOverriddenFrom;
import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * @author hzfjd
 * @create 18/5/17
 **/
public class ThreadPoolExecutorInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("java.util.concurrent.ThreadPoolExecutor")).transform((builder,
                                                                                                               typeDescription,
                                                                                                               classloader,
                                                                                                               javaModule) -> builder.method(namedIgnoreCase("execute").and(isOverriddenFrom(Executor.class))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        public static void execute(@Argument(0) Runnable command, @Super ExecutorService executorService) {
            AsyncTracing asyncTracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                asyncTracing = beanFactory.getBean(AsyncTracing.class);
            }
            if (asyncTracing == null) {
                executorService.execute(command);
                return;
            }

            executorService.execute(asyncTracing.tracing().currentTraceContext().wrap(command));

        }

        private static void callSuper(Callable<Void> callable) {
            try {
                callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;

                } else {
                    throw new RuntimeException("unknown ThreadPoolExecutor execute exception", e);
                }
            }
        }
    }
}
