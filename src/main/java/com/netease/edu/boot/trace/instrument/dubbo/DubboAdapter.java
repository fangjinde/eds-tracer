package com.netease.edu.boot.trace.instrument.dubbo;

import com.alibaba.dubbo.rpc.RpcContext;
import com.netease.edu.boot.trace.utils.StringCaseUtils;

public abstract class DubboAdapter {

    /**
     * Returns the span name of the rpcContext.
     */
    public String getSpanName(RpcContext rpcContext) {
        return StringCaseUtils.toUnderlineName(getMethodName(rpcContext));
    }

    public String getMethodName(RpcContext rpcContext) {
        String className = rpcContext.getUrl().getPath();
        String simpleName = className.substring(className.lastIndexOf(".") + 1);
        return simpleName + "." + RpcContext.getContext().getMethodName();
    }

    /**
     * Returns the ip address and port. <p>If provider invoke this method then remote address is
     * consumer address</p>
     */
    public String getRemoteAddress(RpcContext rpcContext) {
        return rpcContext.getRemoteAddressString();
    }
}
