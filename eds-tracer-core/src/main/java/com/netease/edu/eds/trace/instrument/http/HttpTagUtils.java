package com.netease.edu.eds.trace.instrument.http;

import brave.Span;
import com.netease.edu.eds.trace.utils.SpanUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author hzfjd
 * @create 18/10/8
 **/
public class HttpTagUtils {
	public static void tagRequestParams(HttpServletRequest request, Span span) {
		if (request == null || span == null || span.isNoop()) {
			return;
		}
		Enumeration<String> paraNames = request.getParameterNames();
		while (paraNames.hasMoreElements()) {
			String paramName = paraNames.nextElement();
			String paramValue = request.getParameter(paramName);
			SpanUtils.safeTag(span, "p_" + paramName, paramValue);

		}
	}

	public static void tagRequestHeaders(HttpServletRequest request, Span span) {
		if (request == null || span == null || span.isNoop()) {
			return;
		}
		Enumeration<String> paraNames = request.getHeaderNames();
		while (paraNames.hasMoreElements()) {
			String paramName = paraNames.nextElement();
			String paramValue = request.getHeader(paramName);
			SpanUtils.safeTag(span, "h_" + paramName, paramValue);

		}
	}

}
