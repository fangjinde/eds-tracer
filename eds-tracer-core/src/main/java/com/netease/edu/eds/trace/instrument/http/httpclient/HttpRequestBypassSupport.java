package com.netease.edu.eds.trace.instrument.http.httpclient;

import com.netease.edu.eds.trace.instrument.http.ClientSkipUriMatcher;
import com.netease.edu.eds.trace.support.SpringBeanFactorySupport;
import com.netease.edu.eds.trace.support.TracePropertiesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author hzfjd
 * @create 19/2/14
 **/
public class HttpRequestBypassSupport {
	private static final Logger log = LoggerFactory
			.getLogger(HttpRequestBypassSupport.class);

	public static boolean byPassTrace(String uri) {
		try {
			URL url = new URL(uri);
			ClientSkipUriMatcher clientSkipUriMatcher = SpringBeanFactorySupport
					.getBean(ClientSkipUriMatcher.class);

			if (clientSkipUriMatcher != null) {
				return clientSkipUriMatcher.match(url.toString());
			}
			else {
				// fall back to check certain important ignores
				if (url.getPath().startsWith("/eureka/")) {
					return !TracePropertiesSupport.isHttpClientTracedForEureka();
				}
			}

		}
		catch (MalformedURLException e) {
			log.error("byPassTrace error for MalformedURLException, url= " + uri, e);
		}
		return false;
	}
}
