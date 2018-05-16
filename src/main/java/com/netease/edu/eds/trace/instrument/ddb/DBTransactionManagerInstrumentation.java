package com.netease.edu.eds.trace.instrument.ddb;

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.springframework.beans.factory.BeanFactory;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;

/**
 * @author hzfjd
 * @create 18/5/16
 **/
public class DBTransactionManagerInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("com.netease.dbsupport.transaction.impl.DBTransactionManagerImpl")).transform((builder,
                                                                                                                                       typeDescription,
                                                                                                                                       classloader,
                                                                                                                                       javaModule) -> builder.method(namedIgnoreCase("setAutoCommit").or(namedIgnoreCase("commit").or(namedIgnoreCase("rollback")))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        private static ThreadLocal<Tracer.SpanInScope> transactionSpanContext = new ThreadLocal<>();

        private static DdbTracing getDdbTracingAndFalltoSuperCallIfMissing(Callable<Void> callable) {
            DdbTracing ddbTracing = null;
            BeanFactory beanFactory = SpringBeanFactorySupport.getBeanFactory();
            if (beanFactory != null) {
                ddbTracing = beanFactory.getBean(DdbTracing.class);
            }

            if (ddbTracing == null) {
                callSuper(callable);
            }

            return ddbTracing;
        }

        private static void callSuper(Callable<Void> callable) {
            try {
                callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;

                } else {
                    throw new RuntimeException("unknown ddb Transaction operation exception", e);
                }
            }
        }

        private static void finishSpanWithTransactionStatus(DdbTracing ddbTracing, String transactionResult) {
            Span ddbTransactionSpan = ddbTracing.tracing().tracer().currentSpan();
            if (ddbTransactionSpan != null && !ddbTransactionSpan.isNoop()) {
                ddbTransactionSpan.tag("transaction_result", transactionResult);
                ddbTransactionSpan.finish();
            }

            Tracer.SpanInScope spanInScope = transactionSpanContext.get();
            if (spanInScope != null) {
                spanInScope.close();
            }
        }

        public static void setAutoCommit(@Argument(0) boolean b, @SuperCall Callable<Void> callable) {

            DdbTracing ddbTracing = getDdbTracingAndFalltoSuperCallIfMissing(callable);
            if (ddbTracing == null) {
                return;
            }

            // add trace
            if (!b) {
                Span ddbTransactionSpan = ddbTracing.tracing().tracer().nextSpan();
                Tracer.SpanInScope spanInScope = ddbTracing.tracing().tracer().withSpanInScope(ddbTransactionSpan);
                transactionSpanContext.set(spanInScope);
                ddbTransactionSpan.kind(Span.Kind.CLIENT).name("ddb_transaction").start();
            }

        }

        public static void commit(@SuperCall Callable<Void> callable) {
            DdbTracing ddbTracing = getDdbTracingAndFalltoSuperCallIfMissing(callable);
            if (ddbTracing == null) {
                return;
            }

            callSuper(callable);
            // add trace
            finishSpanWithTransactionStatus(ddbTracing, "commit");

        }

        public static void rollback(@SuperCall Callable<Void> callable) {
            DdbTracing ddbTracing = getDdbTracingAndFalltoSuperCallIfMissing(callable);
            if (ddbTracing == null) {
                return;
            }

            callSuper(callable);
            // add trace
            finishSpanWithTransactionStatus(ddbTracing, "rollback");

        }
    }
}
