package com.netease.edu.eds.shuffle.instrument.rabbit;

import com.netease.edu.eds.trace.support.AbstractTraceAgentInstrumetation;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author hzfjd
 * @create 18/11/23
 **/
public class SimpleMessageListenerContainerShuffleInstrumentation extends AbstractTraceAgentInstrumetation {

    private static final String INVOKE_LISTENER_METHOD_NAME = "invokeListener";

    @Override
    protected ElementMatcher.Junction defineTypeMatcher(Map<String, String> props) {
        return namedIgnoreCase("org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer");
    }

    @Override
    protected ElementMatcher.Junction defineMethodMatcher(Map<String, String> props, TypeDescription typeDescription,
                                                          ClassLoader classLoader, JavaModule module) {
        // protected void invokeListener(Channel channel, Message message) throws Exception

        ElementMatcher.Junction invokeListener2 = isDeclaredBy(typeDescription).and(namedIgnoreCase(INVOKE_LISTENER_METHOD_NAME)).and(takesArguments(2)).and(isProtected());
        return invokeListener2;
    }

    @Override
    protected Class defineInterceptorClass(Map<String, String> props) {
        return MessageListenerContainerShuffleInterceptor.class;
    }
}
