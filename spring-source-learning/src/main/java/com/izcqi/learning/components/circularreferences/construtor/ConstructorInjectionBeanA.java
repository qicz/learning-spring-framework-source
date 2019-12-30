package com.izcqi.learning.components.circularreferences.construtor;

import com.izcqi.learning.components.circularreferences.ISay;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:41
 * @description：
 * @modified By：
 * @version:
 */
//@Component
public class ConstructorInjectionBeanA implements ISay {

	private final ConstructorInjectionBeanB constructorInjectionBeanB;

	public ConstructorInjectionBeanA(ConstructorInjectionBeanB constructorInjectionBeanB) {
		this.constructorInjectionBeanB = constructorInjectionBeanB;
	}
}
