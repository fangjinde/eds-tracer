package com.netease.edu.eds.trace.instrument.ddb;

import brave.Span;
import brave.Tracer;
import com.netease.backend.db.DBResultSet;
import com.netease.backend.db.common.utils.OneBasedArray;
import com.netease.backend.db.result.Record;
import com.netease.edu.eds.trace.spi.TraceAgentInstrumetation;
import com.netease.edu.eds.trace.support.DefaultAgentBuilderListener;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.ExceptionHandler;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.eds.trace.utils.JsonUtils;
import com.netease.edu.eds.trace.utils.SpanStringUtils;
import com.netease.framework.dbsupport.callback.DBListHandler;
import com.netease.framework.dbsupport.callback.DBObjectHandler;
import com.netease.framework.dbsupport.impl.DBResource;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.bytebuddy.matcher.ElementMatchers.namedIgnoreCase;
import static net.bytebuddy.matcher.ElementMatchers.takesGenericArgument;

/**
 * @author hzfjd
 * @create 18/5/16
 **/
public class SqlManagerInstrumentation implements TraceAgentInstrumetation {

    private static Logger logger = LoggerFactory.getLogger(SqlManagerInstrumentation.class);

    @Override
    public void premain(Map<String, String> props, Instrumentation inst) {

        // new
        // AgentBuilder.Default().type(namedIgnoreCase("com.netease.framework.dbsupport.impl.SqlManagerImpl")).transform((builder,
        // typeDescription,
        // classloader,
        // javaModule) ->
        // builder.method(namedIgnoreCase("allocateRecordId").or(namedIgnoreCase("executeQuery").and(takesGenericArgument(1,
        // TypeDescription.Generic.Builder.parameterizedType(List.class,
        // Object.class).build()))).or(namedIgnoreCase("updateRecords").and(takesGenericArgument(1,
        // TypeDescription.Generic.Builder.parameterizedType(List.class,
        // Object.class).build())))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);
        //

        new AgentBuilder.Default().type(namedIgnoreCase("com.netease.framework.dbsupport.impl.SqlManagerImpl")).transform((builder,
                                                                                                                           typeDescription,
                                                                                                                           classloader,
                                                                                                                           javaModule) -> builder.method(namedIgnoreCase("allocateRecordId").or(namedIgnoreCase("existRecord")).or(namedIgnoreCase("queryCount")).or(namedIgnoreCase("queryList")).or(namedIgnoreCase("queryObject")).or(namedIgnoreCase("queryObjectId")).or(namedIgnoreCase("queryObjectIds")).or(namedIgnoreCase("querySingleColInOneRecord").and(takesGenericArgument(1,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          TypeDescription.Generic.Builder.parameterizedType(List.class,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            Object.class).build()))).or(namedIgnoreCase("updateRecords").and(takesGenericArgument(1,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  TypeDescription.Generic.Builder.parameterizedType(List.class,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    Object.class).build())))).intercept(MethodDelegation.to(TraceInterceptor.class))).with(DefaultAgentBuilderListener.getInstance()).installOn(inst);

    }

    private static String getResultStringFromDBResource(DBResource dBResource) {
        try {
            if (dBResource.getResultSet() instanceof DBResultSet) {
                DBResultSet dbResultSet = (DBResultSet) dBResource.getResultSet();
                OneBasedArray<Record> records = dbResultSet.getAllRecord();
                List<List<Object>> rowsList = new ArrayList<>();
                for (int row = 0; row < records.size(); row++) {
                    Record record = records.get(row);
                    List<Object> oneRowList = new ArrayList<>();
                    rowsList.add(oneRowList);
                    OneBasedArray<Object> values = record.getValues();
                    for (int col = 0; col < values.size(); col++) {
                        oneRowList.add(values.get(col));
                    }

                }
                return JsonUtils.toJson(rowsList);

            }
        } catch (Exception e) {
            logger.error("getResultStringFromDBResource error", e);

        }
        return "";
    }

    public static class TraceInterceptor {

        private static final String  SQL_IN_PATTERN = "in[\\s]+\\(((?!select|SELECT)[^\\)])+\\)";
        private static final Pattern pattern        = Pattern.compile(SQL_IN_PATTERN);

        private static <T> T tracedExecute(DdbTracing ddbTracing, String sql, List<Object> params,
                                           Callable<T> callable) {

            // 避免重复拦截
            if (DdbTraceContext.currentSpan() != null) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw ExceptionHandler.wrapToRuntimeException(e);
                }
            }

            Span ddbSpan = ddbTracing.tracing().tracer().nextSpan();

