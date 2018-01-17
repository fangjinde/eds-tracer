package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netease.sentry.javaagent.collector.api.Collector;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class HystrixMetricsSentryCollector extends Collector {

    private String collectorName;

    public HystrixMetricsSentryCollector(String collectorName) {
        this.collectorName = collectorName;
    }

    private static HystrixMetricsSentryCollector AP_COMMAND_COLLECTOR = new HystrixMetricsSentryCollector(
            "EduApCommandCollector");

    static {

    }

    @Override
    public String getCollectorName() {
        return collectorName;
    }

    public static void onNext(HystrixDashboardStream.DashboardData data) {
        for (HystrixCommandMetrics commandMetrics : data.getCommandMetrics()) {

        }
        for (HystrixThreadPoolMetrics threadPoolMetrics : data.getThreadPoolMetrics()) {

        }
    }

}
