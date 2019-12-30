package com.izcqi.learning.beans.circularreferences;

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
public class BeanB {

	@Autowired
	private BeanA beanA;

	public BeanB() {
		LogUtil.printObject("init BeanB");
	}

	public void sayHello() {
		LogUtil.printObject("BeanB Say Hello");
	}
}
