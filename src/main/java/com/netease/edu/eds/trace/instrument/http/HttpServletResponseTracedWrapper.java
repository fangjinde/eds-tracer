package com.netease.edu.eds.trace.instrument.http;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.netease.edu.eds.trace.constants.PropagationConstants;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceContextPropagationUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/9/10
 **/

public class HttpServletResponseTracedWrapper extends HttpServletResponseWrapper {

    public HttpServletResponseTracedWrapper(HttpServletResponse response) {
        super(response);

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

    /**
     * 冲定向前
     */

    @Override
    public void sendRedirect(String location) throws IOException {

        Tracing tracing = Tracing.current();
        if (tracing != null) {

            Span span = tracing.tracer().nextSpan();
            span.kind(Span.Kind.CLIENT).name(location == null ? "null" : location);
            SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.HTTP_REDIRECT);
            SpanUtils.tagPropagationInfos(span);

            Environment environment = SpringBeanFactorySupport.getBean(Environment.class);
            if (environment != null) {
                SpanUtils.safeTag(span, "clientEnv", environment.getProperty("spring.profiles.active"));
            }

            span.start();
            try (Tracer.SpanInScope spanInScope = tracing.tracer().withSpanInScope(span)) {
                TraceContext.Injector<Map<String, String>> injector = tracing.propagation().injector(SETTER);
                Map<String, String> tracePropagationMap = new LinkedHashMap<>();
                injector.inject(span.context(), tracePropagationMap);
                String newLocation = addTracePropagationToLocation(location, tracePropagationMap);
                if (StringUtils.isNotBlank(newLocation)) {
                    location = newLocation;
                }

                super.sendRedirect(location);

            }

        } else {
            super.sendRedirect(location);
        }

    }

    private String addTracePropagationToLocation(String location, Map<String, String> tracePropagationMap) {
        String traceContextHexString = TraceContextPropagationUtils.generateTraceContextHexString(tracePropagationMap);
        if (StringUtils.isNotBlank(traceContextHexString)) {
            String newLocation = new UrlParameterManagerDto(location).addParamValuePairWithEncode(PropagationConstants.TRACE_CONTEXT_PROPAGATION_KEY,
                                                                                                  traceContextHexString).toUrl();
            if (StringUtils.isNotBlank(newLocation)) {
                return newLocation;

            }
        }

        return null;
    }

    public static void main(String[] args) {

        String[] testCases = { "http://study.163.com/course/introduction/782023.htm",
                               "http://study.163.com/course/introduction/782023.htm?UTM_MEDIUM=MEDIUM",
                               "http://study.163.com/course/introduction/782023.htm#/courseDetail",
                               "http://study.163.com/course/introduction/782023.htm?UTM_MEDIUM=MEDIUM#/courseDetail",
                               "http://study.163.com/course/introduction/782023.htm?param1=test1#/courseDetail",
                               "http://study.163.com/course/introduction/782023.htm?param1=test1&UTM_MEDIUM=MEDIUM#/courseDetail" };

        for (String testCase : testCases) {

            Assert.isTrue(testCase.equals(new UrlParameterManagerDto(testCase).toUrl()),
                          " before : " + testCase + " ,after : " + new UrlParameterManagerDto(testCase).toUrl());

        }

        System.out.println("all success");

        System.out.println("after add traceInfo=xxx");
        for (String testCase : testCases) {
            UrlParameterManagerDto urlParameterManagerDto = new UrlParameterManagerDto(testCase);
            urlParameterManagerDto.addParamValuePairWithEncode("traceInfo", null);
            System.out.println(urlParameterManagerDto.toUrl());

        }

    }

    public static class UrlParameterManagerDto {

        private Map<String, String> newOrderdMap() {
            return new LinkedHashMap<>();
        }

        private String              hashSection;
        private Map<String, String> encodedParams     = newOrderdMap();
        private String              uri;

        private static final String NULL_VALUE_STRING = "_HttpServletResponseTracedWrapper_UrlParameterManagerDto_NULL_VALUE_";

        public UrlParameterManagerDto(String originalUrl) {
            int hashSymbol = originalUrl.indexOf('#');

            StringBuilder newLocationSb = new StringBuilder();

            String noHashLocation = originalUrl;
            String hashRemainLocation = null;
            if (hashSymbol > -1) {
                noHashLocation = originalUrl.substring(0, hashSymbol);
                hashSection = originalUrl.substring(hashSymbol);
            }

            int paramSymbol = noHashLocation.indexOf('?');
            if (paramSymbol <= -1) {
                uri = noHashLocation;
            } else {
                uri = noHashLocation.substring(0, paramSymbol);
                String paramsString = noHashLocation.substring(paramSymbol + 1);
                if (StringUtils.isNotBlank(paramsString)) {
                    encodedParams = parseParamMapFromString(paramsString);
                }

            }
        }

        public UrlParameterManagerDto addParamValuePairWithEncode(String rawKey, String rawValue) {
            return addEncodedParamValuePair(getEncoded(rawKey), getEncoded(rawValue));
        }

        private String getEncoded(String rawString) {
            if (rawString != null) {
                try {
                    return URLEncoder.encode(rawString, "utf-8");
                } catch (UnsupportedEncodingException e) {

                }
            }
            return rawString;
        }

        public UrlParameterManagerDto addEncodedParamValuePair(String encodeKey, String encodedValue) {
            if (StringUtils.isBlank(encodeKey)) {
                return this;
            }
            if (StringUtils.isBlank(encodedValue)) {
                encodedParams.put(encodeKey, NULL_VALUE_STRING);
            } else {
                encodedParams.put(encodeKey, encodedValue);
            }
            return this;

        }

        public String toUrl() {
            if (uri == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder(uri);
            if (MapUtils.isNotEmpty(encodedParams)) {
                sb.append("?");
            }
            int index = 0;
            for (Map.Entry<String, String> entry : encodedParams.entrySet()) {
                if (index >= 1) {
                    sb.append("&");
                }
                sb.append(entry.getKey());
                if (!NULL_VALUE_STRING.equals(entry.getValue())) {
                    sb.append("=").append(entry.getValue());
                }
                index++;
            }

            if (StringUtils.isNotBlank(hashSection)) {
                sb.append(hashSection);
            }
            return sb.toString();

        }

        private Map<String, String> parseParamMapFromString(String paramsString) {

            Map<String, String> paramMap = newOrderdMap();

            if (StringUtils.isBlank(paramsString)) {
                return paramMap;
            }

            String[] paramsEntries = paramsString.split("&");
            for (String paramEntry : paramsEntries) {
                if (StringUtils.isBlank(paramEntry)) {
                    continue;
                }
                String[] paramKeyValuePairs = paramEntry.split("=", 2);
                if (paramKeyValuePairs == null) {
                    continue;
                }
                if (paramKeyValuePairs.length == 1) {
                    if (StringUtils.isNotBlank(paramKeyValuePairs[0])) {
                        paramMap.put(paramKeyValuePairs[0], NULL_VALUE_STRING);
                    }

                } else if (paramKeyValuePairs.length == 2) {
                    if (StringUtils.isNotBlank(paramKeyValuePairs[0])) {
                        if (StringUtils.isBlank(paramKeyValuePairs[1])) {
                            paramMap.put(paramKeyValuePairs[0], NULL_VALUE_STRING);
                        } else {
                            paramMap.put(paramKeyValuePairs[0], paramKeyValuePairs[1]);
                        }

                    }

                }

            }

            return paramMap;

        }
    }

}
