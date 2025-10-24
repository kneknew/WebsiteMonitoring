package websitemonitoring;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartManager {
    private static final Map<String, XYSeries> seriesMap = new HashMap<>();

    public static void updateChart(String url, long responseMs) {
        XYSeries series = seriesMap.computeIfAbsent(url, k -> new XYSeries(k));
        if (series.getItemCount() > 200) {
            series.remove(0);
        }
        series.add(series.getItemCount(), responseMs);
    }

    public static void clearAll() {
        seriesMap.clear();
    }

    
    public static JFreeChart getCombinedChart(List<String> urls, boolean darkMode) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (String url : urls) {
            XYSeries series = seriesMap.get(url);
            if (series != null && series.getItemCount() > 0) {
                dataset.addSeries(series);
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Summary Response Chart",
                "The Test",
                "ms",
                dataset
        );

        Color[] lightSeriesColors = new Color[]{
                new Color(33, 150, 243),
                new Color(76, 175, 80),
                new Color(255, 87, 34),
                new Color(156, 39, 176),
                new Color(255, 193, 7),
                new Color(3, 169, 244),
                new Color(244, 67, 54),
                new Color(0, 150, 136)
        };

        Color[] darkSeriesColors = new Color[]{
                new Color(79, 195, 247),
                new Color(129, 199, 132),
                new Color(255, 167, 38),
                new Color(186, 104, 200),
                new Color(255, 214, 79),
                new Color(128, 216, 255),
                new Color(239, 83, 80),
                new Color(38, 166, 154)
        };

        Color bgChart = darkMode ? new Color(0x2F3438) : Color.WHITE;
        Color bgPlot = darkMode ? new Color(0x353A3E) : new Color(0xF5F7FA);
        Color axisColor = darkMode ? Color.decode("#E6EEF3") : Color.decode("#212121");
        Color gridColor = darkMode ? new Color(0x37474F) : new Color(0xE0E0E0);
        Color titleColor = darkMode ? new Color(0xB0BEC5) : new Color(0x263238);
        Color legendText = axisColor;

        chart.setBackgroundPaint(bgChart);
        if (chart.getTitle() != null) chart.getTitle().setPaint(titleColor);

        Plot p = chart.getPlot();
        if (p instanceof XYPlot) {
            XYPlot plot = (XYPlot) p;
            plot.setBackgroundPaint(bgPlot);
            plot.setDomainGridlinePaint(gridColor);
            plot.setRangeGridlinePaint(gridColor);

            if (plot.getDomainAxis() != null) {
                plot.getDomainAxis().setLabelPaint(axisColor);
                plot.getDomainAxis().setTickLabelPaint(axisColor);
            }
            if (plot.getRangeAxis() != null) {
                plot.getRangeAxis().setLabelPaint(axisColor);
                plot.getRangeAxis().setTickLabelPaint(axisColor);
            }

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
            Color[] palette = darkMode ? darkSeriesColors : lightSeriesColors;
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, palette[i % palette.length]);
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
                renderer.setSeriesShapesVisible(i, false);
            }
            plot.setRenderer(renderer);
        }

        LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setItemPaint(legendText);
            legend.setBackgroundPaint(bgPlot);
            legend.setPosition(RectangleEdge.BOTTOM);
            legend.setFrame(new BlockBorder(bgPlot));
        }

        return chart;
    }
}
