package com.netease.edu.boot.ccyctrl.annotation;

import com.netease.edu.boot.ccyctrl.CcyCtrlAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by hzfjd on 17/12/5.
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Configuration
@Import({CcyCtrlAutoConfiguration.class})
public @interface EnableCcyCtrl {

}
