package com.izcqi.learning.utils;

import java.io.PrintStream;

/**
 * @author ：Qicz
 * @project ：spring
 * @date ：Created in 2019/12/30 14:15
 * @description：主要用于测试日志在console的输出
 * @modified By：
 * @version:
 */
public class LogUtil {

	private static final String spacer = "==============================";

	enum Level {
		INFO,
		ERROR
	}

	private static Level activeLevel = Level.INFO;

	private LogUtil(){}

	private static void resetLevel() {
		LogUtil.infoLevel();
	}

	private static void errorLevel() {
		LogUtil.activeLevel = Level.ERROR;
	}

	private static void infoLevel() {
		LogUtil.activeLevel = Level.INFO;
	}

	private static void print(String string) {
		StringBuilder out = new StringBuilder();
		out.append(LogUtil.spacer);
		out.append(string);
		out.append(LogUtil.spacer);
		PrintStream ps = null;
		if (LogUtil.activeLevel.equals(Level.ERROR)) {
			ps = System.err;
		} else {
			ps = System.out;
		}
		ps.println(out);
	}

	private static void print(Object object) {
		LogUtil.printStart();
		System.out.println(object);
		LogUtil.printEnd();
	}

	private static void printStart() {
		LogUtil.print("START");
	}

	private static void printEnd() {
		LogUtil.print(" END ");
	}

	public static void printObject(Object object) {
		LogUtil.print(object);
	}

	public static void printError(Object object) {
		LogUtil.errorLevel();
		LogUtil.print(object);
		LogUtil.resetLevel();
	}
}
