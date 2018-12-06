package com.netease.edu.eds.trace.instrument.ddb;

import brave.Span;
import com.netease.backend.db.DBConnContext;
import com.netease.backend.db.DBConnection;
import com.netease.backend.db.common.management.DDBURL;
import com.netease.backend.db.common.management.dbi.DBIContext;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import zipkin2.Endpoint;

import java.lang.instrument.Instrumentation;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * @author hzfjd
 * @create 18/5/16
 **/
public class DBConnectionInstrumentation implements TraceAgentInstrumetation {

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {
        new AgentBuilder.Default().type(namedIgnoreCase("com.netease.backend.db.DBConnection")).transform((builder,
                                                                                                           typeDescription,
                                                                                                           classloader,
                                                                                                           javaModule) -> builder.method(namedIgnoreCase("allocateRecordId").and(takesArguments(1)).or(namedIgnoreCase("prepareStatement").and(takesArguments(1))).or(namedIgnoreCase("createStatement").and(takesArguments(0)))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    public static class TraceInterceptor {

        @RuntimeType
        public static Object around(@SuperCall Callable<Object> callable, @This Object proxy) throws SQLException {
            Span span = DdbTraceContext.currentSpan();
            if (span != null && !span.isNoop()) {
                if (proxy instanceof DBConnection) {
                    DBConnection dbConnection = (DBConnection) proxy;
                    String ddbName = dbConnection.getDdbNameStr();
                    if (ddbName != null && ddbName.length() > 0) {
                        DBConnContext dbConnContext = dbConnection.getConnContext(ddbName);
                        if (dbConnContext != null) {
                            DBIContext dbiContext = dbConnContext.getContext();
                            span.tag("ActiveConnNum", String.valueOf(dbiContext.getActiveConnNum()));
                            span.tag("MaxConn", String.valueOf(dbConnContext.getDBIConfig().getMaxConn()));
                            span.tag("ClientSoTimeout", String.valueOf(dbiContext.getClientSoTimeout()));
                            span.tag("SocketTimeout", String.valueOf(dbConnContext.getDBIConfig().getSocketTimeout()));
                            DDBURL ddburl = dbiContext.getMasterURL();
                            span.remoteEndpoint(Endpoint.newBuilder().ip(ddburl.getHost()).port(ddburl.getPort()).build());
                        }
                    }

                }

            }
            try {
                return callable.call();
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    throw (SQLException) e;
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("DBConnection execute error", e);
                }
            }

        }

    }
}
