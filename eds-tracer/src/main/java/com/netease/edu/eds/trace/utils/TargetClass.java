package com.netease.edu.eds.trace.utils;/**
 * Created by hzfjd on 18/4/17.
 */

/**
 * @author hzfjd
 * @create 18/4/17
 */
public class TargetClass {
    public String pubGet(String prefix) {
        return "pub call:"+privateGet(prefix);
    }

    private String privateGet(String prefix) {
        return prefix+"privateGet";
    }
}
