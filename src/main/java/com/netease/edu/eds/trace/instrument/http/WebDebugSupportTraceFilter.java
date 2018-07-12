package com.netease.edu.eds.trace.instrument.http;/**
 * Created by hzfjd on 18/4/2.
 */

import brave.Span;
import brave.SpanCustomizer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @see TracingFilter
 * @author hzfjd
 * @create 18/4/2
 */
@Deprecated
public class WebDebugSupportTraceFilter implements Filter {

    private WebDebugMatcher webDebugMatcher;

    public WebDebugSupportTraceFilter(WebDebugMatcher webDebugMatcher) {
        this.webDebugMatcher = webDebugMatcher;
    }

    @Override public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        Span span = (Span) httpServletRequest.getAttribute(SpanCustomizer.class.getName());
        if (span == null) {
            chain.doFilter(request, response);
            return;
        }

        if (span.isNoop()) {
            if (webDebugMatcher.matches(httpServletRequest)) {

            }
        }

    }

    @Override public void destroy() {

    }
}
