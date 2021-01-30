package xyz.msws.tracker.data.graph;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;
import xyz.msws.tracker.utils.TimeParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ServerGraph extends Graph {

    private final ServerData data;

    public ServerGraph(PlayerTrackerModule tracker, ServerData data) {
        super(tracker);
        this.data = data;
    }

    @Override
    public File generate() {
        XYChart chart = new XYChart(2000, 800, new TrackerTheme());
        chart.setTitle(data.getName() + " Graph");
        chart.setXAxisTitle("Time Passed");
        chart.setYAxisTitle("Players");

        XYStyler styler = chart.getStyler();
        styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        styler.setLegendPosition(Styler.LegendPosition.OutsideE);
        styler.setPlotContentSize(1.0);

        long accuracy = TimeUnit.MINUTES.toMillis(10), label = TimeUnit.HOURS.toMillis(2);
        long start = System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(1));
        Map<String, Map<Long, Integer>> values = new HashMap<>();
        Map<Object, Object> mappings = new HashMap<>();
        String lastMap = null;
        for (long s = start; s < System.currentTimeMillis(); s += accuracy) {
            int players = 0;
            for (ServerPlayer player : tracker.getPlayers()) {
                long time = player.getPlaytimeDuring(s, s + accuracy, data.getName());
                if (time == 0)
                    continue;
                players++;
            }
            if (players == 0)
                continue;
            String map = data.getMap(s);
            if (map == null)
                map = "Unknown";
            int units = (int) ((System.currentTimeMillis() - s) / accuracy);
            if ((s - start) % label == 0 || units == 0) {
                mappings.put(s, units == 0 ? "Present" : TimeParser.getDurationDescription((System.currentTimeMillis() - s) / 1000));
            } else {
                mappings.put(s, " ");
            }

            Map<Long, Integer> vs = values.getOrDefault(map, new LinkedHashMap<>());

            if (!map.equals(lastMap) && lastMap != null) {
                Map<Long, Integer> tmp = values.getOrDefault(lastMap, new LinkedHashMap<>());
                tmp.put(s - 1, players);
                tmp.put(s, 0);
                values.put(lastMap, tmp);

                tmp = values.getOrDefault(map, new LinkedHashMap<>());
                tmp.put(s - 1, 0);
                values.put(map, tmp);
            }

            vs.put(s, players);
            values.put(map, vs);
            lastMap = map;
        }
        chart.setCustomXAxisTickLabelsMap(mappings);

        for (Map.Entry<String, Map<Long, Integer>> entry : values.entrySet()) {
            List<Number> x, y;
            x = new ArrayList<>(entry.getValue().keySet());
            y = new ArrayList<>(entry.getValue().values());
            if (entry.getKey().trim().isEmpty())
                continue;
            chart.addSeries(entry.getKey(), x, y);
        }


        File result = new File("output.png");
        try {
//            BitmapEncoder.saveBitmapWithDPI(chart, "output", BitmapEncoder.BitmapFormat.PNG, 72);
            BitmapEncoder.saveBitmap(chart, "output", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
