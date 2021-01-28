package xyz.msws.tracker.data.graph;

import org.knowm.xchart.BoxChart;
import org.knowm.xchart.BoxChartBuilder;
import org.knowm.xchart.CategoryChart;
import xyz.msws.tracker.data.ServerData;
import xyz.msws.tracker.data.ServerPlayer;
import xyz.msws.tracker.module.PlayerTrackerModule;

import java.io.File;
import java.util.*;

public class ServerGraph extends Graph {

    private ServerData data;

    public ServerGraph(PlayerTrackerModule tracker, ServerData data) {
        super(tracker);
        this.data = data;
    }

    @Override
    public File generate() {
        CategoryChart chart = new CategoryChart(500, 500);

        Map<ServerPlayer, Long> rankings = new HashMap<>();
        tracker.getPlayers().forEach(e -> rankings.put(e, e.getTotalPlaytime(data.getName())));
        List<Map.Entry<ServerPlayer, Long>> sorted = new ArrayList<>(rankings.entrySet());
        sorted.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return null;
    }
}
