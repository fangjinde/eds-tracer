package com.netease.edu.eds.shuffle.support;

import com.google.common.collect.Lists;
import com.netease.edu.eds.shuffle.core.EnvironmentShuffleUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hzfjd
 * @create 18/8/23
 **/
public class ShuffleEnvironmentInfoProcessUtils {

    private static List<String> environmentComponentNameSeparators = Lists.newArrayList(".", "-", "_");

    public static String getRawNameWithoutCurrentEnvironmentInfo(String originName) {
        String currentEnv = EnvironmentShuffleUtils.getCurrentEnv();
        return getRawNameWithoutCerturnValue(originName, currentEnv);
    }

    /**
     * 查看originName是否有包含内容未oldValue的前、后缀。如果没有则返回原字符串。如果有，则再看是否newValue有内容，如果有，则使用newValue做为新的前、后缀。否则去除oldValue的内容,同时还出去特定的一个分隔符。
     * 
     * @param originName
     * @param oldValue
     * @param newValue
     * @return
     */
    public static String getNameWithNewFixOrRemoveOldFix(String originName, String oldValue, String newValue) {
        if (StringUtils.isBlank(originName)) {
            return originName;
        }

        if (StringUtils.isBlank(oldValue)) {
            return originName;
        }

        if (oldValue.equals(newValue)) {
            return originName;
        }

        if (originName.startsWith(oldValue)) {
            originName = originName.substring(oldValue.length());
            if (StringUtils.isBlank(newValue)) {
                for (String separator : environmentComponentNameSeparators) {
                    if (originName.startsWith(separator)) {
                        originName = originName.substring(separator.length());
                        break;
                    }
                }
            } else {
                originName = newValue.trim() + originName;
            }

        }

        if (originName.endsWith(oldValue)) {
            originName = originName.substring(0, originName.length() - oldValue.length());
            if (StringUtils.isBlank(newValue)) {
                for (String separator : environmentComponentNameSeparators) {
                    if (originName.endsWith(separator)) {
                        originName = originName.substring(0, originName.length() - separator.length());
                        break;
                    }
                }
            } else {
                originName = originName + newValue.trim();
            }

        }

        return originName;
    }

    static String getRawNameWithoutCerturnValue(String originName, String value) {

        return getNameWithNewFixOrRemoveOldFix(originName, value, null);

        // if (StringUtils.isBlank(originName)) {
        // return originName;
        // }
        //
        // if (StringUtils.isBlank(value)) {
        // return originName;
        // }
        //
        // if (originName.startsWith(value)) {
        // originName = originName.substring(value.length());
        // for (String separator : environmentComponentNameSeparators) {
        // if (originName.startsWith(separator)) {
        // originName = originName.substring(separator.length());
        // break;
        // }
        // }
        // }
        //
        // if (originName.endsWith(value)) {
        // originName = originName.substring(0, originName.length() - value.length());
        // for (String separator : environmentComponentNameSeparators) {
        // if (originName.endsWith(separator)) {
        // originName = originName.substring(0, originName.length() - separator.length());
        // break;
        // }
        // }
        // }
        //
        // return originName;
    }

    public static void main(String[] args) {

        String rawName = "rawName";

        List<String> cases = new ArrayList<>();
        cases.add(rawName + ".hzfjd");
        cases.add(rawName + "hzfjd");
        cases.add(rawName + "_hzfjd");
        cases.add("hzfjd-" + rawName);
        cases.add("hzfjd" + rawName);

        for (String name : cases) {
            String extractedName = getRawNameWithoutCerturnValue(name, "hzfjd");
            System.out.println(extractedName);
            Assert.isTrue(rawName.equals(extractedName), "case of " + name + " is wrong");
        }

    }

}
