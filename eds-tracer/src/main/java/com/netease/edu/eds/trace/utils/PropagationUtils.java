package com.netease.edu.eds.trace.utils;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.ExtraFieldPropagation;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.constants.PropagationConstants;
import com.netease.edu.eds.trace.core.UrlParameterManagerDto;
import com.netease.edu.eds.trace.instrument.http.RedirectUrlTraceMatcher;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.support.TraceKvSupport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/7/18
 **/
public class PropagationUtils {

    public static void main(String[] args) {

        test("http://test.com/abc.htm?p1=123&p2=456#hash");
        test("abc.htm?p1=123&p2=456#hash");
        test("//test.com/abc.htm?p1=123&p2=456#hash");
        test("test.com/abc.htm?p1=123&p2=456#hash");

    }

    public static void test(String location) {
        try {
            URL locationUrl = new URL(location);
            System.out.println(locationUrl.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(PropagationUtils.class);

    public static void setOriginEnvIfNotExists(TraceContext context, String currentEnv) {
        String originEnv = ExtraFieldPropagation.get(PropagationConstants.ORIGIN_ENV);
        if (StringUtils.isBlank(originEnv) && StringUtils.isNotBlank(currentEnv)) {
            ExtraFieldPropagation.set(context, PropagationConstants.ORIGIN_ENV, currentEnv);
        }
    }

    public static String getOriginEnv() {
        return ExtraFieldPropagation.get(PropagationConstants.ORIGIN_ENV);
    }

    /**
     * 如果有追踪上下文，并且指定location时站内域名，则在重定向location url上增加追踪上下问题。主要应用在跳往站外然后会回调回来的场景。
     * 
     * @param location
     * @return 如果符合，则返回新url，否则为原url
     */
    public static String addTraceContextOnUrlIfNeeded(String location) {

        Tracing tracing = Tracing.current();
        Span span = null;
        if (tracing != null) {
            Tracer tracer = tracing.tracer();
            if (tracer != null) {
                span = tracer.currentSpan();
            }
        }

        if (span == null) {
            return location;
        }

        RedirectUrlTraceMatcher redirectUrlTraceMatcher = SpringBeanFactorySupport.getBean(RedirectUrlTraceMatcher.class);

        if (redirectUrlTraceMatcher == null) {
            return location;
        }

        try {
            URL locationUrl = new URL(location);
            if (!redirectUrlTraceMatcher.needTrace(locationUrl)) {
                return location;
            }

        } catch (MalformedURLException e) {
            // 正常的外域重定向，肯定有正确的protocol和host信息。如果不正常，都当做内部域名重定向。
            if (e.getMessage() == null || !e.getMessage().startsWith("no protocol")) {
                logger.error(String.format("MalformedURLException on: %s, so don't add trace context to it", location),
                             e);
                return location;
            }

        }

        TraceContext.Injector<Map<String, String>> injector = tracing.propagation().injector(SETTER);
        Map<String, String> tracePropagationMap = new LinkedHashMap<>();

        injector.inject(span.context(), tracePropagationMap);
        String tracePropagationInfoStr = TraceContextPropagationUtils.generateTraceContextJson(tracePropagationMap);
        String uuid = TraceContextPropagationUtils.generateTraceUniqueKey();
        TraceKvSupport.unsafeSetTraceContext(uuid, tracePropagationInfoStr);

        String newLocation = addTracePropagationToLocation(location, uuid);
        if (StringUtils.isNotBlank(newLocation)) {
            location = newLocation;
        }

        return location;
    }

    private static String addTracePropagationToLocation(String location, String traceUuid) {
        if (StringUtils.isNotBlank(traceUuid)) {
            String newLocation = new UrlParameterManagerDto(location).addParamValuePairWithEncode(PropagationConstants.TRACE_CONTEXT_PROPAGATION_KEY,
                                                                                                  traceUuid).toUrl();
            if (StringUtils.isNotBlank(newLocation)) {
                return newLocation;

            }
        }

        return null;
    }

    static final Propagation.Setter<Map<String, String>, String> SETTER = new Propagation.Setter<Map<String, String>, String>() {

        @Override
        public void put(Map<String, String> carrier, String key, String value) {
            carrier.put(key, value);
        }

        @Override
        public String toString() {
            return "Map::put";
        }
    };

}
