package com.netease.edu.eds.trace.instrument.dubbo;

import brave.Span;

/**
 * @author hzfjd
 * @create 19/1/18
 **/
public class DubboInvokeTraceContext {

    private boolean shareSpanErrorMarked = false;
    private Span    shareSpan;
    private boolean shareSpanMerged      = false;

    public boolean isShareSpanErrorMarked() {
        return shareSpanErrorMarked;
    }

    public void setShareSpanErrorMarked(boolean shareSpanErrorMarked) {
        this.shareSpanErrorMarked = shareSpanErrorMarked;
    }

    public Span getShareSpan() {
        return shareSpan;
    }

    public void setShareSpan(Span shareSpan) {
        this.shareSpan = shareSpan;
    }

    public boolean isShareSpanMerged() {
        return shareSpanMerged;
    }

    public void setShareSpanMerged(boolean shareSpanMerged) {
        this.shareSpanMerged = shareSpanMerged;
    }
}
