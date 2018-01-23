package com.netease.edu.boot.hystrix.support;/**
 * Created by hzfjd on 18/1/17.
 */

import com.alibaba.fastjson.JSON;
import com.netease.edu.boot.hystrix.core.HystrixKeyParam;
import com.netease.sentry.javaagent.collector.api.MultiPrimaryKeyAggregator;
import com.netease.sentry.javaagent.collector.api.PrimaryKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author hzfjd
 * @create 18/1/17
 */
public class HystrixCommandMetricsModelAggregator
        extends MultiPrimaryKeyAggregator<HystrixCommandMetricsModelAggregator.HystrixCommandMetricsSentryHolder> {

    private Logger logger = LoggerFactory.getLogger(HystrixCommandMetricsModelAggregator.class);
    private String modelName;

    public HystrixCommandMetricsModelAggregator(String modelName) {
        this.modelName = modelName;
    }

    @Override
    protected Map<String, Object> constructItemRow(PrimaryKey key, HystrixCommandMetricsSentryHolder o2) {
        try {
            return innverConstructItemRow(key, o2);
        } catch (RuntimeException e) {
            logger.warn(String.format("HystrixCommandMetricsModelAggregator constructItemRow error. key=%s, value=%s",
                                      JSON.toJSONString(key), JSON.toJSONString(o2)), e);
            return null;
        }

    }

    private Map<String, Object> innverConstructItemRow(PrimaryKey key, HystrixCommandMetricsSentryHolder o2) {
        HystrixCommandMetrics hystrixCommandMetrics = o2.getHystrixCommandMetrics();
        if (hystrixCommandMetrics == null) {
            return null;
        }
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("Cmd", key.get(0));
        if (key.getKeyLength() >= 2) {
            row.put("Origin", key.get(1));
        }
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
                isoSemaphore ? hystrixCommandMetrics.getProperties().executionIsolationSemaphoreMaxConcurrentRequests().get() : -1);
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
        HystrixCommandMetricsSentryHolder value = null;
        if (primaryKeyLength() >= 2) {
            value = getValue(hystrixKeyParam.generateByPrefixAndMethodSignature(), getOriginApplicationNameWithDefault(
                    hystrixKeyParam.getOriginApplicationName()));
        } else {
            value = getValue(hystrixKeyParam.generateByPrefixAndMethodSignature());
        }
        value.setHystrixCommandMetrics(
                data);

    }

    @Override
    protected int primaryKeyLength() {
        return 2;
    }

    public static class HystrixCommandMetricsSentryHolder {

        AtomicReference<HystrixCommandMetrics> hystrixCommandMetricsAtomicReference = new AtomicReference<HystrixCommandMetrics>();

        public HystrixCommandMetrics getHystrixCommandMetrics() {
            return hystrixCommandMetricsAtomicReference.get();
        }

        public void setHystrixCommandMetrics(HystrixCommandMetrics hystrixCommandMetrics) {
            hystrixCommandMetricsAtomicReference.set(hystrixCommandMetrics);
        }
    }

    public static class HystrixCommandNoOriginMetricsModelAggregator extends HystrixCommandMetricsModelAggregator {

        public HystrixCommandNoOriginMetricsModelAggregator(String modelName) {
            super(modelName);
        }

        @Override
        protected int primaryKeyLength() {
            return 1;
        }
    }
}
