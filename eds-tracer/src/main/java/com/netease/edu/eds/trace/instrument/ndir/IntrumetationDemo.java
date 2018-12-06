package com.netease.edu.eds.trace.instrument.ndir;/**
 * Created by hzfjd on 18/4/17.
 */

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.ndir.client.exception.NDirClientException;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/4/17
 */
public class IntrumetationDemo implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        //        new AgentBuilder.Default().type(
        //                ElementMatchers.namedIgnoreCase("com.netease.edu.eds.trace.utils.TargetClass")).transform(
        //                (builder, typeDescription, classloader, javaModule) ->
        //                        builder.method(ElementMatchers.namedIgnoreCase("privateGet")).intercept(
        //                                MethodDelegation.to(TraceInterceptor.class)).method(
        //                                ElementMatchers.namedIgnoreCase("pubGet")).intercept(
        //                                MethodDelegation.to(TraceInterceptor.class))
        //        ).installOn(inst);

        new AgentBuilder.Default().type(
                ElementMatchers.namedIgnoreCase("com.netease.edu.eds.trace.utils.TargetClass2")).transform(
                (builder, typeDescription, classloader, javaModule) ->
                        builder.method(ElementMatchers.namedIgnoreCase("privateGet")).intercept(
                                MethodDelegation.to(TraceInterceptor.class))
        ).installOn(inst);

//        new AgentBuilder.Default().type(
//                ElementMatchers.namedIgnoreCase("com.netease.edu.util.sql.SqlBuilder")).transform(
//                (builder, typeDescription, classloader, javaModule) ->
//                        builder.method(ElementMatchers.namedIgnoreCase("builtInCondition")).intercept(
//                                MethodDelegation.to(TraceInterceptor.class))
//        ).installOn(inst);
    }

    public static class TraceInterceptor {

        public static String around(@Argument(0) String prefix, @Origin String methodString,
                                    @SuperCall Callable<String> callable) throws NDirClientException {
            System.out.println("before");
            try {
                System.out.println("prefix=" + prefix);
                System.out.println(methodString != null ? methodString : "");
               // Method method = Method.getMethod(methodString);
                return "traced+" + callable.call();
            } catch (Exception e) {
                System.out.println("exception");
                e.printStackTrace();
                return null;
            } finally {
                System.out.println("after");
            }
        }
    }

}
