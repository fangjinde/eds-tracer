package com.netease.edu.eds.trace.core;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/11/26
 **/
public  class UrlParameterManagerDto {

    private Map<String, String> newOrderdMap() {
        return new LinkedHashMap<>();
    }

    private String              hashSection;
    private Map<String, String> encodedParams     = newOrderdMap();
    private String              uri;

    private static final String NULL_VALUE_STRING = "_HttpServletResponseTracedWrapper_UrlParameterManagerDto_NULL_VALUE_";

    public UrlParameterManagerDto(String originalUrl) {
        int hashSymbol = originalUrl.indexOf('#');

        StringBuilder newLocationSb = new StringBuilder();

        String noHashLocation = originalUrl;
        String hashRemainLocation = null;
        if (hashSymbol > -1) {
            noHashLocation = originalUrl.substring(0, hashSymbol);
            hashSection = originalUrl.substring(hashSymbol);
        }

        int paramSymbol = noHashLocation.indexOf('?');
        if (paramSymbol <= -1) {
            uri = noHashLocation;
        } else {
            uri = noHashLocation.substring(0, paramSymbol);
            String paramsString = noHashLocation.substring(paramSymbol + 1);
            if (StringUtils.isNotBlank(paramsString)) {
                encodedParams = parseParamMapFromString(paramsString);
            }

        }
    }

    public UrlParameterManagerDto addParamValuePairWithEncode(String rawKey, String rawValue) {
        return addEncodedParamValuePair(getEncoded(rawKey), getEncoded(rawValue));
    }

    private String getEncoded(String rawString) {
        if (rawString != null) {
            try {
                return URLEncoder.encode(rawString, "utf-8");
            } catch (UnsupportedEncodingException e) {

            }
        }
        return rawString;
    }

    public UrlParameterManagerDto addEncodedParamValuePair(String encodeKey, String encodedValue) {
        if (StringUtils.isBlank(encodeKey)) {
            return this;
        }
        if (StringUtils.isBlank(encodedValue)) {
            encodedParams.put(encodeKey, NULL_VALUE_STRING);
        } else {
            encodedParams.put(encodeKey, encodedValue);
        }
        return this;

    }

    public String toUrl() {
        if (uri == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(uri);
        if (MapUtils.isNotEmpty(encodedParams)) {
            sb.append("?");
        }
        int index = 0;
        for (Map.Entry<String, String> entry : encodedParams.entrySet()) {
            if (index >= 1) {
                sb.append("&");
            }
            sb.append(entry.getKey());
            if (!NULL_VALUE_STRING.equals(entry.getValue())) {
                sb.append("=").append(entry.getValue());
            }
            index++;
        }

        if (StringUtils.isNotBlank(hashSection)) {
            sb.append(hashSection);
        }
        return sb.toString();

    }

    private Map<String, String> parseParamMapFromString(String paramsString) {

        Map<String, String> paramMap = newOrderdMap();

        if (StringUtils.isBlank(paramsString)) {
            return paramMap;
        }

        String[] paramsEntries = paramsString.split("&");
        for (String paramEntry : paramsEntries) {
            if (StringUtils.isBlank(paramEntry)) {
                continue;
            }
            String[] paramKeyValuePairs = paramEntry.split("=", 2);
            if (paramKeyValuePairs == null) {
                continue;
            }
            if (paramKeyValuePairs.length == 1) {
                if (StringUtils.isNotBlank(paramKeyValuePairs[0])) {
                    paramMap.put(paramKeyValuePairs[0], NULL_VALUE_STRING);
                }

            } else if (paramKeyValuePairs.length == 2) {
                if (StringUtils.isNotBlank(paramKeyValuePairs[0])) {
                    if (StringUtils.isBlank(paramKeyValuePairs[1])) {
                        paramMap.put(paramKeyValuePairs[0], NULL_VALUE_STRING);
                    } else {
                        paramMap.put(paramKeyValuePairs[0], paramKeyValuePairs[1]);
                    }

                }

            }

        }

        return paramMap;

    }
}
