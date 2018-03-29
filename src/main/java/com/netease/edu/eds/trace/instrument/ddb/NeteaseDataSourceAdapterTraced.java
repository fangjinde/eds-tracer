package com.netease.edu.eds.trace.instrument.ddb;/**
 * Created by hzfjd on 18/3/29.
 */

import com.netease.edu.persist.dao.utils.NeteaseDataSourceAdapter;

import java.sql.Connection;

/**
 * @author hzfjd
 * @create 18/3/29
 */
public class NeteaseDataSourceAdapterTraced extends NeteaseDataSourceAdapter {

    @Override public Connection getConnection() {
        return new ConnectionTraceWrapper(super.getConnection(), getUrl());
    }
}