            try (Tracer.SpanInScope spanInScope = ddbTracing.tracing().tracer().withSpanInScope(ddbSpan)) {
                ddbSpan.kind(Span.Kind.CLIENT).name(getSpanName(sql)).tag("sql_detail",
                                                                          getSqlDetail(sql, params,
                                                                                       ddbSpan).toString()).start();
                DdbTraceContext.setSpan(ddbSpan);
                T result = callable.call();

                if (result instanceof DBResource) {
                    DBResource dBResource = (DBResource) result;
                    ddbSpan.tag("return", getResultStringFromDBResource(dBResource));
                } else {
                    ddbSpan.tag("return", JsonUtils.toJson(result));
                }

                return result;
            } catch (Exception e) {
                ddbSpan.tag("has_error", String.valueOf(true));
                ddbSpan.tag("ddb_error", ExceptionStringUtils.getStackTraceString(e));
                throw ExceptionHandler.wrapToRuntimeException(e);
            } finally {
                DdbTraceContext.setSpan(null);
                ddbSpan.finish();
            }
        }

        private static String getSpanName(String sql) {
            return SpanStringUtils.filterSpanName(sql);
        }

        private static StringBuilder getSqlDetail(String sql, List<Object> params, Span span) {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isEmpty(sql)) {
                return sb;
            }

            sql = sql.toLowerCase();
            if (sql.length() > 200) {
                span.tag("longSql", String.valueOf(true));
                shortenSqlForInCondition(sb, sql);
            } else {
                sb.append(sql);
            }
            if (params != null && params.size() > 50) {
                span.tag("longParams", String.valueOf(true));
            }
            sb.append(" params: ").append(params);

            return sb;
        }

        private static void shortenSqlForInCondition(StringBuilder sb, String sql) {

            Matcher matcher = pattern.matcher(sql);
            int head = 0;
            int start;
            int newEnd;
            int end;
            int count;
            int maxCount = 5;
            while (matcher.find()) {
                count = 0;
                start = matcher.start();
                end = matcher.end();
                newEnd = end;
                for (int i = start; i < end; ++i) {
                    if (',' == sql.charAt(i)) {
                        count++;
                        if (count == maxCount) {
                            newEnd = i;
                            break;
                        }
                    }
                }

                if (newEnd < end) {
                    sb.append(sql, head, newEnd).append("...)");
                } else {
                    sb.append(sql, head, end);
                }

                head = end;
            }

            if (head < sql.length()) {
                sb.append(sql, head, sql.length());
            }

        }

        private static DdbTracing getDdbTracing() {
            DdbTracing ddbTracing = SpringBeanFactorySupport.getBean(DdbTracing.class);
            return ddbTracing;
        }

        private static <R> R callSuper(Callable<R> callable) {
            try {
                return callable.call();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;

                } else {
                    throw new RuntimeException("unknown ddb Transaction operation exception", e);
                }
            }
        }

        public static long allocateRecordId(@Argument(0) String tableName, @SuperCall Callable<Long> callable) {

            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            Span previousSpan = ddbTracing.tracing().tracer().currentSpan();
            Span ddbSpan = ddbTracing.tracing().tracer().nextSpan();
            Tracer.SpanInScope spanInScope = ddbTracing.tracing().tracer().withSpanInScope(ddbSpan);
            ddbSpan.kind(Span.Kind.CLIENT).name("genID").tag("tableName", tableName).start();

            try {
                DdbTraceContext.setSpan(ddbSpan);
                Long result = callable.call();
                String retJson = JsonUtils.toJson(result);
                ddbSpan.tag("return", retJson);
                return result;
            } catch (Exception e) {
                ddbSpan.tag("has_error", String.valueOf(true));
                ddbSpan.tag("ddb_error", ExceptionStringUtils.getStackTraceString(e));
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException("Sql Manager execute error", e);
                }

            } finally {
                DdbTraceContext.setSpan(null);
                ddbSpan.finish();
                spanInScope.close();
                ddbTracing.tracing().tracer().withSpanInScope(previousSpan);
            }

        }

        public static int updateRecords(@Argument(0) String sql, @Argument(1) List<Object> params,
                                        @SuperCall Callable<Integer> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        // public static DBResource executeQuery(@Argument(0) String sql, @Argument(1) List<Object> params,
        // @SuperCall Callable<DBResource> callable) {
        // DdbTracing ddbTracing = getDdbTracing();
        // if (ddbTracing == null) {
        // return callSuper(callable);
        // }
        //
        // return tracedExecute(ddbTracing, sql, params, callable);
        // }

        public static boolean existRecord(@Argument(0) String sql, @Argument(1) List<Object> params,
                                          @SuperCall Callable<Boolean> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        public static Long queryCount(@Argument(0) String sql, @Argument(1) List<Object> params,
                                      @SuperCall Callable<Long> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        public static <T> List<T> queryList(@Argument(0) String sql, @Argument(1) DBObjectHandler<T> handler,
                                            @Argument(2) DBListHandler<T> listHandler, @Argument(3) List<Object> params,
                                            @SuperCall Callable<List<T>> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        public static <T> T queryObject(@Argument(0) String sql, @Argument(1) DBObjectHandler<T> handler,
                                        @Argument(2) List<Object> params, @SuperCall Callable<T> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        public static Long queryObjectId(@Argument(0) String sql, @Argument(1) List<Object> params,
                                         @SuperCall Callable<Long> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        public static Long[] queryObjectIds(@Argument(0) String sql, @Argument(1) List<Object> params,
                                            @SuperCall Callable<Long[]> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }

        public static String querySingleColInOneRecord(@Argument(0) String sql, @Argument(1) List<Object> params,
                                                       @SuperCall Callable<String> callable) {
            DdbTracing ddbTracing = getDdbTracing();
            if (ddbTracing == null) {
                return callSuper(callable);
            }

            return tracedExecute(ddbTracing, sql, params, callable);
        }
    }
}
