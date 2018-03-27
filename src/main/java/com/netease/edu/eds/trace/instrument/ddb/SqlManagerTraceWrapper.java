package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/22.
 */

import brave.Span;
import brave.Tracer;
import com.netease.framework.dbsupport.SqlManager;
import com.netease.framework.dbsupport.callback.DBListHandler;
import com.netease.framework.dbsupport.callback.DBObjectHandler;
import com.netease.framework.dbsupport.impl.DBResource;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author hzfjd
 * @create 18/3/22
 */
public class SqlManagerTraceWrapper implements SqlManager {

    private SqlManager target;

    private DdbTracing ddbTracing;

    public SqlManagerTraceWrapper(SqlManager target, DdbTracing ddbTracing) {
        this.target = target;
        this.ddbTracing = ddbTracing;
    }

    static interface SqlCommand<T> {

        T execute(String sql, List<Object> params);
    }

    private <T> T tracedExecute(String sql, List<Object> params, SqlCommand<T> sqlCommand) {
        Span previousSpan = ddbTracing.tracing().tracer().currentSpan();
        Span ddbSpan = ddbTracing.tracing().tracer().nextSpan();
        Tracer.SpanInScope spanInScope = ddbTracing.tracing().tracer().withSpanInScope(ddbSpan);
        ddbSpan.kind(Span.Kind.CLIENT).name(sql).start();

        try {
            return sqlCommand.execute(sql, params);
        } catch (RuntimeException e) {
            ddbSpan.tag("ddb_error", e.getMessage());
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
