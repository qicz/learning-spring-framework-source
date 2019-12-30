package com.izcqi.learning.beans;

import com.izcqi.learning.beans.circularreferences.BeanA;
import com.izcqi.learning.beans.components.BeanComponent;
import com.izcqi.learning.beans.config.BeanConfig;
import com.izcqi.learning.utils.LogUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 13:21
 * @description：
 * @modified By：
 * @version:
 */
public class LearningBeansApp {

	private static AnnotationConfigApplicationContext applicationContext = null;

	public static void main(String[] args) {
		applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.register(BeanConfig.class);
		applicationContext.refresh();

		circularRef();
	}

	private static void circularRef() {
		BeanA beanA = applicationContext.getBean(BeanA.class);
		beanA.sayHello();
	}
}
