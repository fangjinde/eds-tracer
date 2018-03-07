package com.netease.edu.boot.trace.instrument.dubbo;

import brave.SpanCustomizer;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;

public class DubboClientParser extends DubboParser {

    @Override
    public void request(DubboAdapter adapter, RpcContext rpcContext, SpanCustomizer customizer) {
        customizer.name(spanName(adapter, rpcContext));
        String path = adapter.getRemoteAddress(rpcContext);
        if (path != null) {
            customizer.tag("provider.address", path);
            customizer.tag("provider.methodName", adapter.getMethodName(rpcContext));
        }
    }

    @Override
    public void response(DubboAdapter adapter, Result rpcResult, SpanCustomizer customizer) {
        if (!rpcResult.hasException()) {
            customizer.tag("consumer.result", "normal");
        } else {
            customizer.tag("consumer.result", "exception");
            customizer.tag("consumer.exception",rpcResult.getException().getMessage());
        }
    }
}
