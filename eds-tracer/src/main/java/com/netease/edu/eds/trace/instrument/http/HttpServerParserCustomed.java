package com.netease.edu.eds.trace.instrument.http;/**
 * Created by hzfjd on 18/4/3.
 */

import brave.http.HttpAdapter;
import brave.http.HttpServerParser;

/**
 * @author hzfjd
 * @create 18/4/3
 */
public class HttpServerParserCustomed extends HttpServerParser {

    @Override protected <Req> String spanName(HttpAdapter<Req, ?> adapter, Req req) {
        return adapter.method(req) + " " + adapter.path(req);
    }
}
