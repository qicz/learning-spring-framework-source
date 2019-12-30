package com.izcqi.learning.components.circularreferences;

import com.izcqi.learning.utils.LogUtil;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 21:44
 * @description：
 * @modified By：
 * @version:
 */
public interface ISay {

	default void sayHello() {
		LogUtil.printObject(this.getClass().getSimpleName() + " Say Hello");
	}

}
