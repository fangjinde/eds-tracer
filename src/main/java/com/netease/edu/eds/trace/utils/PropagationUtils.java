package com.netease.edu.eds.trace.utils;

import org.apache.commons.lang.StringUtils;

import com.netease.edu.eds.trace.constants.PropagationConstants;

import brave.propagation.ExtraFieldPropagation;
import brave.propagation.TraceContext;

/**
 * @author hzfjd
 * @create 18/7/18
 **/
public class PropagationUtils {

    public static void setOriginEnvIfNotExists(TraceContext context, String currentEnv) {
        String originEnv = ExtraFieldPropagation.get(PropagationConstants.ORIGIN_ENV);
        if (StringUtils.isBlank(originEnv) && StringUtils.isNotBlank(currentEnv)) {
            ExtraFieldPropagation.set(context, PropagationConstants.ORIGIN_ENV, currentEnv);
        }
    }

    public static String getOriginEnv() {
        return ExtraFieldPropagation.get(PropagationConstants.ORIGIN_ENV);
    }

}
