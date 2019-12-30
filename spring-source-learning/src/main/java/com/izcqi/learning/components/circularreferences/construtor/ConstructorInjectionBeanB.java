package com.izcqi.learning.components.circularreferences.construtor;

import com.izcqi.learning.components.circularreferences.ISay;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:42
 * @description：
 * @modified By：
 * @version:
 */
//@Component
public class ConstructorInjectionBeanB implements ISay {

	private final ConstructorInjectionBeanA constructorInjectionBeanA;

	public ConstructorInjectionBeanB(ConstructorInjectionBeanA constructorInjectionBeanA) {
		this.constructorInjectionBeanA = constructorInjectionBeanA;
	}
}
