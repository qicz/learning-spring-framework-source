package com.izcqi.learning.beans;

import com.izcqi.learning.beans.components.BeanComponent;
import com.izcqi.learning.beans.config.BeanConfig;
import com.izcqi.learning.utils.LogUtil;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 13:21
 * @description：
 * @modified By：
 * @version:
 */
public class LearningBeansApp {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.register(BeanConfig.class);
		applicationContext.refresh();

		BeanComponent beanComponent = applicationContext.getBean(BeanComponent.class);

		LogUtil.printObject(beanComponent);
	}
}
