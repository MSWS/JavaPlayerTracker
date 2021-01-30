package xyz.msws.tracker.data.graph;

import org.knowm.xchart.style.MatlabTheme;
import org.knowm.xchart.style.Styler;

import java.awt.*;

public class TrackerTheme extends MatlabTheme {
    private static final Color discord = new Color(54, 57, 63);

    @Override
    public Color getChartBackgroundColor() {
        return discord;
    }

    @Override
    public Color getChartFontColor() {
        return Color.WHITE;
    }

    @Override
    public Color getLegendBackgroundColor() {
        return Color.DARK_GRAY;
    }

    @Override
    public Color getPlotBackgroundColor() {
        return discord.darker();
    }

    @Override
    public Styler.LegendPosition getLegendPosition() {
        return Styler.LegendPosition.InsideNE;
    }
}
