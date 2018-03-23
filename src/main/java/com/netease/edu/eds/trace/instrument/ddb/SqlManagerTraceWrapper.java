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

    /**
     * 所有查询操作入口
     *
     * @param sql
     * @param params
     * @return
     */
    @Override public DBResource executeQuery(String sql, List<Object> params) {
        return tracedExecute(sql, params, (sqlIn, paramsIn) -> target.executeQuery(sql, params));
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
     * 所有更新操作入口
     *
     * @param sql
     * @param params
     * @return
     */
    @Override public int updateRecords(String sql, List<Object> params) {

        return tracedExecute(sql, params, (sqlIn, paramsIn) -> target.updateRecords(sql, params));
    }

    @Override public long allocateRecordId(String tableName) {
        return target.allocateRecordId(tableName);
    }

    @Override public Long queryCount(String s, List<Object> list) {
        return target.queryCount(s, list);
    }

    @Override public Long queryCount(String s, Object... objects) {
        return target.queryCount(s, objects);
    }

    @Override public <T> List<T> queryList(String s, DBObjectHandler<T> dbObjectHandler, List<Object> list) {
        return target.queryList(s, dbObjectHandler, list);
    }

    @Override public <T> List<T> queryList(String s, DBObjectHandler<T> dbObjectHandler, DBListHandler<T> dbListHandler,
                                           List<Object> list) {
        return target.queryList(s, dbObjectHandler, dbListHandler, list);
    }

    @Override public <T> List<T> queryList(String var1, DBObjectHandler<T> var2, Object... var3) {
        return target.queryList(var1, var2, var3);
    }

    @Override public <T> List<T> queryList(String var1, DBObjectHandler<T> var2, DBListHandler<T> var3,
                                           Object... var4) {
        return target.queryList(var1, var2, var3, var4);
    }

    @Override public DBResource executeQuery(String var1, Object... var2) {
        return target.executeQuery(var1, var2);
    }

    @Override public Connection getConnection() {
        return target.getConnection();
    }

    @Override public void closeDBResource(DBResource var1) {
        target.closeDBResource(var1);
    }

    @Override public boolean updateRecord(String var1, Object... var2) {
        return target.updateRecord(var1, var2);
    }

    @Override public String querySingleColInOneRecord(String var1, List<Object> var2) {
        return target.querySingleColInOneRecord(var1, var2);
    }

    @Override public String querySingleColInOneRecord(String var1, Object... var2) {
        return target.querySingleColInOneRecord(var1, var2);
    }

    @Override public boolean addRecord(String var1, List<Object> var2) {
        return target.addRecord(var1, var2);
    }

    @Override public boolean addRecord(String var1, Object... var2) {
        return target.addRecord(var1, var2);
    }

    @Override public Long[] queryObjectIds(String var1, List<Object> var2) {
        return target.queryObjectIds(var1, var2);
    }

    @Override public Long[] queryObjectIds(String var1, Object... var2) {
        return target.queryObjectIds(var1, var2);
    }

    @Override public <T> T queryObject(String var1, DBObjectHandler<T> var2, List<Object> var3) {
        return target.queryObject(var1, var2, var3);
    }

    @Override public <T> T queryObject(String var1, DBObjectHandler<T> var2, Object... var3) {
        return target.queryObject(var1, var2, var3);
    }

    @Override public Long queryObjectId(String var1, List<Object> var2) {
        return target.queryObjectId(var1, var2);
    }

    @Override public Long queryObjectId(String var1, Object... var2) {
        return target.queryObjectId(var1, var2);
    }

    @Override public boolean updateRecord(String var1, List<Object> var2) {
        return target.updateRecord(var1, var2);
    }

    @Override public int updateRecords(String var1, Object... var2) {
        return target.updateRecords(var1, var2);
    }

    @Override public boolean existRecord(String var1, List<Object> var2) {
        return target.existRecord(var1, var2);
    }

    @Override public boolean existRecord(String var1, Object... var2) {
        return target.existRecord(var1, var2);
    }

    @Override public Set<String> getColumns(String var1) {
        return target.getColumns(var1);
    }
}
