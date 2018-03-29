package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/28.
 */

import com.netease.dbsupport.IConnectionManager;

import java.sql.Connection;

/**
 * using subclass ConnectionManagerDDBTracedImpl instead.
 *
 * @author hzfjd
 * @create 18/3/28
 */
@Deprecated
public class ConnectionManagerDDBImplTraceWrapper implements IConnectionManager {

    private IConnectionManager target;
    private String             ddbUrl;

    public ConnectionManagerDDBImplTraceWrapper(IConnectionManager target, String ddbUrl) {
        this.target = target;
        this.ddbUrl = ddbUrl;

    }

    @Override public boolean init() {
        return target.init();
    }

    @Override public Connection getConnection() {
        return new ConnectionTraceWrapper(target.getConnection(), ddbUrl);
    }

    @Override public long genID(Connection connection, String s) {
        return target.genID(connection, s);
    }
}
