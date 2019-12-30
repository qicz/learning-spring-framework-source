package com.izcqi.learning.base;

import com.izcqi.learning.config.BeanConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:51
 * @description：
 * @modified By：
 * @version:
 */
public class BaseTest {

	private AnnotationConfigApplicationContext applicationContext = null;
	
	public BaseTest() {
		applicationContext = new AnnotationConfigApplicationContext();

		applicationContext.register(BeanConfig.class);
		applicationContext.refresh();
	}

	public AnnotationConfigApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public <T> T getBean(Class<T> requiredType) {
		return this.applicationContext.getBean(requiredType);
	}
}
