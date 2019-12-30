package com.izcqi.learning.components.circularreferences.property;

import com.izcqi.learning.components.circularreferences.ISay;
import com.izcqi.learning.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 16:49
 * @description：
 * @modified By：
 * @version:
 */
@Component
public class PropertyInjectionBeanA implements ISay {

	@Autowired
	private PropertyInjectionBeanB propertyInjectionBeanB;

	public PropertyInjectionBeanA() {
		LogUtil.printObject("init BeanA");
	}
}
