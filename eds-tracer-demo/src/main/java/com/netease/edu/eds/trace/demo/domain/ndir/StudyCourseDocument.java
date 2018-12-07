package com.netease.edu.eds.trace.demo.domain.ndir;

import com.netease.edu.persist.search.annotation.SearchDocumentMetadata;

import java.io.Serializable;

/**
 * Created by liujinquan on 2016/11/23.
 */
@SearchDocumentMetadata(documentName = "StudyCourse2_bk")
public class StudyCourseDocument implements Serializable{

    private String id;

    private String front_category_ids;

    private String activity_ids;

    private String mix_cid;

    private Integer mode;

    private Integer type;

    private Long term_id;

    private Double price;

    private Long start_time;

    private Long end_time;

    private Integer priviledge;

    private String course_name;

//    @SearchFieldConfig(highlight=true)
    private String course_name_suff_sepa;

    private Long publish_time;

    private Double uniform_combo_score;

    private Double mob_uniform_combo_score;

    private Integer tag_iap;

    private String sub_course_name;

    private String category_name;

    private String tag;

    private String teachers;

    private String author_suff_sepa;

    private String provider;

    private String provider_suff_sepa;

    private String description;

    private String lesson_names;

    private String chapter_names;

    private Integer web_visible;

    private Integer ios_visible;

    private Float favourable_sort_score;

    private Float selective_score;

    private Float revenue_score;

    private Integer display_type;

    private String recommend_tag;

    private Integer machine_grade;

    private Double selling_rank_score;

    private Long provider_id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFront_category_ids() {
        return front_category_ids;
    }

    public void setFront_category_ids(String front_category_ids) {
        this.front_category_ids = front_category_ids;
    }

    public String getMix_cid() {
        return mix_cid;
    }

    public void setMix_cid(String mix_cid) {
        this.mix_cid = mix_cid;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getTerm_id() {
        return term_id;
    }

    public void setTerm_id(Long term_id) {
        this.term_id = term_id;
    }

    public Long getStart_time() {
        return start_time;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public Long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Long end_time) {
        this.end_time = end_time;
    }

    public Integer getPriviledge() {
        return priviledge;
    }

    public void setPriviledge(Integer priviledge) {
        this.priviledge = priviledge;
    }

    public String getCourse_name() {
        return course_name;
    }

    public void setCourse_name(String course_name) {
        this.course_name = course_name;
    }

    public String getCourse_name_suff_sepa() {
        return course_name_suff_sepa;
    }

    public void setCourse_name_suff_sepa(String course_name_suff_sepa) {
        this.course_name_suff_sepa = course_name_suff_sepa;
    }

    public Long getPublish_time() {
        return publish_time;
    }

    public void setPublish_time(Long publish_time) {
        this.publish_time = publish_time;
    }


    public Integer getTag_iap() {
        return tag_iap;
    }

    public void setTag_iap(Integer tag_iap) {
        this.tag_iap = tag_iap;
    }

    public String getSub_course_name() {
        return sub_course_name;
    }

    public void setSub_course_name(String sub_course_name) {
        this.sub_course_name = sub_course_name;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTeachers() {
        return teachers;
    }

    public void setTeachers(String teachers) {
        this.teachers = teachers;
    }

    public String getAuthor_suff_sepa() {
        return author_suff_sepa;
    }

    public void setAuthor_suff_sepa(String author_suff_sepa) {
        this.author_suff_sepa = author_suff_sepa;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProvider_suff_sepa() {
        return provider_suff_sepa;
    }

    public void setProvider_suff_sepa(String provider_suff_sepa) {
        this.provider_suff_sepa = provider_suff_sepa;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLesson_names() {
        return lesson_names;
    }

    public void setLesson_names(String lesson_names) {
        this.lesson_names = lesson_names;
    }

    public String getChapter_names() {
        return chapter_names;
    }

    public void setChapter_names(String chapter_names) {
        this.chapter_names = chapter_names;
    }

    public Integer getWeb_visible() {
        return web_visible;
    }

    public void setWeb_visible(Integer web_visible) {
        this.web_visible = web_visible;
    }

    public Integer getIos_visible() {
        return ios_visible;
    }

    public void setIos_visible(Integer ios_visible) {
        this.ios_visible = ios_visible;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getUniform_combo_score() {
        return uniform_combo_score;
    }

    public void setUniform_combo_score(Double uniform_combo_score) {
        this.uniform_combo_score = uniform_combo_score;
    }

    public Double getMob_uniform_combo_score() {
        return mob_uniform_combo_score;
    }

    public void setMob_uniform_combo_score(Double mob_uniform_combo_score) {
        this.mob_uniform_combo_score = mob_uniform_combo_score;
    }

    public Float getFavourable_sort_score() {
        return favourable_sort_score;
    }

    public void setFavourable_sort_score(Float favourable_sort_score) {
        this.favourable_sort_score = favourable_sort_score;
    }

    public Float getSelective_score() {
        return selective_score;
    }

    public void setSelective_score(Float selective_score) {
        this.selective_score = selective_score;
    }

    public Float getRevenue_score() {
        return revenue_score;
    }

    public void setRevenue_score(Float revenue_score) {
        this.revenue_score = revenue_score;
    }

    public Integer getDisplay_type() {
        return display_type;
    }

    public void setDisplay_type(Integer display_type) {
        this.display_type = display_type;
    }

    public String getRecommend_tag() {
        return recommend_tag;
    }

    public void setRecommend_tag(String recommend_tag) {
        this.recommend_tag = recommend_tag;
    }

    public String getActivity_ids() {
        return activity_ids;
    }

    public void setActivity_ids(String activity_ids) {
        this.activity_ids = activity_ids;
    }

    public Integer getMachine_grade() {
        return machine_grade;
    }

    public void setMachine_grade(Integer machine_grade) {
        this.machine_grade = machine_grade;
    }

    public Double getSelling_rank_score() {
        return selling_rank_score;
    }

    public void setSelling_rank_score(Double selling_rank_score) {
        this.selling_rank_score = selling_rank_score;
    }

    public Long getProvider_id() {
        return provider_id;
    }

    public void setProvider_id(Long provider_id) {
        this.provider_id = provider_id;
    }
}
