package com.izcqi.learning.components.circularreferences.setter;

import com.izcqi.learning.components.circularreferences.ISay;
import com.izcqi.learning.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:42
 * @description：
 * @modified By：
 * @version:
 */
@Component
public class SetterInjectionBeanB implements ISay {

	@Autowired
	private SetterInjectionBeanA setterInjectionBeanA;

	public void setSetterInjectionBeanA(SetterInjectionBeanA setterInjectionBeanA) {
		LogUtil.printObject("Injection=> setterInjectionBeanA");
		this.setterInjectionBeanA = setterInjectionBeanA;
	}

	public SetterInjectionBeanA getSetterInjectionBeanA() {
		return setterInjectionBeanA;
	}
}
