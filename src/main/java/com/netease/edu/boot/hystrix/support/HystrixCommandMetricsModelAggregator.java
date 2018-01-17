package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.netease.sentry.javaagent.collector.api.SinglePrimaryKeyAggregator;
import com.netflix.hystrix.HystrixCommandMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class HystrixCommandMetricsModelAggregator
        extends SinglePrimaryKeyAggregator<HystrixCommandMetricsSentryHolder> {

    private String modelName;

    public HystrixCommandMetricsModelAggregator(String modelName) {
        this.modelName = modelName;
    }

    @Override
    protected Map<String, Object> constructItemRow(String key, HystrixCommandMetricsSentryHolder o2) {
        HystrixCommandMetrics hystrixCommandMetrics = o2.getHystrixCommandMetrics();
        if (hystrixCommandMetrics == null) {
            return null;
        }
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("Key", hystrixCommandMetrics.getCommandKey().name());
        // total
        row.put("TMT", hystrixCommandMetrics.getTotalTimeMean());
        row.put("TMT90", hystrixCommandMetrics.getTotalTimePercentile(90));
        row.put("TMT95", hystrixCommandMetrics.getTotalTimePercentile(95));
        row.put("TMT99", hystrixCommandMetrics.getTotalTimePercentile(99));
        // concurrency
        row.put("CC", hystrixCommandMetrics.getCurrentConcurrentExecutionCount());
        // tps
        row.put("Total", hystrixCommandMetrics.getHealthCounts().getTotalRequests());
        row.put("Suc", hystrixCommandMetrics.getHealthCounts().getTotalRequests()
                       - hystrixCommandMetrics.getHealthCounts().getErrorCount());
        row.put("Fail", hystrixCommandMetrics.getHealthCounts().getErrorCount());
        //rate
        row.put("FailRate", hystrixCommandMetrics.getHealthCounts().getErrorPercentage());
        row.put("FrTH", hystrixCommandMetrics.getProperties().circuitBreakerErrorThresholdPercentage().get());
        // timeout
        row.put("Timeout", hystrixCommandMetrics.getProperties().executionTimeoutInMilliseconds().get());
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

    public void updateMetrics(HystrixCommandMetrics data){
        //永远记录采样点的那个数据
        getValue(data.getCommandKey().name()).setHystrixCommandMetrics(data);
    }
}
