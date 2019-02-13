/*
 * Copyright 2013-2018 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.netease.edu.eds.trace.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.sleuth.ErrorParser;

import com.netease.edu.eds.trace.constants.CommonTagKeys;
import com.netease.edu.eds.trace.utils.ExceptionStringUtils;
import com.netease.edu.eds.trace.utils.SpanUtils;

import brave.SpanCustomizer;

/**
 * {@link ErrorParser} that sets the error tag for an exportable span.
 *
 * @author Marcin Grzejszczak
 * @since 1.2.1
 */
public class EduExceptionMessageErrorParser implements ErrorParser {

    private static final Log log = LogFactory.getLog(EduExceptionMessageErrorParser.class);

    @Override
    public void parseErrorTags(SpanCustomizer span, Throwable error) {
        if (span != null && error != null) {
            SpanUtils.safeTag(span, CommonTagKeys.ERROR, ExceptionStringUtils.getStackTraceString(error));
        }
    }

}
