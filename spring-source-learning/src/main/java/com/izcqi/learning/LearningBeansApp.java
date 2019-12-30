package com.izcqi.learning;

import com.izcqi.learning.components.circularreferences.property.PropertyInjectionBeanA;
import com.izcqi.learning.config.BeanConfig;
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

	private static AnnotationConfigApplicationContext applicationContext = null;

	public static void main(String[] args) {
		applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.register(BeanConfig.class);
		applicationContext.refresh();

		circularRef();
	}

	private static void circularRef() {
		PropertyInjectionBeanA propertyInjectionBeanA = applicationContext.getBean(PropertyInjectionBeanA.class);
		propertyInjectionBeanA.sayHello();
	}
}
