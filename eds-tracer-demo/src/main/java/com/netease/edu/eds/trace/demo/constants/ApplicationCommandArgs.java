package com.netease.edu.eds.trace.demo.constants;

/**
 * @author hzfjd
 * @create 18/12/7
 **/
public interface ApplicationCommandArgs {
    String[] SAME_ARGS= {
            "--spring.cloud.stream.bindings.shuffleStreamOutput.binder=serviceBus",
            "--spring.cloud.stream.bindings.shuffleStreamOutput.destination=shuffleCloudStreamDemoTopic",
            "--spring.cloud.stream.rabbit.bindings.shuffleStreamOutput.producer.prefix=${spring.profiles.active}-",
            "--spring.sleuth.web.additionalSkipPattern=/health/status|/web/echoNoTrace",
            "--hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=3600000",
            "--spring.data.elasticsearch.clusterName=ykt_trade_test",
            "--spring.data.elasticsearch.cluster-nodes=10.201.113.86:3900",
            "--deployAppName=${spring.application.name}",
            "--deployAppClusterName=${spring.application.name}_${spring.profiles.active}",
            "--edu-hystrix-demo-web_service_version_suffix=-${spring.profiles.active}",
            "--local_service_version_suffix=-${spring.profiles.active}",
            "--remote_service_version_suffix=-${spring.profiles.active}",
            "--management.health.db=false"
    };
}
