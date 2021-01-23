package xyz.msws.tracker.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Logger {
	private static List<String> logs = new ArrayList<>();
	String pattern;
	private final static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy h:m:s.S");

	public static void log(String message) {
		logs.add(format.format(System.currentTimeMillis()) + " " + message);
		System.out.println(message);
	}

	public static void logf(String msg, Object... format) {
		log(String.format(msg, format));
	}

	public static List<String> getLogs() {
		return logs;
	}

}
