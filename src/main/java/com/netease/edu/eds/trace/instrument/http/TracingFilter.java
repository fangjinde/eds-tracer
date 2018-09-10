package com.netease.edu.eds.trace.instrument.http;

import brave.Span;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Getter;
import brave.propagation.TraceContext;
import brave.servlet.HttpServletAdapter;
import com.netease.edu.eds.trace.constants.PropagationConstants;
import com.netease.edu.eds.trace.constants.SpanType;
import com.netease.edu.eds.trace.utils.PropagationUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;
import com.netease.edu.eds.trace.utils.TraceContextPropagationUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class TracingFilter implements Filter {

    static final Getter<HttpServletRequest, String> GETTER  = new Getter<HttpServletRequest, String>() {

                                                                @Override
                                                                public String get(HttpServletRequest carrier,
                                                                                  String key) {

                                                                    String traceContextHexString = carrier.getParameter(PropagationConstants.TRACE_CONTEXT_PROPAGATION_KEY);
                                                                    String value = TraceContextPropagationUtils.getTraceContextValue(traceContextHexString,
                                                                                                                                     key);
                                                                    if (StringUtils.isNotBlank(value)) {
                                                                        return value;
                                                                    }

                                                                    return carrier.getHeader(key);
                                                                }

                                                                @Override
                                                                public String toString() {
                                                                    return "HttpServletRequest::getParameter(PropagationConstants.TRACE_CONTEXT_PROPAGATION_KEY) , or fall back to HttpServletRequest::getHeader";
                                                                }
                                                            };
    static final HttpServletAdapter                 ADAPTER = new HttpServletAdapter();

    public static Filter create(Tracing tracing, SkipUriMatcher skipUriMatcher, WebDebugMatcher webDebugMatcher,
                                Environment environment) {
        return new TracingFilter(HttpTracing.create(tracing), skipUriMatcher, webDebugMatcher, environment);
    }

    public static Filter create(HttpTracing httpTracing, SkipUriMatcher skipUriMatcher, WebDebugMatcher webDebugMatcher,
                                Environment environment) {
        return new TracingFilter(httpTracing, skipUriMatcher, webDebugMatcher, environment);
    }

    private SkipUriMatcher                                           skipUriMatcher;
    final Tracer                                                     tracer;
    final HttpServerHandler<HttpServletRequest, HttpServletResponse> handler;
    final TraceContext.Extractor<HttpServletRequest>                 extractor;
    private Environment                                              environment;

    private final UrlPathHelper                                      urlPathHelper = new UrlPathHelper();

    TracingFilter(HttpTracing httpTracing, SkipUriMatcher skipUriMatcher, WebDebugMatcher webDebugMatcher,
                  Environment environment) {
        tracer = httpTracing.tracing().tracer();
        handler = HttpServerHandler.create(httpTracing, ADAPTER, webDebugMatcher);
        extractor = httpTracing.tracing().propagation().extractor(GETTER);
        this.skipUriMatcher = skipUriMatcher;
        this.environment = environment;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) (response);

        String uri = urlPathHelper.getPathWithinApplication(httpRequest);
        if (skipUriMatcher != null && skipUriMatcher.match(uri)) {
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        // 对http 302做增强
        httpResponse = new HttpServletResponseTracedWrapper(httpResponse);

        // Prevent duplicate spans for the same request
        if (request.getAttribute("TracingFilter") != null) {
            chain.doFilter(request, response);
            return;
        }

        request.setAttribute("TracingFilter", "true");

        Span span = handler.handleReceive(extractor, httpRequest);

        SpanUtils.safeTag(span, SpanType.TAG_KEY, SpanType.HTTP);

        if (span != null && !span.isNoop() && environment != null) {
            SpanUtils.safeTag(span, "env", environment.getProperty("spring.profiles.active"));
        }

        // Add attributes for explicit access to customization or span context
        request.setAttribute(SpanCustomizer.class.getName(), span);
        request.setAttribute(TraceContext.class.getName(), span.context());

        Throwable error = null;
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {

            // 一定要放在SpanInScope中，否则CurrentContext不正确。
            PropagationUtils.setOriginEnvIfNotExists(span.context(), environment.getProperty("spring.profiles.active"));
            SpanUtils.safeTag(span, "originEnv", PropagationUtils.getOriginEnv());
            // any downstream code can see Tracer.currentSpan() or use Tracer.currentSpanCustomizer()
            SpanUtils.tagPropagationInfos(span);
            chain.doFilter(httpRequest, httpResponse);
        } catch (IOException | ServletException | RuntimeException | Error e) {
            error = e;
            throw e;
        } finally {
            handler.handleSend(httpResponse, error, span);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }
}
