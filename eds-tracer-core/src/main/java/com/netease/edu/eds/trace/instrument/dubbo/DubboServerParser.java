package com.netease.edu.eds.trace.instrument.dubbo;

import brave.SpanCustomizer;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;

public class DubboServerParser extends DubboParser {

    @Override
    public void request(DubboAdapter adapter, RpcContext rpcContext, SpanCustomizer customizer) {
        customizer.name(spanName(adapter, rpcContext));
        String path = adapter.getRemoteAddress(rpcContext);
        if (path != null) {
            customizer.tag("consumer.address", path);
            customizer.tag("consumer.methodName", adapter.getMethodName(rpcContext));
        }
    }

    @Override
    public void response(DubboAdapter adapter, Result rpcResult, SpanCustomizer customizer) {
        if (!rpcResult.hasException()) {
            customizer.tag("provider.result", "normal");
        } else {
            customizer.tag("provider.result", "exception");

            customizer.tag("provider.exception", StringUtils.toString(rpcResult.getException()));
        }
    }
}
