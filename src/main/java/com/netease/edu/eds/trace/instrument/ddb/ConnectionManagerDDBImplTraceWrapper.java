package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/28.
 */

import com.netease.dbsupport.IConnectionManager;

import java.sql.Connection;

/**
 * @author hzfjd
 * @create 18/3/28
 */
public class ConnectionManagerDDBImplTraceWrapper implements IConnectionManager {

    private IConnectionManager target;
    private DdbTracing         ddbTracing;

    public ConnectionManagerDDBImplTraceWrapper(IConnectionManager target, DdbTracing ddbTracing) {
        this.target = target;
        this.ddbTracing = ddbTracing;

    }

    @Override public boolean init() {
        return target.init();
    }

    @Override public Connection getConnection() {
        return target.getConnection();
    }

    @Override public long genID(Connection connection, String s) {
        return target.genID(connection, s);
    }
}
