package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/29.
 */

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.eds.trace.utils.SpanStringUtils;
import com.netease.framework.dbsupport.impl.DBResource;
import com.netease.framework.dbsupport.impl.SqlManagerImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import zipkin2.Endpoint;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 暂时使用子类进行拦截处理.会破坏原有SqlManagerImpl的子类的实现,如果有的话.
 * //TODO 后期改成字节码修改方案,直接修改SqlManagerImpl类本身的实现.
 *
 * @author hzfjd
 * @create 18/3/29
 */
public class SqlManagerTracedImpl extends SqlManagerImpl {

    private static final String  SQL_IN_PATTERN = "in[\\s]+\\(((?!select|SELECT)[^\\)])+\\)";
    private static final Pattern pattern        = Pattern.compile(SQL_IN_PATTERN);

    private DdbTracing ddbTracing;

    @Autowired
    public void setDdbTracing(DdbTracing ddbTracing) {
        this.ddbTracing = ddbTracing;
    }

    @Override
    public Connection getConnection() {
        Connection connection = super.getConnection();
        if (connection instanceof ConnectionTraceWrapper) {
            ConnectionTraceWrapper connectionTraceWrapper = (ConnectionTraceWrapper) connection;
            Span ddbSpan = ddbTracing.tracing().tracer().currentSpan();
            if (ddbSpan != null && !ddbSpan.isNoop()) {
                Endpoint.Builder endpointBuilder = Endpoint.newBuilder().serviceName("ddb").port(
                        connectionTraceWrapper.getPort());
                if (endpointBuilder.parseIp(connectionTraceWrapper.getHost())) {
                    ddbSpan.remoteEndpoint(endpointBuilder.build());
                }
            }
        }

        return connection;
    }

    @Override
    public DBResource executeQuery(String sql, List<Object> params) {
        return tracedExecute(sql, params, (String sqlIn, List<Object> paramsIn) -> super.executeQuery(sqlIn, paramsIn));
    }

    @Override
    public int updateRecords(String sql, List<Object> params) {
        return tracedExecute(sql, params,
                             (String sqlIn, List<Object> paramsIn) -> super.updateRecords(sqlIn, paramsIn));
    }

    static interface SqlCommand<T> {

        T execute(String sql, List<Object> params);
    }

    private String getSpanName(String sql) {
        return SpanStringUtils.filterSpanName(sql);
    }

    private StringBuilder getSqlDetail(String sql, List<Object> params) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(sql)) {
            return sb;
        }

        sql = sql.toLowerCase();
        if (sql.length() > 200) {
            shortenSqlForInCondition(sb, sql);
        } else {
            sb.append(sql);
        }
        sb.append(" params: ").append(params);

        return sb;
    }

    private void shortenSqlForInCondition(StringBuilder sb, String sql) {

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

    private <T> T tracedExecute(String sql, List<Object> params, SqlCommand<T> sqlCommand) {
        Span previousSpan = ddbTracing.tracing().tracer().currentSpan();
        Span ddbSpan = ddbTracing.tracing().tracer().nextSpan();
        Tracer.SpanInScope spanInScope = ddbTracing.tracing().tracer().withSpanInScope(ddbSpan);
        ddbSpan.kind(Span.Kind.CLIENT).name(getSpanName(sql)).tag("sql_detail",
                                                                  getSqlDetail(sql, params).toString()).start();

        try {
            return sqlCommand.execute(sql, params);
        } catch (RuntimeException e) {
            ddbSpan.tag("has_error", String.valueOf(true));
            ddbSpan.tag("ddb_error", ExceptionStringUtils.getStackTraceString(e));
            throw e;
        } finally {
            ddbSpan.finish();
            spanInScope.close();
            ddbTracing.tracing().tracer().withSpanInScope(previousSpan);
        }
    }

}
