package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/27.
 */

import brave.Span;
import com.netease.dbsupport.transaction.IDBTransactionManager;

import java.sql.Connection;

/**
 * @author hzfjd
 * @create 18/3/27
 */
public class IDBTransactionManagerTraceWrapper implements IDBTransactionManager {

    private IDBTransactionManager target;
    private DdbTracing            ddbTracing;

    public IDBTransactionManagerTraceWrapper(IDBTransactionManager target, DdbTracing ddbTracing) {
        this.target = target;
        this.ddbTracing = ddbTracing;
    }

    @Override public Connection getConnection() {
        return target.getConnection();
    }

    @Override public void releaseConnection() {
        target.releaseConnection();
    }

    @Override public void setConnection(Connection connection) {
        target.setConnection(connection);
    }

    @Override public boolean getAutoCommit() {
        return target.getAutoCommit();
    }

    @Override public void setAutoCommit(boolean b) {
        target.setAutoCommit(b);
        if (!b) {
            tagTransactionStatus("begin");
        }
    }

    @Override public void commit() {

        target.commit();
        tagTransactionStatus("commit");

    }

    @Override public void rollback() {
        target.rollback();
        tagTransactionStatus("rollback");
    }

    private void tagTransactionStatus(String status) {
        Span span = ddbTracing.tracing().tracer().currentSpan();
        if (span == null || span.isNoop()) {
            return;
        }
        span.tag("ddb_transaction", status);
        span.tag("ddb_tran_tid", String.valueOf(Thread.currentThread().getId()));
    }

    @Override public void clear() {
        target.clear();
    }
}
