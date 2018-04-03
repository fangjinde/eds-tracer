package com.netease.edu.eds.trace.instrument.http;/**
 * Created by hzfjd on 18/4/3.
 */

import brave.internal.Nullable;
import com.netease.edu.web.config.EduWebProjectConfig;
import com.netease.edu.web.cookie.utils.CookieUtils;
import com.netease.edu.web.cookie.utils.NeteaseEduCookieManager;
import com.netease.edu.web.utils.WebUser;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/4/3
 */

@ConfigurationProperties(prefix = "edu.trace.debug")
public class DefaultWebDebugMatcher implements WebDebugMatcher {

    private Map<String, String> httpHeaders;
    private Map<String, String> httpParams;
    private String              httpMethod;//auxiliary
    private String              httpUri;
    private Integer             loginType;
    private String              loginId;//auxiliary
    private String debugMark = "debug";

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public Map<String, String> getHttpParams() {
        return httpParams;
    }

    public void setHttpParams(Map<String, String> httpParams) {
        this.httpParams = httpParams;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getHttpUri() {
        return httpUri;
    }

    public void setHttpUri(String httpUri) {
        this.httpUri = httpUri;
    }

    public Integer getLoginType() {
        return loginType;
    }

    public void setLoginType(Integer loginType) {
        this.loginType = loginType;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getDebugMark() {
        return debugMark;
    }

    public void setDebugMark(String debugMark) {
        this.debugMark = debugMark;
    }

    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    @Autowired
    ObjectProvider<EduWebProjectConfig> eduWebProjectConfigObjectProvider;

    @Override public boolean matches(HttpServletRequest request) {

        if (MapUtils.isEmpty(httpHeaders) && MapUtils.isEmpty(httpParams)
            && StringUtils.isBlank(httpUri) && StringUtils.isBlank(loginId)) {
            return false;
        }

        if (!mapMatches(request, HttpServletRequest::getHeader, httpHeaders)) {
            return false;
        }

        if (!mapMatches(request, HttpServletRequest::getParameter, httpParams)) {
            return false;
        }

        if (StringUtils.isNotBlank(httpUri)) {
            String uri = urlPathHelper.getPathWithinApplication(request);
            if (!httpUri.equals(uri)) {
                return false;
            }

        }

        if (StringUtils.isNotBlank(loginId)) {
            Map<String, Cookie> cookieMap = CookieUtils.readCookieToMap(request);
            boolean isMockUrs = true;
            EduWebProjectConfig eduWebProjectConfig = eduWebProjectConfigObjectProvider.getIfAvailable();
            if (eduWebProjectConfig != null) {
                isMockUrs = eduWebProjectConfig.isMockUrs();
            }
            WebUser webUser = NeteaseEduCookieManager.getWebUserFromStudyCookie(cookieMap,

                                                                                isMockUrs);
            if (webUser == null) {
                return false;
            }

            if (!loginId.equals(webUser.getLoginId())) {
                return false;
            }
            if (loginType != null && !loginType.equals(webUser.getLoginType())) {
                return false;
            }

        }

        return true;

    }

    interface Getter<C> {

        @Nullable String get(C carrier, String key);
    }

    private <C> boolean mapMatches(C carrier, Getter<C> getter, Map<String, String> criteria) {
        if (MapUtils.isNotEmpty(criteria)) {
            for (Map.Entry<String, String> entry : criteria.entrySet()) {
                if (!ObjectUtils.equals(getter.get(carrier, entry.getKey()), entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override public String debugMark() {
        return debugMark;
    }

}
