package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/22.
 */

import brave.Span;
import brave.Tracer;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.eds.trace.utils.SpanStringUtils;
import com.netease.framework.dbsupport.SqlManager;
import com.netease.framework.dbsupport.callback.DBListHandler;
import com.netease.framework.dbsupport.callback.DBObjectHandler;
import com.netease.framework.dbsupport.impl.DBResource;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deprecated caused by wrapper mode defect. see SqlManagerTracedImpl.class
 *
 * @author hzfjd
 * @create 18/3/22
 */
@Deprecated
public class SqlManagerTraceWrapper implements SqlManager {

    private SqlManager target;

    private DdbTracing ddbTracing;

    private static final String  SQL_IN_PATTERN = "in[\\s]+\\(((?!select|SELECT)[^\\)])+\\)";
    private static final Pattern pattern        = Pattern.compile(SQL_IN_PATTERN);

    public SqlManagerTraceWrapper(SqlManager target, DdbTracing ddbTracing) {
        this.target = target;
        this.ddbTracing = ddbTracing;
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
            ddbSpan.tag("ddb_error", ExceptionStringUtils.getStackTraceString(e));
            throw e;
        } finally {
            ddbSpan.finish();
            spanInScope.close();
            ddbTracing.tracing().tracer().withSpanInScope(previousSpan);
        }
    }

    /**
     * 所有查询操作入口
     *
     * @param sql
     * @param params
     * @return
     */
    @Override public DBResource executeQuery(String sql, List<Object> params) {
        return tracedExecute(sql, params, (sqlIn, paramsIn) -> target.executeQuery(sqlIn, paramsIn));
    }

    /**
     * 所有更新操作入口
     *
     * @param sql
     * @param params
     * @return
     */
    @Override public int updateRecords(String sql, List<Object> params) {

        return tracedExecute(sql, params, (sqlIn, paramsIn) -> target.updateRecords(sqlIn, paramsIn));
    }

    @Override public long allocateRecordId(String tableName) {
        return target.allocateRecordId(tableName);
    }

    @Override public Long queryCount(String s, List<Object> list) {
        return tracedExecute(s, list, (sqlIn, paramsIn) -> target.queryCount(sqlIn, paramsIn));
    }

    @Override public Long queryCount(String s, Object... objects) {
        return tracedExecute(s, Arrays.asList(objects), (sqlIn, paramsIn) -> target.queryCount(sqlIn, paramsIn));
    }

    @Override public <T> List<T> queryList(String s, DBObjectHandler<T> dbObjectHandler, List<Object> list) {
        return tracedExecute(s, list, (sqlIn, paramsIn) -> target.queryList(sqlIn, dbObjectHandler, paramsIn));
    }

    @Override public <T> List<T> queryList(String s, DBObjectHandler<T> dbObjectHandler, DBListHandler<T> dbListHandler,
                                           List<Object> list) {
        return tracedExecute(s, list,
                             (sqlIn, paramsIn) -> target.queryList(sqlIn, dbObjectHandler, dbListHandler, paramsIn));
    }

    @Override public <T> List<T> queryList(String sql, DBObjectHandler<T> dbObjectHandler, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.queryList(sqlIn, dbObjectHandler, paramsIn));
    }

    @Override public <T> List<T> queryList(String sql, DBObjectHandler<T> dbObjectHandler,
                                           DBListHandler<T> dbListHandler,
                                           Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.queryList(sqlIn, dbObjectHandler, dbListHandler, paramsIn));
    }

    @Override public DBResource executeQuery(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.executeQuery(sqlIn, paramsIn));
    }

    @Override public Connection getConnection() {
        return target.getConnection();
    }

    @Override public void closeDBResource(DBResource var1) {
        target.closeDBResource(var1);
    }

    @Override public boolean updateRecord(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.updateRecord(sqlIn, paramsIn));
    }

    @Override public String querySingleColInOneRecord(String sql, List<Object> params) {
        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.querySingleColInOneRecord(sqlIn, paramsIn));
    }

    @Override public String querySingleColInOneRecord(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.querySingleColInOneRecord(sqlIn, paramsIn));
    }

    @Override public boolean addRecord(String sql, List<Object> params) {

        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.addRecord(sqlIn, paramsIn));

    }

    @Override public boolean addRecord(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.addRecord(sqlIn, paramsIn));
    }

    @Override public Long[] queryObjectIds(String sql, List<Object> params) {
        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.queryObjectIds(sqlIn, paramsIn));
    }

    @Override public Long[] queryObjectIds(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.queryObjectIds(sqlIn, paramsIn));
    }

    @Override public <T> T queryObject(String sql, DBObjectHandler<T> dbObjectHandler, List<Object> params) {
        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.queryObject(sqlIn, dbObjectHandler, paramsIn));
    }

    @Override public <T> T queryObject(String sql, DBObjectHandler<T> dbObjectHandler, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.queryObject(sqlIn, dbObjectHandler, paramsIn));
    }

    @Override public Long queryObjectId(String sql, List<Object> params) {
        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.queryObjectId(sqlIn, paramsIn));
    }

    @Override public Long queryObjectId(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.queryObjectId(sqlIn, paramsIn));
    }

    @Override public boolean updateRecord(String sql, List<Object> params) {
        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.updateRecord(sqlIn, paramsIn));
    }

    @Override public int updateRecords(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.updateRecords(sqlIn, paramsIn));
    }

    @Override public boolean existRecord(String sql, List<Object> params) {
        return tracedExecute(sql, params,
                             (sqlIn, paramsIn) -> target.existRecord(sqlIn, paramsIn));
    }

    @Override public boolean existRecord(String sql, Object... params) {
        return tracedExecute(sql, Arrays.asList(params),
                             (sqlIn, paramsIn) -> target.existRecord(sqlIn, paramsIn));
    }

    @Override public Set<String> getColumns(String var1) {
        return target.getColumns(var1);
    }
}
