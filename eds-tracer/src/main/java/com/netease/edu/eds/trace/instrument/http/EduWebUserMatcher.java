package com.netease.edu.eds.trace.instrument.http;/**
 * Created by hzfjd on 18/4/3.
 */

import com.netease.edu.web.config.EduWebProjectConfig;
import com.netease.edu.web.cookie.utils.CookieUtils;
import com.netease.edu.web.cookie.utils.NeteaseEduCookieManager;
import com.netease.edu.web.utils.WebUser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author hzfjd
 * @create 18/4/3
 */
public class EduWebUserMatcher implements WebUserMatcher {

    @Autowired
    ObjectProvider<EduWebProjectConfig> eduWebProjectConfigObjectProvider;

    @Override public boolean matches(HttpServletRequest request, String loginIdCriteria, Integer loginTypeCriteria) {
        if (StringUtils.isNotBlank(loginIdCriteria)) {
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

            if (!loginIdCriteria.equals(webUser.getLoginId())) {
                return false;
            }
            if (loginTypeCriteria != null && !loginTypeCriteria.equals(webUser.getLoginType())) {
                return false;
            }

        }
        return true;
    }
}
