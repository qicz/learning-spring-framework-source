package com.izcqi.learning.cicularreferences;

import com.izcqi.learning.base.BaseTest;
import com.izcqi.learning.components.circularreferences.property.PropertyInjectionBeanA;
import com.izcqi.learning.components.circularreferences.setter.SetterInjection;
import com.izcqi.learning.components.circularreferences.setter.SetterInjectionBeanA;
import com.izcqi.learning.components.circularreferences.setter.SetterInjectionBeanB;
import com.izcqi.learning.utils.LogUtil;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:50
 * @description：
 * @modified By：
 * @version:
 */
public class CircularReferenceTest extends BaseTest {

	@Test
	public void testPropertyInjectionCircularRef() {
		PropertyInjectionBeanA propertyInjectionBeanA = this.getBean(PropertyInjectionBeanA.class);
		propertyInjectionBeanA.sayHello();
	}

	@Test
	public void testSetterInjectionCircularRef() {
		SetterInjectionBeanA setterInjectionBeanA = this.getBean(SetterInjectionBeanA.class);
		setterInjectionBeanA.sayHello();

		LogUtil.printObject(setterInjectionBeanA);

		LogUtil.printObject(setterInjectionBeanA.getSetterInjectionBeanB());


		SetterInjectionBeanB setterInjectionBeanB1 = this.getBean(SetterInjectionBeanB.class);
		setterInjectionBeanB1.sayHello();
		LogUtil.printObject(setterInjectionBeanB1);
		LogUtil.printObject(setterInjectionBeanB1.getSetterInjectionBeanA());
	}


	@Test
	public void setterInjection() {
		SetterInjection setterInjection = this.getBean(SetterInjection.class);
		System.out.println(setterInjection.getData());
	}

}
