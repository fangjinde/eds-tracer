package com.netease.edu.eds.trace.constants;

/**
 * 记录Span的类型，便于统计分析
 */
public interface SpanType {

    String TAG_KEY       = "SpanType";

    String DDB           = "Ddb";
    String NDIR          = "Ndir";
    String MEMCACHE      = "Memcache";
    String ASYNC         = "Async";
    String TRANSACTION   = "Transaction";
    String DUBBO         = "Dubbo";
    String RABBIT        = "Rabbit";
    String REDIS         = "Redis";
    String HTTP          = "Http";
    String HTTP_REDIRECT = "Redirect";

    interface AsyncSubType {

        String TAG_KEY            = "AsyncType";
        String SPRING_ASYNC       = "SpringAsync";
        String NATIVE_THREAD_POOL = "NativeThreadPool";
        String NATIVE_THREAD      = "NativeThread";
    }

}
