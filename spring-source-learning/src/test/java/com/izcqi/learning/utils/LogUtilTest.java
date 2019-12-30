package com.izcqi.learning.utils;

import org.junit.jupiter.api.Test;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 14:22
 * @description：LogUtil测试
 * @modified By：
 * @version:
 */
public class LogUtilTest {

	private class A {
		private String propertyA;
		private String propertyB;

		public String getPropertyA() {
			return propertyA;
		}

		public void setPropertyA(String propertyA) {
			this.propertyA = propertyA;
		}

		public String getPropertyB() {
			return propertyB;
		}

		public void setPropertyB(String propertyB) {
			this.propertyB = propertyB;
		}
	}

	private A a = new A();

	@Test
	public void printInfoLog() {
		LogUtil.printObject(a);
	}

	@Test
	public void printErrorLog() {
		LogUtil.printError(a);
	}
}
