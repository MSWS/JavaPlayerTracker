package xyz.msws.tracker.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.github.slugify.Slugify;

public class MSG {
	static Slugify slg = new Slugify();

	public static String simplify(String name) {
		return slg.slugify(name);
	}

	public static String toString(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
