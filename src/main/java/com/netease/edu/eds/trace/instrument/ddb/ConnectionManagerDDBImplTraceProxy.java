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

    @Override public Connection getConnection() {
        return super.getConnection();
    }
}
