package com.netease.edu.eds.shuffle.dto;

/**
 * @author hzfjd
 * @create 18/12/20
 **/
public class PrefixOrSuffixInfoDto {

    private boolean exist = false;
    private String  prefix;
    private String  suffix;
    private String  separator;
    private String  env;

    public PrefixOrSuffixInfoDto(String env) {
        this.env = env;
    }

    public PrefixOrSuffixInfoDto(String env, String prefix, String suffix, String separator) {
        this.env = env;
        this.exist = true;
        this.prefix = prefix;
        this.suffix = suffix;
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

}
