package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/22.
 */

import com.netease.dbsupport.impl.ConnectionManagerDDBImpl;

import java.sql.Connection;

/**
 * @author hzfjd
 * @create 18/3/22
 */
public class ConnectionManagerDDBImplTraceProxy extends ConnectionManagerDDBImpl {

    @Override public long genID(Connection _connection, String _tableName) {
        return super.genID(_connection, _tableName);
    }

    @Override public Connection getConnection() {
        return super.getConnection();
    }
}
