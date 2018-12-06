/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.edu.eds.trace.instrument.async;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;

import java.util.concurrent.Executor;

/**
 * {@link AsyncConfigurerSupport} that creates a tracing data passing version
 * of the {@link Executor}
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class EduLazyTraceAsyncCustomizer extends AsyncConfigurerSupport {

	private final BeanFactory beanFactory;
	private final AsyncConfigurer delegate;

	public EduLazyTraceAsyncCustomizer(BeanFactory beanFactory, AsyncConfigurer delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	@Override
	public Executor getAsyncExecutor() {
		if (this.delegate.getAsyncExecutor() instanceof EduLazyTraceExecutor) {
			return this.delegate.getAsyncExecutor();
		}
		return new EduLazyTraceExecutor(this.beanFactory, this.delegate.getAsyncExecutor());
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return this.delegate.getAsyncUncaughtExceptionHandler();
	}

}
