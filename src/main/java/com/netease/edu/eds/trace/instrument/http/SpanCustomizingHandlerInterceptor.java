package com.netease.edu.eds.trace.instrument.http;

import brave.SpanCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Adds application-tier data to an existing http span via {@link HandlerParser}.
 * This also sets the request property "http.route" so that it can be used in naming the http span.
 *
 * <p>Use this instead of {@link brave.spring.webmvc.TracingHandlerInterceptor} when you start traces at the servlet
 * level via {@link brave.servlet.TracingFilter}.
 */
public final class SpanCustomizingHandlerInterceptor implements HandlerInterceptor {
  /** Redefined from HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE added in Spring 3. */
  static final String BEST_MATCHING_PATTERN_ATTRIBUTE =
      "org.springframework.web.servlet.HandlerMapping.bestMatchingPattern";

  @Autowired(required = false)
  HandlerParser handlerParser = new HandlerParser();

  public SpanCustomizingHandlerInterceptor() { // hide the ctor so we can change later if needed
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
    SpanCustomizer span = (SpanCustomizer) request.getAttribute(SpanCustomizer.class.getName());
    if (span != null) {
      setHttpRouteAttribute(request);
      handlerParser.preHandle(request, o, span);
    }
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
  }

  @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
  }

  /**
   * Sets the "http.route" attribute from {@link #BEST_MATCHING_PATTERN_ATTRIBUTE} so that the
   * {@link brave.servlet.TracingFilter} can read it.
   */
  static void setHttpRouteAttribute(HttpServletRequest request) {
    Object httpRoute = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE);
    request.setAttribute("http.route", httpRoute != null ? httpRoute.toString() : "");
  }
}
