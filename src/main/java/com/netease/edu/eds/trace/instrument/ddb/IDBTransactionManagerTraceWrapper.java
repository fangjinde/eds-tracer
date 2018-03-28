package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/27.
 */

import brave.Span;
import brave.Tracer;
import com.netease.dbsupport.transaction.IDBTransactionManager;

import java.sql.Connection;

/**
 * @author hzfjd
 * @create 18/3/27
 */
public class IDBTransactionManagerTraceWrapper implements IDBTransactionManager {

    private IDBTransactionManager target;
    private DdbTracing            ddbTracing;
    private static ThreadLocal<Tracer.SpanInScope> currentSpanInScope = new ThreadLocal<>();

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

        // add trace
        Span ddbTransactionSpan = ddbTracing.tracing().tracer().nextSpan();
        Tracer.SpanInScope spanInScope = ddbTracing.tracing().tracer().withSpanInScope(ddbTransactionSpan);
        currentSpanInScope.set(spanInScope);
        ddbTransactionSpan.kind(Span.Kind.CLIENT).name("ddb_transaction").start();

    }

    @Override public void commit() {

        target.commit();

        //add trace
        finishSpanWithTransactionStatus("commit");

    }

    private void finishSpanWithTransactionStatus(String transactionResult) {
        Span ddbTransactionSpan = ddbTracing.tracing().tracer().currentSpan();
        if (ddbTransactionSpan != null && !ddbTransactionSpan.isNoop()) {
            ddbTransactionSpan.tag("transaction_result", transactionResult);
            ddbTransactionSpan.finish();
        }

        Tracer.SpanInScope spanInScope = currentSpanInScope.get();
        if (spanInScope != null) {
            spanInScope.close();
        }
    }

    @Override public void rollback() {
        
        target.rollback();

        //add trace
        finishSpanWithTransactionStatus("rollback");
    }

    @Override public void clear() {
        target.clear();
    }
}
