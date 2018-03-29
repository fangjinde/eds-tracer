package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/29.
 */

import com.netease.dbsupport.impl.ConnectionManagerDDBImpl;

import java.sql.Connection;

/**
 * @author hzfjd
 * @create 18/3/29
 */
public class ConnectionManagerDDBTracedImpl extends ConnectionManagerDDBImpl {

    @Override public Connection getConnection() {
        return new ConnectionTraceWrapper(super.getConnection(), getUrl());
    }
}
