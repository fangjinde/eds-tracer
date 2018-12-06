package com.netease.edu.eds.trace.instrument.dubbo;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;

import java.util.Map;

/**
 * @see DubboTraceFilter
 * Tracing filter for dubbo consumer,returns cs and cr.
 *
 * @author Will Tong
 */
@Deprecated
@Activate(group = Constants.CONSUMER, order = -8990)
public class DubboConsumerTraceFilter implements Filter {
  private Tracer tracer;
  private DubboClientHandler handler;
  private TraceContext.Injector<Map<String, String>> injector;
  private TraceContext.Extractor<Map<String, String>> extractor;

  public void setDubboTracing(DubboTracing dubboTracing) {
    tracer = dubboTracing.tracing().tracer();
    handler = DubboClientHandler.create(dubboTracing, new DubboClientAdapter());
    injector = dubboTracing.tracing().propagation().injector(Map::put);
    extractor = dubboTracing.tracing().propagation().extractor(Map::get);
  }

  @Override
  public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
    Span span = handler.handleSend(extractor, injector);
    Result rpcResult = null;
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
      return rpcResult = invoker.invoke(invocation);
    } catch (RuntimeException | Error e) {
      throw e;
    } finally {
      handler.handleReceive(rpcResult, span);
    }
  }
}
