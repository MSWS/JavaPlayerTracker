package xyz.msws.tracker.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    private static final List<String> logs = new ArrayList<>();
    private final static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy h:m:s.S");
    String pattern;

    public static void log(String message) {
        logs.add(format.format(System.currentTimeMillis()) + " " + message);
        System.out.println(message);
    }

    public static void logf(String msg, Object... format) {
        log(String.format(msg, format));
    }

    public static void log(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String str = sw.toString();
        for (String s : str.split("\n"))
            log(s);
    }

    public static List<String> getLogs() {
        return logs;
    }

}
