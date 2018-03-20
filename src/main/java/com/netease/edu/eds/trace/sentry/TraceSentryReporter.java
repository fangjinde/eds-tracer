package com.netease.edu.eds.trace.sentry;/**
                                          * Created by hzfjd on 18/3/20.
                                          */

import zipkin2.reporter.ReporterMetrics;

/**
 * @author hzfjd
 * @create 18/3/20
 */
public class TraceSentryReporter implements ReporterMetrics {

    static enum MetricKey {
                           messages, messagesDropped, messageBytes, spans, spanBytes, spansDropped, spansPending,
                           spanBytesPending;

        private MetricKey() {
        }
    }

    @Override
    public void incrementMessages() {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.messages.name(), 1L);
    }

    @Override
    public void incrementMessagesDropped(Throwable cause) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.messagesDropped.name(), 1);
    }

    @Override
    public void incrementSpans(int quantity) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.spans.name(),quantity);
    }

    @Override
    public void incrementSpanBytes(int quantity) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.spanBytes.name(),quantity);
    }

    @Override
    public void incrementMessageBytes(int quantity) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.messageBytes.name(),quantity);
    }

    @Override
    public void incrementSpansDropped(int quantity) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.spansDropped.name(),quantity);
    }

    @Override
    public void updateQueuedSpans(int update) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.spansPending.name(),update);
    }

    @Override
    public void updateQueuedBytes(int update) {
        TraceReporterMetricsCollector.onIntegerKey1Value1Stats(MetricKey.spanBytesPending.name(),update);
    }
}
