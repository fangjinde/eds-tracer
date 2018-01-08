package com.netease.edu.boot.hystrix.core.constants;

/**
 * Created by hzfjd on 18/1/5.
 */
public interface HystrixKeyConstants {

    /**
     * 消费端,不区分dubbo还是http
     */
    String CONSUMER_PREFIX     = "C";
    /**
     * 服务端,用户界面接口
     */
    String PROVIDER_UI_PREFIX  = "UP";
    /**
     * 服务端,API接口
     */
    String PROVIDER_API_PREFIX = "AP";
}
