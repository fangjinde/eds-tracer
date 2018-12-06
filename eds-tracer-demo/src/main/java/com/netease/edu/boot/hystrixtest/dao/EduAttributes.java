package com.netease.edu.boot.hystrixtest.dao;

import com.netease.framework.dao.sql.annotation.DataProperty;

public class EduAttributes {
    public static final String  KEY_RECOMMEND_COURSES                             = "re_co";
    public static final String  KEY_INDEX_RECOMMEND_COURSES                       = "index_co";
    public static final String  KEY_USER_CENTER_RECOMMEND_COURSES                 = "user_center_co";
    public static final String  KEY_USER_CENTER_RECOMMEND_STUDYPLANS              = "uc_splan";
    public static final String  KEY_ANNOUNCEMENT                                  = "announcement";
    public static final String  KEY_SIDEBAR_POST                                  = "sidebar_post";
    public static final String  KEY_RECOMMEND_PROVIDER                            = "re_provider";
    public static final String  KEY_INDEX_RECOMMEND_PROVIDER                      = "idx_re_provider";
    public static final String  VALUE_SPLIT                                       = ";";

    public static final String  KEY_COMBO_SORT_PARAMS                             = "COMBO_SORT_PARAMS";
    public static final String  KEY_BAMEI_LEYU_DIANPING                           = "bamei:leyu_dianping";
    public static final String  KEY_BAMEI_MEIRIXIXIN_OWNERID                      = "bamei:meirixixing";
    public static final String  KEY_BAMEI_ZUIJURENQI_OWNERID                      = "bamei:zuijurenqi";
    public static final String  KEY_YIXIN_RANDOM_COURSE                           = "yixin:random_course";
    public static final String  KEY_YIXIN_FIXED_COURSE                            = "yixin:fixed_course";

    public static final String  KEY_ACTIVITY_TIME                                 = "KEY_ACTIVITY_TIME";
    public static final String  KEY_ACTIVITY_SCHOOL_CET                           = "KEY_ACTIVITY_SCHOOL_CET_";

    /**
     * 易信线上的access_token的json值
     */
    public static final String  KEY_YIXIN_ACCESSTOKEN                             = "yixin:access_token";
    /**
     * 易信测试环境的access_token的json值
     */
    public static final String  KEY_YIXIN_TEST_ACCESSTOKEN                        = "yixin:test_access_token";

    /**
     * 微信线上access_token的json值
     */
    public static final String  KEY_WEIXIN_ACCESSTOKEN                            = "weixin:access_token";

    /**
     * 微信测试环境access_token的json值
     */
    public static final String  KEY_WEIXIN_TEST_ACCESSTOKEN                       = "weixin:test_access_token";
    /**
     * 易信答题题目总数
     */
    public static final String  KEY_YIXIN_TOTAL_QUESTION_NUM                      = "yixin:total_q_num";
    public static final String  KEY_YNOTE_BANLIST                                 = "QR_YNOTE_BANLIST";

    public static final String  KEY_MOBILE_SETTING_IPHONE                         = "_iphone";
    public static final String  KEY_MOBILE_SETTING_IPAD                           = "_ipad";
    public static final String  KEY_MOBILE_SETTING_ANDROID                        = "_android";
    public static final String  KEY_MOBILE_SETTING_ANDROID_PAD                    = "_apad";

    /**
     * 当前支持的最低版本，低于low必须升级；
     */
    public static final String  KEY_MOBILEVERSION_LOW                             = "mobile_version_low";
    /**
     * 当前支持主流版本，低于high高于low 提醒升级；
     */
    public static final String  KEY_MOBILEVERSION_HIGH                            = "mobile_version_high";
    /**
     * 当前最新版本
     */
    public static final String  KEY_MOBILEVERSION_NEWEST                          = "mobile_version_new";
    /**
     * 当前最新版本
     */
    public static final String  KEY_MOBILEVERSION_DOWNLOAD_URL                    = "mobile_version_dowload_url";
    /**
     * 提醒文案
     */
    public static final String  KEY_MOBILEVERSION_DEPICT                          = "mobile_newest_depict";
    public static final String  KEY_MOBILEVERSION_VIDEOSUPPORT                    = "mobile_videosupport";
    public static final String  KEY_MOBILEVERSION_MAILSWITCH                      = "mobile_mailswitch";
    public static final String  KEY_MOBILEVERSION_REVIEWING_VERSION               = "mobile_reviewing_v";
    public static final String  KEY_REPO_DISPLAY_REGION                           = "repo_display_region";

