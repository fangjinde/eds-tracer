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

    //Provider Collector
    private static HystrixMetricsSentryCollector           PROVIDER_COMMAND_COLLECTOR = new HystrixMetricsSentryProviderCollector();
    // Command Aggregator
    private static HystrixCommandMetricsModelAggregator    P_COMMAND_AGGREGATOR       = new HystrixCommandMetricsModelAggregator(
            "EduProviderCommandAggregator");
    //THREAD Aggregator
    private static HystrixThreadpoolMetricsModelAggregator P_THREAD_POOL_AGGREGATOR   = new HystrixThreadpoolMetricsModelAggregator(
            "EduProviderThreadPoolAggregator");

    //Consumer Collector
    private static HystrixMetricsSentryCollector        CONSUMER_COMMAND_COLLECTOR = new HystrixMetricsSentryConsumerCollector(
    );
    // Command Aggregator
    private static HystrixCommandMetricsModelAggregator C_COMMAND_AGGREGATOR       = new HystrixCommandMetricsModelAggregator(
            "EduConsumerCommandAggregator");

    //THREAD Aggregator
    private static HystrixThreadpoolMetricsModelAggregator C_THREAD_POOL_AGGREGATOR = new HystrixThreadpoolMetricsModelAggregator(
            "EduConsumerThreadPoolAggregator");

    static {
        //AP
        PROVIDER_COMMAND_COLLECTOR.addModelAggregator(P_COMMAND_AGGREGATOR);
        PROVIDER_COMMAND_COLLECTOR.addModelAggregator(P_THREAD_POOL_AGGREGATOR);
        //UP
        //Consumer
        CONSUMER_COMMAND_COLLECTOR.addModelAggregator(C_COMMAND_AGGREGATOR);
        CONSUMER_COMMAND_COLLECTOR.addModelAggregator(C_THREAD_POOL_AGGREGATOR);
    }

    public static void onNext(HystrixDashboardStream.DashboardData data) {
        for (HystrixCommandMetrics commandMetrics : data.getCommandMetrics()) {
            HystrixKeyParam hystrixKeyParam = HystrixKeyParam.parseFromKey(commandMetrics.getCommandKey().name());
            if (HystrixKeyPrefixEnum.API_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())
                || HystrixKeyPrefixEnum.UI_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                P_COMMAND_AGGREGATOR.updateMetrics(commandMetrics, hystrixKeyParam);
            } else if (HystrixKeyPrefixEnum.CONSUMER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                C_COMMAND_AGGREGATOR.updateMetrics(commandMetrics, hystrixKeyParam);
            }

        }
        for (HystrixThreadPoolMetrics threadPoolMetrics : data.getThreadPoolMetrics()) {
            HystrixKeyParam hystrixKeyParam = HystrixKeyParam.parseFromKey(threadPoolMetrics.getThreadPoolKey().name());

            if (HystrixKeyPrefixEnum.API_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())
                || HystrixKeyPrefixEnum.UI_PROVIDER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                P_THREAD_POOL_AGGREGATOR.updateMetrics(threadPoolMetrics, hystrixKeyParam);

            } else if (HystrixKeyPrefixEnum.CONSUMER.getPrefix().equals(hystrixKeyParam.getSidePrefix())) {
                C_THREAD_POOL_AGGREGATOR.updateMetrics(threadPoolMetrics, hystrixKeyParam);
            }
        }
    }

    /**
     * getCollectorName()必须在构造前指定,否则Controller的构造函数中的CollectorManager.register(this)无法正常工作.
     */
    public static class HystrixMetricsSentryProviderCollector extends HystrixMetricsSentryCollector {

        @Override
        public String getCollectorName() {
            return "EduHystrixProviderCollector";
        }
    }

    public static class HystrixMetricsSentryConsumerCollector extends HystrixMetricsSentryCollector {

        @Override
        public String getCollectorName() {
            return "EduHystrixConsumerCollector";
        }
    }

}
