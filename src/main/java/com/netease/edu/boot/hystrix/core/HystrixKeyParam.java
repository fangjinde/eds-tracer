package com.netease.edu.boot.hystrix.core;/**
 * Created by hzfjd on 18/1/9.
 */

import com.netease.edu.boot.hystrix.core.constants.HystrixKeyPrefixEnum;
import com.netease.edu.boot.hystrix.core.constants.OriginApplicationConstants;
import org.apache.commons.lang.StringUtils;

/**
 * @author hzfjd
 * @create 18/1/9
 */
public class HystrixKeyParam {

    private String sidePrefix;
    private String originApplicationName;
    private String methodSignature;

    public void setSidePrefix(String sidePrefix) {
        this.sidePrefix = sidePrefix;
    }

    public String getSidePrefix() {
        return sidePrefix;
    }

    public String getOriginApplicationName() {
        return originApplicationName;
    }

    public void setOriginApplicationName(String originApplicationName) {
        this.originApplicationName = originApplicationName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public HystrixKeyParam(String sidePrefix, String methodSignature) {
        this(sidePrefix, null, methodSignature);
    }

    public HystrixKeyParam(String sidePrefix, String originApplicationName, String methodSignature) {
        this.sidePrefix = sidePrefix;
        this.originApplicationName = originApplicationName;
        this.methodSignature = methodSignature;
    }

    public static HystrixKeyParam parseFromKey(String key) {
        String prefix = HystrixKeyPrefixEnum.getPrefix(key);
        String remain = null;
        String methodSignature=null;
        if (StringUtils.isNotBlank(prefix) && StringUtils.isNotBlank(key)) {
            remain = key.substring(prefix.length() + 1);
            methodSignature=remain;
            if (StringUtils.isNotBlank(remain)){
                int originAppNameIndex= remain.indexOf(OriginApplicationConstants.SEPARATOR);
                if (originAppNameIndex>=0){
                    String originApplicationName=remain.substring(0,originAppNameIndex);
                     methodSignature=remain.substring(originAppNameIndex+1);
                    return new HystrixKeyParam(prefix, originApplicationName, methodSignature);
                }
            }
        }
        return new HystrixKeyParam(prefix, null, methodSignature);
    }

    public String generateCommandKey() {
        StringBuilder sb = new StringBuilder(sidePrefix).append(".");
        if (StringUtils.isNotBlank(originApplicationName)) {
            sb.append(originApplicationName).append(OriginApplicationConstants.SEPARATOR);
        }
        sb.append(methodSignature);
        return sb.toString();
    }

    public String generateThreadPoolKey() {
        StringBuilder sb = new StringBuilder(sidePrefix).append(".");
        if (StringUtils.isNotBlank(originApplicationName)) {
            sb.append(originApplicationName).append(OriginApplicationConstants.SEPARATOR);
        }
        sb.append(methodSignature);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "HystrixKeyParam{" +
               "sidePrefix='" + sidePrefix + '\'' +
               ", originApplicationName='" + originApplicationName + '\'' +
               ", methodSignature='" + methodSignature + '\'' +
               '}';
    }
}