    public static final String  KEY_YIXIN_LOTTERY_CHANCE_CONDITON                 = "YIXIN_LOTTERY_CHANCE_CONDITON";

    /**
     * 云阅读
     */
    public static final String  KEY_YYD_COURSE_IDS                                = "YYD_COURSE_IDS";
    // 云课堂热门精选推荐
    public static final String  KEY_BIGPHOTO_RECOMMEND_HOT                        = "bigphoto_recommend_hot";
    public static final String  KEY_OTHER_RECOMMEND_HOT                           = "other_recommend_hot";
    // 云课堂新品精选推荐
    public static final String  KEY_BIGPHOTO_RECOMMEND_NEWEST                     = "bigphoto_recommend_newest";
    public static final String  KEY_OTHER_RECOMMEND_NEWEST                        = "other_recommend_newest";
    // 畅销课程推荐
    public static final String  KEY_BESTSELL_COURSE_RECOMMEND                     = "bestsell_course_recommend";

    // 如果是默认活动的话，则是CET 或者加后缀
    public static final String  KEY_ACTIVITY_CET                                  = "ACTIVITY_CET";
    public static final String  KEY_ACTIVITY_CET_RECOMMEND_COURSES                = "activity_cet_recommend_courses";

    public static final String  KEY_FEE_COURSE_ACTIVITY_END_TIME                  = "fee_course_activity_end_time";

    public static final String  KEY_SOLD_OUT_COURSES                              = "sold_out_courses";

    public static final String  KEY_FEE_COURSE_ACTIVITY_TOTAL_COUNT               = "fee_course_activity_total_count";

    // 云課堂課程折扣活動
    public static final String  KEY_DISCOUNT_COURSE_ID                            = "key_discount_course_id";

    // 我的云课堂广告位
    public static final String  KEY_PERSONAL_AD                                   = "key_personal_ad";
    // 首页专题广告位
    public static final String  KEY_INDEX_AD                                      = "key_index_ad";

    // 机构讲师首页推荐列表
    public static final String  KEY_HOMEPAGE_LECTORORG_RECOMMEND                  = "homepage_lectororg_recommend";
    // 结构讲师全部课程页推荐列表
    public static final String  KEY_COURSEPAGE_LECTORORG_RECOMMEND                = "coursepage_lectororg_recommend";

    public static final String  STOP_COURSELIST                                   = "stop_courselist";
    public static final String  STOP_LOGIN_STUDY2ICOURSE                          = "stop_login_study2icourse";
    public static final String  STOP_LOGIN_STUDY2MOOC                             = "stop_login_study2mooc";
    public static final String  STOP_RECOMMEND_FEECOURSE                          = "stop_recommend_feecourse";

    // 移动端接口测试服务器ip
    public static final String  ZD_MOBILE_UNIT_TEST_HOST                          = "zd_mobile_unit_test_host";

    public static final String  KEY_TRY_WEB_SERVICE                               = "KEY_TRY_WEB_SERVICE";

    public static final String  KEY_LIVE_ONLINE_NUMBER_LIMIT                      = "KEY_LIVE_ONLINE_NUMBER_LIMIT";

