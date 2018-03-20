package com.netease.edu.boot.trace.sentry;/**
 * Created by hzfjd on 18/3/20.
 */

import zipkin2.reporter.ReporterMetrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author hzfjd
 * @create 18/3/20
 */
public class ReporterStat implements ReporterMetrics {

    private AtomicLong messagesReported;
    private AtomicLong messagesDropped;
    private AtomicLong messageBytesReported;
    private AtomicLong spansReported;
    private AtomicLong spansDropped;
    private AtomicLong spanBytesReported;
    private AtomicLong spansPending;
    private AtomicLong spanBytesPending;

    @Override public void incrementMessages() {

    }

    @Override public void incrementMessagesDropped(Throwable cause) {

    }

    @Override public void incrementSpans(int quantity) {

    }

    @Override public void incrementSpanBytes(int quantity) {

    }

    @Override public void incrementMessageBytes(int quantity) {

    }

    @Override public void incrementSpansDropped(int quantity) {

    }

    @Override public void updateQueuedSpans(int update) {

    }

    @Override public void updateQueuedBytes(int update) {

    }
}
