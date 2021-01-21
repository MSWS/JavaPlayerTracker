package xyz.msws.tracker.utils;

import com.github.slugify.Slugify;

public class MSG {
	static Slugify slg = new Slugify();

	public static String simplify(String name) {
		return slg.slugify(name);
	}
}
