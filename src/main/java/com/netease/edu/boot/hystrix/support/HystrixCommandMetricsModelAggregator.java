package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netease.edu.boot.hystrix.core.HystrixKeyParam;
import com.netease.sentry.javaagent.collector.api.MultiPrimaryKeyAggregator;
import com.netease.sentry.javaagent.collector.api.PrimaryKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class HystrixCommandMetricsModelAggregator
        extends MultiPrimaryKeyAggregator<HystrixCommandMetricsModelAggregator.HystrixCommandMetricsSentryHolder> {

    private String modelName;

    public HystrixCommandMetricsModelAggregator(String modelName) {
        this.modelName = modelName;
    }

    @Override
    protected Map<String, Object> constructItemRow(PrimaryKey key, HystrixCommandMetricsSentryHolder o2) {
        HystrixCommandMetrics hystrixCommandMetrics = o2.getHystrixCommandMetrics();
        if (hystrixCommandMetrics == null) {
            return null;
        }
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("Cmd", key.get(0));
        row.put("Origin", key.get(1));
        // total
        row.put("TMT", hystrixCommandMetrics.getTotalTimeMean());
        row.put("TMT90", hystrixCommandMetrics.getTotalTimePercentile(90));
        row.put("TMT95", hystrixCommandMetrics.getTotalTimePercentile(95));
        row.put("TMT99", hystrixCommandMetrics.getTotalTimePercentile(99));
        // concurrency
        row.put("Cc", hystrixCommandMetrics.getCurrentConcurrentExecutionCount());
        // tps
        row.put("Total", hystrixCommandMetrics.getHealthCounts().getTotalRequests());
        row.put("Suc", hystrixCommandMetrics.getHealthCounts().getTotalRequests()
                       - hystrixCommandMetrics.getHealthCounts().getErrorCount());
        row.put("Fail", hystrixCommandMetrics.getHealthCounts().getErrorCount());
        //rate
        row.put("FailRate", hystrixCommandMetrics.getHealthCounts().getErrorPercentage());
        row.put("FrTh", hystrixCommandMetrics.getProperties().circuitBreakerErrorThresholdPercentage().get());
        // timeout
        row.put("Timeout", hystrixCommandMetrics.getProperties().executionTimeoutInMilliseconds().get());
        //
        boolean isoSemaphore = HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE.equals(
                hystrixCommandMetrics.getProperties().executionIsolationStrategy().get());
        row.put("Iso", isoSemaphore ? "S" : "T");
        row.put("CcTh",
                isoSemaphore ? hystrixCommandMetrics.getProperties().executionIsolationSemaphoreMaxConcurrentRequests().get() : "unknow");
        // vanilla
        row.put("EMT", hystrixCommandMetrics.getExecutionTimeMean());
        row.put("EMT90", hystrixCommandMetrics.getExecutionTimePercentile(90));
        row.put("EMT95", hystrixCommandMetrics.getExecutionTimePercentile(95));
        row.put("EMT99", hystrixCommandMetrics.getExecutionTimePercentile(99));

        return row;
    }

    @Override
    protected Class getValueType() {
        return HystrixCommandMetricsSentryHolder.class;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    private String getOriginApplicationNameWithDefault(String originApplicationName) {
        if (originApplicationName == null || originApplicationName.length() == 0) {
            return "none";
        }
        return originApplicationName;
    }

    public void updateMetrics(HystrixCommandMetrics data, HystrixKeyParam hystrixKeyParam) {
        //永远记录采样点的那个数据
        getValue(hystrixKeyParam.generateByPrefixAndMethodSignature(), getOriginApplicationNameWithDefault(
                hystrixKeyParam.getOriginApplicationName())).setHystrixCommandMetrics(
                data);
    }

    @Override
    protected int primaryKeyLength() {
        return 2;
    }

    public static class HystrixCommandMetricsSentryHolder {

        AtomicReference<HystrixCommandMetrics> hystrixCommandMetricsHolder;

        public HystrixCommandMetrics getHystrixCommandMetrics() {
            return hystrixCommandMetricsHolder.get();
        }

        public void setHystrixCommandMetrics(HystrixCommandMetrics hystrixCommandMetrics) {
            hystrixCommandMetricsHolder.set(hystrixCommandMetrics);
        }
    }
}
