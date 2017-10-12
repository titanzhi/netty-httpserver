package com.s3d.httpserver.util;

public class ServerUtil {
	/** 是否是Linux操作系统 */
	private static boolean isLinuxPlatform = false;

	static {
		String osName = System.getProperty("os.name");
		if (osName != null && osName.toLowerCase().indexOf("linux") >= 0) {
			isLinuxPlatform = true;
		}
	}

	public static boolean isLinuxPlatform() {
		return isLinuxPlatform;
	}
}
