package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/17.
 */

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hzfjd
 * @create 18/1/17
 */
@ConfigurationProperties(prefix = "eduHystrix")
public class EduHystrixProperties {

    private final Metrics metrics = new Metrics();

    public Metrics getMetrics() {
        return metrics;
    }

    public static class Metrics {

        private Integer maxConcurrentConnections = 5;
        private Integer intervalInMilliseconds   = 500;

        public Integer getMaxConcurrentConnections() {
            return maxConcurrentConnections;
        }

        public void setMaxConcurrentConnections(Integer maxConcurrentConnections) {
            this.maxConcurrentConnections = maxConcurrentConnections;
        }

        public Integer getIntervalInMilliseconds() {
            return intervalInMilliseconds;
        }

        public void setIntervalInMilliseconds(Integer intervalInMilliseconds) {
            this.intervalInMilliseconds = intervalInMilliseconds;
        }
    }

}
