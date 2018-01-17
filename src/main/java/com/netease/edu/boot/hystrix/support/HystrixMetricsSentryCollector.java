package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netease.edu.boot.hystrix.core.HystrixKeyParam;
import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
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

    //AP COMMAND
    private static HystrixMetricsSentryCollector           AP_COMMAND_COLLECTOR      = new HystrixMetricsSentryCollector(
            "EduApCommandCollector");
    private static HystrixCommandMetricsModelAggregator    AP_COMMAND_AGGREGATOR     = new HystrixCommandMetricsModelAggregator(
            "EduApCommandAggregator");
    //AP THREAD
    private static HystrixMetricsSentryCollector           AP_THREAD_POOL_COLLECTOR  = new HystrixMetricsSentryCollector(
            "EduApThreadPoolCollector");
    private static HystrixThreadpoolMetricsModelAggregator AP_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduApThreadPoolAggregator");

    //UP COMMAND
    private static HystrixMetricsSentryCollector        UP_COMMAND_COLLECTOR  = new HystrixMetricsSentryCollector(
            "EduUpCommandCollector");
    private static HystrixCommandMetricsModelAggregator UP_COMMAND_AGGREGATOR = new HystrixCommandMetricsModelAggregator(
            "EduUpCommandAggregator");

    //UP THREAD
    private static HystrixMetricsSentryCollector           UP_THREAD_POOL_COLLECTOR  = new HystrixMetricsSentryCollector(
            "EduUpThreadPoolCollector");
    private static HystrixThreadpoolMetricsModelAggregator UP_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduUpThreadPoolAggregator");

    //Consumer COMMAND
    private static HystrixMetricsSentryCollector        C_COMMAND_COLLECTOR  = new HystrixMetricsSentryCollector(
            "EduConsumerCommandCollector");
    private static HystrixCommandMetricsModelAggregator C_COMMAND_AGGREGATOR = new HystrixCommandMetricsModelAggregator(
            "EduConsumerCommandAggregator");

    //Consumer THREAD
    private static HystrixMetricsSentryCollector           C_THREAD_POOL_COLLECTOR  = new HystrixMetricsSentryCollector(
            "EduConsumerThreadPoolCollector");
    private static HystrixThreadpoolMetricsModelAggregator C_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduConsumerThreadPoolAggregator");

    static {
        //AP
        AP_COMMAND_COLLECTOR.addModelAggregator(AP_COMMAND_AGGREGATOR);
        AP_THREAD_POOL_COLLECTOR.addModelAggregator(AP_THREAD_POOL_AGGREGATOR);
        //UP
        UP_COMMAND_COLLECTOR.addModelAggregator(UP_COMMAND_AGGREGATOR);
        UP_THREAD_POOL_COLLECTOR.addModelAggregator(UP_THREAD_POOL_AGGREGATOR);
        //Consumer
        C_COMMAND_COLLECTOR.addModelAggregator(C_COMMAND_AGGREGATOR);
        C_THREAD_POOL_COLLECTOR.addModelAggregator(C_THREAD_POOL_AGGREGATOR);
    }

    @Override
    public String getCollectorName() {
        return collectorName;
    }

    public static void onNext(HystrixDashboardStream.DashboardData data) {
        for (HystrixCommandMetrics commandMetrics : data.getCommandMetrics()) {
            HystrixKeyParam hystrixKeyParam = HystrixKeyParam.parseFromKey(commandMetrics.getCommandKey().name());
            if (HystrixKeyPrefixEnum.API_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                AP_COMMAND_AGGREGATOR.updateMetrics(commandMetrics, hystrixKeyParam);
            } else if (HystrixKeyPrefixEnum.UI_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                UP_COMMAND_AGGREGATOR.updateMetrics(commandMetrics, hystrixKeyParam);
            } else if (HystrixKeyPrefixEnum.CONSUMER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                C_COMMAND_AGGREGATOR.updateMetrics(commandMetrics, hystrixKeyParam);
            }

        }
        for (HystrixThreadPoolMetrics threadPoolMetrics : data.getThreadPoolMetrics()) {
            HystrixKeyParam hystrixKeyParam = HystrixKeyParam.parseFromKey(threadPoolMetrics.getThreadPoolKey().name());
            if (HystrixKeyPrefixEnum.API_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {

            } else if (HystrixKeyPrefixEnum.UI_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {

            } else if (HystrixKeyPrefixEnum.CONSUMER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {

            }
        }
    }

}
