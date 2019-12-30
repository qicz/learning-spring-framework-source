package com.izcqi.learning.components.circularreferences.setter;

import com.izcqi.learning.utils.LogUtil;
import org.springframework.stereotype.Component;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 22:29
 * @description：
 * @modified By：
 * @version:
 */
@Component
public class SetterInjection {

	private Data data;

	public void setData(Data data) {
		LogUtil.printObject("SetterInjection  setter");
		this.data = data;
	}

	public Data getData() {
		return data;
	}
}
