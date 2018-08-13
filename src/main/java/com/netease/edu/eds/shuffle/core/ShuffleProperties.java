package com.netease.edu.eds.shuffle.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author hzfjd
 * @create 18/7/20
 **/
@ConfigurationProperties(prefix = "shuffle")
public class ShuffleProperties {

    private boolean            turnOn;
    private String             standardEnvName         = STANDARD_ENV_NAME;
    /**
     * wait 150ms to send message to latter environment to reduce cross environment consumer compete as less as
     * possible.
     */
    private int                delayMSToSendLatter     = DELAY_MS_TO_SEND_LATTER;
    public static final int    DELAY_MS_TO_SEND_LATTER = 150;
    public static final String STANDARD_ENV_NAME       = "std";

    public boolean isTurnOn() {
        return turnOn;
    }

    public void setTurnOn(boolean turnOn) {
        this.turnOn = turnOn;
    }

    public String getStandardEnvName() {
        return standardEnvName;
    }

    public void setStandardEnvName(String standardEnvName) {
        this.standardEnvName = standardEnvName;
    }

    public int getDelayMSToSendLatter() {
        return delayMSToSendLatter;
    }

    public void setDelayMSToSendLatter(int delayMSToSendLatter) {
        this.delayMSToSendLatter = delayMSToSendLatter;
    }
}
