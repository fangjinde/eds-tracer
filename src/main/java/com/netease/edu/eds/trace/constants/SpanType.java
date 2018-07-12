package com.netease.edu.eds.trace.constants;

/**
 * 记录Span的类型，便于统计分析
 */
public interface SpanType {

    String TAG_KEY     = "SpanType";

    String DDB         = "ddb";
    String NDIR        = "ndir";
    String MEMCACHE    = "memcache";
    String ASYNC       = "async";
    String TRANSACTION = "transaction";
    String DUBBO       = "dubbo";
    String RABBIT      = "rabbit";
    String REDIS       = "redis";
    String HTTP        = "http";

}
