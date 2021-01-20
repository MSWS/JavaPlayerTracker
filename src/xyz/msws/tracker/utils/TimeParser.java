package xyz.msws.tracker.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.text.WordUtils;

public class TimeParser {

	public static long getPunishmentDate(String date) {
		SimpleDateFormat format = new SimpleDateFormat("MM-dd-yy kk:mm");
		try {
			return format.parse(date).toInstant().toEpochMilli();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 0;
	}

	public static long getPunishmentDuration(String duration) {
		long d = 0;
		try {
			for (String s : duration.split(",")) {
				s = s.trim();
				int v = Integer.parseInt(s.split(" ")[0]);
				TimeUnit unit = TimeUnit.fromString(s.split(" ")[1]);
				d += v * unit.getSeconds();
			}
		} catch (NumberFormatException e) {
			return d;
		}

		return d;
	}

	public static String getDateDescription(long epoch) {
		SimpleDateFormat format = new SimpleDateFormat("MM-dd-yy kk:mm");
		return format.format(new Date(epoch));
	}

	public static String getDurationDescription(long seconds) {
		return getDurationDescription(seconds, 2);
	}

	public static String getDurationDescription(long seconds, int precision) {
		for (int i = 0; i < TimeUnit.values().length - 1; i++) {
			TimeUnit c = TimeUnit.values()[i], n = TimeUnit.values()[i + 1];
			if (c.getSeconds() <= seconds && n.getSeconds() > seconds) {
				if ((double) seconds / (double) c.getSeconds() == 1)
					return WordUtils.capitalizeFully(
							String.format("1 %s", c.toString().substring(0, c.toString().length() - 1)));
				if ((double) seconds % (double) c.getSeconds() == 0)
					return WordUtils.capitalizeFully(String.format("%d %s", seconds / c.getSeconds(), c.toString()));
				return WordUtils.capitalizeFully(
						String.format("%." + precision + "f %s", seconds / (double) c.getSeconds(), c.toString()));
			}

		}
		return WordUtils.capitalizeFully(String.format("%." + precision + "f %s",
				seconds / (double) TimeUnit.YEARS.getSeconds(), TimeUnit.YEARS.toString()));
	}

	public enum TimeUnit {
		SECONDS(1, "s"), MINUTES(60, "m"), HOURS(60 * 60, "h"), DAYS(60 * 60 * 24, "d"), WEEKS(60 * 60 * 24 * 7, "wk"),
		MONTHS(60 * 60 * 24 * 7 * 4, "mo"), YEARS(60 * 60 * 24 * 7 * 4 * 12, "y");

		long seconds;
		String id;

		TimeUnit(long seconds, String id) {
			this.seconds = seconds;
			this.id = id;
		}

		public long getSeconds() {
			return seconds;
		}

		public String getId() {
			return id;
		}

		public static TimeUnit fromString(String s) {
			for (TimeUnit u : TimeUnit.values())
				if (u.getId().equalsIgnoreCase(s))
					return u;
			for (TimeUnit u : TimeUnit.values())
				if (s.toLowerCase().startsWith(u.getId()))
					return u;
			return SECONDS;
		}
	}
}
