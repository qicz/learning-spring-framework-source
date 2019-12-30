package com.izcqi.learning.beans.circularreferences;

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
public class BeanA {

	@Autowired
	private BeanB beanB;

	public BeanA() {
		LogUtil.printObject("init BeanA");
	}

	public void sayHello() {
		LogUtil.printObject("BeanA Say Hello");
	}

}
