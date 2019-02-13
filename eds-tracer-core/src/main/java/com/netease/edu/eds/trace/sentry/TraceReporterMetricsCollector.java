package com.netease.edu.eds.trace.sentry;/**
 * Created by hzfjd on 18/3/20.
 */

import com.netease.sentry.javaagent.collector.api.Collector;
import com.netease.sentry.javaagent.collector.config.CollectorsToRegister;
import com.netease.sentry.javaagent.collector.plugin.user.IntKeyValueAggregator;

/**
 * @author hzfjd
 * @create 18/3/20
 */
public class TraceReporterMetricsCollector extends Collector {

    public static IntKeyValueAggregator         intKeyValueAggregator = new IntKeyValueAggregator();
    public static TraceReporterMetricsCollector instance              = new TraceReporterMetricsCollector();

    private TraceReporterMetricsCollector() {
    }

    @Override
    public String getCollectorName() {
        return TraceReporterMetricsCollector.class.getSimpleName();
    }

    public static void onIntegerKey1Value1Stats(String key, long value) {
        intKeyValueAggregator.onValue(key, Long.valueOf(value));
    }

    static {
        instance.addModelAggregator(intKeyValueAggregator);
        CollectorsToRegister.register(instance.getCollectorName());
    }
}
