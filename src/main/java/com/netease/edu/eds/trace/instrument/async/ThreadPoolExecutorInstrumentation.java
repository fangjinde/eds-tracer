package com.netease.edu.eds.trace.instrument.async;

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/5/17
 **/
public class ThreadPoolExecutorInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        // new AgentBuilder.Default().enableBootstrapInjection(inst, new File("/Users/hzfjd")).ignore(
        // new
        // AgentBuilder.RawMatcher.ForElementMatchers(nameStartsWith("net.bytebuddy.").or(nameStartsWith("sun.reflect.")).<TypeDescription>
        // or(isSynthetic()))).type(namedIgnoreCase("java.util.concurrent.ThreadPoolExecutor")).transform((builder,
        // typeDescription, classloader, javaModule) ->
        // builder.method(namedIgnoreCase("execute")).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
        //
        //
        // }
    }

    // public static class TraceInterceptor {
    //
    // public static void execute(@Argument(0) Runnable command, @SuperCall Callable<Void> executorService) {
    // AsyncTracing asyncTracing = null;
    // BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
    // if (beanFactory != null) {
    // asyncTracing = beanFactory.getBean(AsyncTracing.class);
    // }
    // if (asyncTracing == null) {
    // callSuper(executorService);
    // return;
    // }
    //
    // callSuper(executorService);
    //
    // // executorService.execute(asyncTracing.tracing().currentTraceContext().wrap(command));
    //
    // }
    //
    // private static void callSuper(Callable<Void> callable) {
    // try {
    // callable.call();
    // } catch (Exception e) {
    // if (e instanceof RuntimeException) {
    // throw (RuntimeException) e;
    //
    // } else {
    // throw new RuntimeException("unknown ThreadPoolExecutor execute exception", e);
    // }
    // }
    // }
    // }
}
