package com.netease.edu.eds.trace.constants;

/**
 * @author hzfjd
 * @create 18/7/18
 **/
public interface PropagationConstants {

    String ORIGIN_ENV                    = "traceoriginenv";
    String TRACE_CONTEXT_PROPAGATION_KEY = "_trace_c_p_k2_";
    String TRACE_ID_HTTP_RESPONSE_HEADER_NAME="_trace_id_hk_";
    Object NULL_OBJECT                   = new Object();
}
