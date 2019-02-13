package com.netease.edu.eds.trace.instrument.http.client.reactornetty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.NettyOutbound;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.channel.data.FileChunkedStrategy;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.websocket.WebsocketOutbound;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The `org.springframework.cloud.gateway.filter.NettyRoutingFilter` in SC Gateway is
 * adding only these headers that were set when the request came in. That means that
 * adding any additional headers (via instrumentation) is completely ignored. That's why
 * we're wrapping the `HttpClientRequest` in such a wrapper that when `setHeaders` is
 * called (that clears any current headers), will also add the tracing headers * @author
 * hzfjd * @create 19/2/12
 */

public class TracedHttpClientRequest implements HttpClientRequest {
	private HttpClientRequest delegate;
	private final io.netty.handler.codec.http.HttpHeaders addedHeaders;

	TracedHttpClientRequest(HttpClientRequest delegate, HttpHeaders addedHeaders) {
		this.delegate = delegate;
		this.addedHeaders = addedHeaders;
	}

	@Override
	public HttpClientRequest addCookie(Cookie cookie) {
		this.delegate = this.delegate.addCookie(cookie);
		return this;
	}

	@Override
	public HttpClientRequest addHeader(CharSequence name, CharSequence value) {
		this.delegate = this.delegate.addHeader(name, value);
		return this;
	}

	@Override
	public HttpClientRequest context(Consumer<NettyContext> contextCallback) {
		this.delegate = this.delegate.context(contextCallback);
		return this;
	}

	@Override
	public HttpClientRequest chunkedTransfer(boolean chunked) {
		this.delegate = this.delegate.chunkedTransfer(chunked);
		return this;
	}

	@Override
	public HttpClientRequest options(
			Consumer<? super NettyPipeline.SendOptions> configurator) {
		this.delegate = this.delegate.options(configurator);
		return this;
	}

	@Override
	public HttpClientRequest followRedirect() {
		this.delegate = this.delegate.followRedirect();
		return this;
	}

	@Override
	public HttpClientRequest failOnClientError(boolean shouldFail) {
		this.delegate = this.delegate.failOnClientError(shouldFail);
		return this;
	}

	@Override
	public HttpClientRequest failOnServerError(boolean shouldFail) {
		this.delegate = this.delegate.failOnServerError(shouldFail);
		return this;
	}

	@Override
	public boolean hasSentHeaders() {
		return this.delegate.hasSentHeaders();
	}

	@Override
	public HttpClientRequest header(CharSequence name, CharSequence value) {
		this.delegate = this.delegate.header(name, value);
		return this;
	}

	@Override
	public HttpClientRequest headers(HttpHeaders headers) {
		HttpHeaders copy = headers.copy();
		copy.add(this.addedHeaders);
		this.delegate = this.delegate.headers(copy);
		return this;
	}

	@Override
	public boolean isFollowRedirect() {
		return this.delegate.isFollowRedirect();
	}

	@Override
	public HttpClientRequest keepAlive(boolean keepAlive) {
		this.delegate = this.delegate.keepAlive(keepAlive);
		return this;
	}

	@Override
	public HttpClientRequest onWriteIdle(long idleTimeout, Runnable onWriteIdle) {
		this.delegate = this.delegate.onWriteIdle(idleTimeout, onWriteIdle);
		return this;
	}

	@Override
	public String[] redirectedFrom() {
		return this.delegate.redirectedFrom();
	}

	@Override
	public HttpHeaders requestHeaders() {
		return this.delegate.requestHeaders();
	}

	@Override
	public Mono<Void> send() {
		return this.delegate.send();
	}

	@Override
	public Flux<Long> sendForm(Consumer<Form> formCallback) {
		return this.delegate.sendForm(formCallback);
	}

	@Override
	public NettyOutbound sendHeaders() {
		return this.delegate.sendHeaders();
	}

	@Override
	public WebsocketOutbound sendWebsocket() {
		return this.delegate.sendWebsocket();
	}

	@Override
	public WebsocketOutbound sendWebsocket(String subprotocols) {
		return this.delegate.sendWebsocket(subprotocols);
	}

	@Override
	public ByteBufAllocator alloc() {
		return this.delegate.alloc();
	}

	@Override
	public NettyContext context() {
		return this.delegate.context();
	}

	@Override
	public FileChunkedStrategy getFileChunkedStrategy() {
		return this.delegate.getFileChunkedStrategy();
	}

	@Override
	public Mono<Void> neverComplete() {
		return this.delegate.neverComplete();
	}

	@Override
	public NettyOutbound send(Publisher<? extends ByteBuf> dataStream) {
		return this.delegate.send(dataStream);
	}

	@Override
	public NettyOutbound sendByteArray(Publisher<? extends byte[]> dataStream) {
		return this.delegate.sendByteArray(dataStream);
	}

	@Override
	public NettyOutbound sendFile(Path file) {
		return this.delegate.sendFile(file);
	}

	@Override
	public NettyOutbound sendFile(Path file, long position, long count) {
		return this.delegate.sendFile(file, position, count);
	}

	@Override
	public NettyOutbound sendFileChunked(Path file, long position, long count) {
		return this.delegate.sendFileChunked(file, position, count);
	}

	@Override
	public NettyOutbound sendGroups(
			Publisher<? extends Publisher<? extends ByteBuf>> dataStreams) {
		return this.delegate.sendGroups(dataStreams);
	}

	@Override
	public NettyOutbound sendObject(Publisher<?> dataStream) {
		return this.delegate.sendObject(dataStream);
	}

	@Override
	public NettyOutbound sendObject(Object msg) {
		return this.delegate.sendObject(msg);
	}

	@Override
	public NettyOutbound sendString(Publisher<? extends String> dataStream) {
		return this.delegate.sendString(dataStream);
	}

	@Override
	public NettyOutbound sendString(Publisher<? extends String> dataStream,
			Charset charset) {
		return this.delegate.sendString(dataStream, charset);
	}

	@Override
	public void subscribe(Subscriber<? super Void> s) {
		this.delegate.subscribe(s);
	}

	@Override
	public Mono<Void> then() {
		return this.delegate.then();
	}

	@Override
	public NettyOutbound then(Publisher<Void> other) {
		return this.delegate.then(other);
	}

	@Override
	public Map<CharSequence, Set<Cookie>> cookies() {
		return this.delegate.cookies();
	}

	@Override
	public boolean isKeepAlive() {
		return this.delegate.isKeepAlive();
	}

	@Override
	public boolean isWebsocket() {
		return this.delegate.isWebsocket();
	}

	@Override
	public HttpMethod method() {
		return this.delegate.method();
	}

	@Override
	public String path() {
		return this.delegate.path();
	}

	@Override
	public String uri() {
		return this.delegate.uri();
	}

	@Override
	public HttpVersion version() {
		return this.delegate.version();
	}
}
