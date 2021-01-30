package xyz.msws.tracker.data.graph;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.XYStyler;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.TimeParser;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GlobalGraph extends Graph {

    public GlobalGraph(PlayerTrackerModule tracker) {
        super(tracker);
    }

    @Override
    public File generate() {
        XYChart chart = new XYChart(1200, 800, new TrackerTheme());
        XYStyler styler = chart.getStyler();

        chart.setTitle("Players on all servers");

        long accuracy = TimeUnit.HOURS.toMillis(1), label = TimeUnit.HOURS.toMillis(12);
        long start = System.currentTimeMillis() - (accuracy * 24 * 30);
        chart.setXAxisTitle("Time Passed");
        chart.setYAxisTitle("Players");
        styler.setXAxisTitleColor(new Color(114, 137, 218));
        styler.setYAxisTitleColor(new Color(0, 191, 255));
        styler.setXAxisTickMarksColor(styler.getXAxisTitleColor().darker());
        styler.setYAxisTickMarksColor(styler.getYAxisTitleColor().darker());
        styler.setChartFontColor(Color.LIGHT_GRAY);
        styler.setPlotContentSize(1.0);
        styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);

        Map<Object, Object> mappings = new HashMap<>();
        List<String> sort = tracker.getServerNames().stream().sorted().collect(Collectors.toList());

        for (String server : sort) {
            List<Long> x = new ArrayList<>();
            List<Integer> y = new ArrayList<>();

            for (long s = start; s < System.currentTimeMillis(); s += accuracy) {
                int players = 0;
                for (ServerPlayer player : tracker.getPlayers()) {
                    long time = player.getPlaytimeDuring(s, s + accuracy, server);
                    if (time == 0)
                        continue;
                    players++;
                }
                if (players == 0)
                    continue;
                int units = (int) ((System.currentTimeMillis() - s) / accuracy);
                x.add(s);
                if ((s - start) % label == 0 || units == 0) {
                    mappings.put(s, units == 0 ? "Present" : TimeParser.getDurationDescription((System.currentTimeMillis() - s) / 1000));
                } else {
                    mappings.put(s, " ");
                }
                y.add(players);
            }
            Series series = chart.addSeries(server, x, y);
        }

        chart.setCustomXAxisTickLabelsMap(mappings);
        File result = new File("output.png");
        try {
            BitmapEncoder.saveBitmapWithDPI(chart, "output", BitmapEncoder.BitmapFormat.PNG, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
