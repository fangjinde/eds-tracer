package com.netease.edu.eds.trace.instrument.http;

import java.net.URL;

public interface RedirectUrlTraceMatcher {

    boolean needTrace(URL url);
}
