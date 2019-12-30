package com.izcqi.learning.components.circularreferences.setter;

import com.izcqi.learning.components.circularreferences.ISay;
import com.izcqi.learning.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:41
 * @description：
 * @modified By：
 * @version:
 */
@Component
public class SetterInjectionBeanA implements ISay {

	@Autowired
	private SetterInjectionBeanB setterInjectionBeanB;

	public void setSetterInjectionBeanB(SetterInjectionBeanB setterInjectionBeanB) {
		LogUtil.printObject("Injection=>  setterInjectionBeanB");
		this.setterInjectionBeanB = setterInjectionBeanB;
	}

	public SetterInjectionBeanB getSetterInjectionBeanB() {
		return setterInjectionBeanB;
	}
}
