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
public abstract class HystrixMetricsSentryCollector extends Collector {

    //AP Collector
    private static HystrixMetricsSentryCollector           AP_COMMAND_COLLECTOR      = new HystrixMetricsSentryApCollector();
    // Command Aggregator
    private static HystrixCommandMetricsModelAggregator    AP_COMMAND_AGGREGATOR     = new HystrixCommandMetricsModelAggregator(
            "EduApCommandAggregator");
    //THREAD Aggregator
    private static HystrixThreadpoolMetricsModelAggregator AP_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduApThreadPoolAggregator");

    //UP Collector
    private static HystrixMetricsSentryCollector        UP_COMMAND_COLLECTOR  = new HystrixMetricsSentryUpCollector(
    );
    // Command Aggregator
    private static HystrixCommandMetricsModelAggregator UP_COMMAND_AGGREGATOR = new HystrixCommandMetricsModelAggregator(
            "EduUpCommandAggregator");

    //THREAD Aggregator
    private static HystrixThreadpoolMetricsModelAggregator UP_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduUpThreadPoolAggregator");

    //Consumer Collector
    private static HystrixMetricsSentryCollector        C_COMMAND_COLLECTOR  = new HystrixMetricsSentryConsumerCollector(
    );
    // Command Aggregator
    private static HystrixCommandMetricsModelAggregator C_COMMAND_AGGREGATOR = new HystrixCommandMetricsModelAggregator(
            "EduConsumerCommandAggregator");

    //THREAD Aggregator
    private static HystrixThreadpoolMetricsModelAggregator C_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduConsumerThreadPoolAggregator");

    static {
        //AP
        AP_COMMAND_COLLECTOR.addModelAggregator(AP_COMMAND_AGGREGATOR);
        AP_COMMAND_COLLECTOR.addModelAggregator(AP_THREAD_POOL_AGGREGATOR);
        //UP
        UP_COMMAND_COLLECTOR.addModelAggregator(UP_COMMAND_AGGREGATOR);
        UP_COMMAND_COLLECTOR.addModelAggregator(UP_THREAD_POOL_AGGREGATOR);
        //Consumer
        C_COMMAND_COLLECTOR.addModelAggregator(C_COMMAND_AGGREGATOR);
        C_COMMAND_COLLECTOR.addModelAggregator(C_THREAD_POOL_AGGREGATOR);
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

    /**
     * getCollectorName()必须在构造前指定,否则Controller的构造函数中的CollectorManager.register(this)无法正常工作.
     */
    public static class HystrixMetricsSentryApCollector extends HystrixMetricsSentryCollector {

        @Override
        public String getCollectorName() {
            return "EduApCommandCollector";
        }
    }

    public static class HystrixMetricsSentryUpCollector extends HystrixMetricsSentryCollector {

        @Override
        public String getCollectorName() {
            return "EduUpCommandCollector";
        }
    }

    public static class HystrixMetricsSentryConsumerCollector extends HystrixMetricsSentryCollector {

        @Override
        public String getCollectorName() {
            return "EduConsumerCommandCollector";
        }
    }

}
