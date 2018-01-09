package com.netease.edu.boot.hystrix.core.constants;

/**
 * Created by hzfjd on 18/1/5.
 */
public enum HystrixKeyPrefixEnum {
    /**
     * 消费端,不区分dubbo还是http
     */
    CONSUMER("C"),
    /**
     * 服务端,用户界面接口
     */
    UI_PROVIDER("UP"),
    /**
     * 服务端,API接口
     */
    API_PROVIDER("AP");

    public static final String PREFIX_SEPARATOR=".";


    private String prefix;

    private HystrixKeyPrefixEnum(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix(){
        return prefix;
    }

    public static boolean isPrefixValid(String prefix) {
        for (HystrixKeyPrefixEnum e : values()) {
            if (e.prefix.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStartedWithPrefix(String key) {
        if (key == null || key.length() == 0) {
            return false;
        }
        for (HystrixKeyPrefixEnum e : values()) {
            if (key.startsWith(e.prefix + PREFIX_SEPARATOR)) {
                return true;
            }

        }
        return false;
    }

    public static String getPrefix(String key) {
        if (!isStartedWithPrefix(key)) {
            return null;
        }
        return key.substring(0, key.indexOf(PREFIX_SEPARATOR));
    }

}
