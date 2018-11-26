package com.netease.edu.eds.trace.instrument.http;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.core.UrlParameterManagerDto;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * @author hzfjd
 * @create 18/9/10
 **/

public class HttpServletResponseTracedWrapper extends HttpServletResponseWrapper {

    public HttpServletResponseTracedWrapper(HttpServletResponse response) {
        super(response);

    }

    /**
     * 冲定向前
     */

    @Override
    public void sendRedirect(String location) throws IOException {

        Tracing tracing = Tracing.current();
        if (tracing != null) {

            Span span = tracing.tracer().nextSpan();
            span.kind(Span.Kind.CLIENT).name("redirect:" + location);
            SpanUtils.safeTag(span, SpanType.HttpSubType.LOCATION, location);
            SpanUtils.safeTag(span, SpanType.HttpSubType.TAG_KEY, SpanType.HttpSubType.REDIRECT);
            SpanUtils.tagPropagationInfos(span);

            Environment environment = SpringBeanFactorySupport.getBean(Environment.class);
            if (environment != null) {
                SpanUtils.safeTag(span, "clientEnv", environment.getProperty("spring.profiles.active"));
            }

            span.start();
            try (Tracer.SpanInScope spanInScope = tracing.tracer().withSpanInScope(span)) {
                location = PropagationUtils.addTraceContextOnUrlIfNeeded(location);
                super.sendRedirect(location);

            } catch (Exception e) {
                SpanUtils.tagErrorMark(span);
                SpanUtils.tagError(span, e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new RuntimeException(e);
                }

            } finally {
                span.finish();
            }

        } else {
            super.sendRedirect(location);
        }

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

}
