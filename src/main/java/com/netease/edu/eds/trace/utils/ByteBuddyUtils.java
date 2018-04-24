package com.netease.edu.eds.trace.utils;/**
 * Created by hzfjd on 18/4/16.
 */

import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.ndir.client.exception.NDirClientException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

/**
 * @author hzfjd
 * @create 18/4/16
 */
public class ByteBuddyUtils {

    public static class Advice {

        public static String newGet() {
            return "print by advice";
        }
    }

    public static class LogInterceptor {

        public static String logAndCall(@SuperCall Callable<String> superCall) throws Exception {

            System.out.println("before call");
            try {
                return superCall.call();
            } finally {
                System.out.println("after call");
            }
        }
    }

    public static void nativeTest() throws IllegalAccessException, InstantiationException {
        ByteBuddyAgent.install();
        Class proxyClass = new ByteBuddy().rebase(TargetClass.class).method(
                ElementMatchers.namedIgnoreCase("pubGet")).intercept(
                MethodDelegation.to(LogInterceptor.class)).make().load(
                ByteBuddyUtils.class.getClassLoader()).getLoaded();
        System.out.println(TargetClass.class.isAssignableFrom(proxyClass));
        TargetClass proxy = (TargetClass) proxyClass.newInstance();
        System.out.println(proxy.pubGet("pp:"));
    }

    static public class TestDomain {

        public TestDomain(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        private Long   id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static void main(String[] args)
            throws IllegalAccessException, InstantiationException, NDirClientException, IOException {

        ServiceLoader<TraceAgentInstrumetation> serviceLoader = ServiceLoader.load(TraceAgentInstrumetation.class);
        Iterator<TraceAgentInstrumetation> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            TraceAgentInstrumetation traceAgentInstrumetation = iterator.next();
            System.out.println(traceAgentInstrumetation);
        }

        //System.out.println(args);

      //  String sql = SqlBuilder.inSql("name", Arrays.asList("name1", "name2"));
       // System.out.println(sql);
        //System.out.println(new TargetClass().pubGet("pp:"));

        System.out.println(new TargetClass2().pubGet("pp2:"));
        //System.out.println(args);

    }
}
