package com.netease.edu.eds.trace.utils;/**
 * Created by hzfjd on 18/4/17.
 */

import com.netease.ndir.client.exception.NDirClientException;

import java.io.IOException;

/**
 * @author hzfjd
 * @create 18/4/17
 */
public class TargetClass2 {

    public String pubGet(String prefix) throws NDirClientException,IOException {
        return "pub call2:" + privateGet(prefix);
    }

    private String privateGet(String prefix) throws NDirClientException,IOException {
        return prefix + "privateGet2";
    }
}
