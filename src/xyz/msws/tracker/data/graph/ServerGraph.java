package xyz.msws.tracker.data.graph;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
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
        XYChart chart = new XYChart(500, 500);
        chart.setTitle(data.getName() + " Graph");

        XYStyler styler = chart.getStyler();
        styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);

        long accuracy = TimeUnit.HOURS.toMillis(1), label = TimeUnit.HOURS.toMillis(12);
        long start = System.currentTimeMillis() - (accuracy * 24 * 30);
        String lastMap = null;
        Map<String, Map<Long, Integer>> values = new HashMap<>();
        Map<Object, Object> mappings = new HashMap<>();

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
            int units = (int) ((System.currentTimeMillis() - s) / accuracy);
            if ((s - start) % label == 0 || units == 0) {
                mappings.put(s, units == 0 ? "Present" : TimeParser.getDurationDescription((System.currentTimeMillis() - s) / 1000));
            } else {
                mappings.put(s, " ");
            }
            Map<Long, Integer> vs = values.getOrDefault(map, new LinkedHashMap<>());
            vs.put(s, players);
            values.put(map, vs);
        }
        chart.setCustomXAxisTickLabelsMap(mappings);

        for (Map.Entry<String, Map<Long, Integer>> entry : values.entrySet()) {
            List<Number> x, y;
            x = new ArrayList<>(entry.getValue().keySet());
            y = new ArrayList<>(entry.getValue().values());
            chart.addSeries(entry.getKey(), x, y);
        }


        File result = new File("output.png");
        try {
            BitmapEncoder.saveBitmapWithDPI(chart, "output", BitmapEncoder.BitmapFormat.PNG, 300);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
