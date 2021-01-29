package xyz.msws.tracker.data.graph;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.Styler;
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

public class GlobalGraph extends Graph {

    public GlobalGraph(PlayerTrackerModule tracker) {
        super(tracker);
    }

    @Override
    public File generate() {
        XYChart chart = new XYChart(500, 400, Styler.ChartTheme.Matlab);
        Styler styler = chart.getStyler();
        styler.setChartBackgroundColor(new Color(54, 57, 63));
        styler.setChartFontColor(Color.WHITE);
        styler.setLegendBackgroundColor(Color.DARK_GRAY);
        styler.setPlotBackgroundColor(Color.BLACK);
        styler.setLegendPosition(Styler.LegendPosition.InsideNE);
        styler.setXAxisTitleColor(Color.GREEN.darker());
        styler.setYAxisTitleColor(Color.BLUE.brighter());

        chart.setTitle("Players on all servers");

        long size = 1000 * 60 * 60;
        long start = System.currentTimeMillis() - (size * 24 * 30);
        chart.setXAxisTitle(TimeParser.getDurationDescription(size / 1000).split(" ")[1] + "s Ago");
        chart.setYAxisTitle("Players");

        Map<Object, Object> mappings = new HashMap<>();

        for (String server : tracker.getServerNames()) {

            List<Long> x = new ArrayList<>();
            List<Integer> y = new ArrayList<>();

            for (long s = start; s < System.currentTimeMillis(); s += size) {
                int players = 0;
                for (ServerPlayer player : tracker.getPlayers()) {
                    long time = player.getPlaytimeDuring(s, s + size, server);
                    if (time == 0)
                        continue;
                    players++;
                }
                if (players == 0)
                    continue;
                int units = (int) ((System.currentTimeMillis() - s) / size);
                x.add((long) -units);
                mappings.put(-units, units == 0 ? "Present" : units);
                y.add(players);
            }
            chart.addSeries(server, x, y);
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
