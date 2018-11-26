package com.netease.edu.eds.trace.instrument.rabbit;

import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/7/10
 **/
public class AbstractMessageListenerContainerInstrumentation extends AbstractTraceAgentInstrumetation {

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // protected void invokeListener(Channel channel, Message message) throws Exception
        ElementMatcher.Junction invokeListener2 = isDeclaredBy(typeDescription).and(namedIgnoreCase("invokeListener")).and(takesArguments(2)).and(isProtected());
        return invokeListener2;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return MessageListenerContainerTraceInterceptor.class;
    }

}
