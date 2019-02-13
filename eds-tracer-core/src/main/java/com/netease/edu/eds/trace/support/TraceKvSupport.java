package com.netease.edu.eds.trace.support;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netease.edu.eds.trace.server.service.TraceService;

/**
 * @author hzfjd
 * @create 18/11/15
 **/
public class TraceKvSupport {

    private static final Logger logger = LoggerFactory.getLogger(TraceKvSupport.class);

    public static void unsafeSetTraceContext(String uuid, String traceContext) {

        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(traceContext)) {
            return;
        }

        TraceService traceService = SpringBeanFactorySupport.getBean(TraceService.class);
        if (traceService == null) {
            return;
        }

        try {
            traceService.setTraceContextByUuid(uuid, traceContext);
        } catch (Throwable e) {
            logger.error(String.format("unsafeSetTraceContext error, on uuid=%s, traceContext=%s", uuid, traceContext),
                         e);
        }

    }

    public static Object unsafeGetTraceContext(String uuid) {

        if (StringUtils.isBlank(uuid)) {
            return null;
        }

        TraceService traceService = SpringBeanFactorySupport.getBean(TraceService.class);
        if (traceService == null) {
            return null;
        }

        try {

            return traceService.getTraceContextByUuid(uuid);
        } catch (Throwable e) {
            logger.error(String.format("unsafeGetTraceContext error, on uuid=%s", uuid), e);
            return null;
        }

    }

}