    // 年中课程促销活动
    public static final String  KEY_MIDYEAR_PROMOT_ITINTERNET                     = "midyear_promot_itinternet";
    public static final String  KEY_MIDYEAR_PROMOT_LANGUAGESTUDY                  = "midyear_promot_languagestudy";
    public static final String  KEY_MIDYEAR_PROMOT_OFFICEMASTER                   = "midyear_promot_officemaster";
    public static final String  KEY_MIDYEAR_PROMOT_TALENTSKILL                    = "midyear_promot_talentskill";
    public static final String  KEY_MIDYEAR_PROMOT_MORE                           = "midyear_promot_more";
    public static final String  KEY_MIDYEAR_PROMOT_GROUPBUY                       = "midyear_promot_groupbuy";
    // 年中促销活动优惠券数量
    public static final String  KEY_MIDYEAR_PROMOT_COUPON_COUNT                   = "midyear_promot_coupon_count";
    // q3促销活动优惠券数量
    public static final String  KEY_Q3_PROMOT_COUPON_COUNT                        = "q3_promot_coupon_count";
    // 首页促销课程
    public static final String  KEY_MIDYEAR_PROMOT_HOMEPAGE                       = "midyear_promot_homepage";
    // 年中促销活动每日一课的相关信息
    public static final String  KEY_MIDYEAR_PROMOT_DAY_COURSE                     = "midyear_promot_day_course";

    // 云课堂邮件发送黑名单key
    public static final String  KEY_EMAIL_SEND_BLACK_LIST                         = "email_send_black_list";

    // 云课堂邮件发送黑名单key
    public static final String  KEY_360_CATEGORY_MAPPING                          = "cps_360_category_mapping";

    // 云课堂推荐学校id
    public static final String  KEY_RECOMMEND_SCHOOLIDS                           = "recommend_schoolids";

    public static final String  EDU_VISIT_COUNT_CHECK_DEFENDER_POLICY_MANAGER_KEY = "E_V_C_C_D_P_M_K";

    public static final String  LOGON_VISIT_POLICY                                = "LOGON_VISIT_POLICY";

    // 平均每周使用次数、使用天数、使用次数、下次提示间隔天数
    public static final String  KEY_PROMPT_COUNT                                  = "mobile_prompt_count";

    // 金云奖
    public static final String  ACTIVITY_GIFT_COURSE_JINYUN                       = "activity_gift_course_jinyun";

    // 是否忽略预发环境创建订单的服务器回调，平时关，预发需要验证和线上不一样回调时打开
    public static final String  KEY_PRE_ORDER_BACK_NOTIFY_IGNORE_SWITCH           = "pre_order_back_notify_ignore_switch";

    // 收费、免费课程过滤开关
    public static final Integer MOBILE_RECOMMEND_FEECOURSE_SWITCHOFF              = 0;
    public static final Integer MOBILE_RECOMMEND_FEECOURSE_SWITCHON               = 1;

    //IPHONG非审核状态下，发现课程过滤开关
    public static final String  KEY_MOB_FINDCOURSE_FILTER = "mob_findCourse_filter";

    //IPHONG下打开课程详情页是否打开适配页
    public static final String  KEY_MOB_COURSEPAGE_REDIRECT = "mob_coursePage_rediect";

    //IPHONE非审核状态下，我的课程过滤开关
    public static final String  KEY_MOB_MYCOURSE_FILTER   = "mob_myCourse_filter";

    //IPHONE非审核状态下:发现课程过滤开关取值：1-IAP+免费课程；2-免费课程；3-IAP（走WEB收费）+免费课程
    public static final Integer MOB_IPHONE_OUTREVIEW_FINDCOURSE_FREE_WITH_IAP     = 1;
    public static final Integer MOB_IPHONE_OUTREVIEW_FINDCOURSE_FREE              = 2;
    public static final Integer MOB_IPHONE_OUTREVIEW_FINDCOURSE_FREE_WITH_WEB_IAP = 3;

    //IPHONE非审核状态下:我的课程过滤开关取值：1-IAP+免费课程；2-所有课程（不过滤）
    public static final Integer MOB_IPHONE_OUTREVIEW_MYCOURSE_FREE_WITH_IAP       = 1;
    public static final Integer MOB_IPHONE_OUTREVIEW_MYCOURSE_ALL                 = 2;
    
    private Long                id;
    private String              key;
    private String              value;
    private Integer             p;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @DataProperty(column = "k")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @DataProperty(column = "v")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getP() {
        return p;
    }

    public void setP(Integer p) {
        this.p = p;
    }
}
