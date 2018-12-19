package com.netease.edu.eds.trace.server.service;

/**
 * @author hzfjd
 * @create 18/12/12
 **/
public interface TraceService {

    String getTraceContextByUuid(String uuid);

    void setTraceContextByUuid(String uuid, String traceContext);
}
