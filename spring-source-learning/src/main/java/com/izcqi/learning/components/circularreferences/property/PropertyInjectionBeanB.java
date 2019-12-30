package com.izcqi.learning.components.circularreferences.property;

import com.izcqi.learning.components.circularreferences.ISay;
import com.izcqi.learning.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 16:50
 * @description：
 * @modified By：
 * @version:
 */
@Component
public class PropertyInjectionBeanB implements ISay  {

	@Autowired
	private PropertyInjectionBeanA propertyInjectionBeanA;

	public PropertyInjectionBeanB() {
		LogUtil.printObject("init BeanB");
	}

}
