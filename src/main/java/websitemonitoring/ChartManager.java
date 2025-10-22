package websitemonitoring;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartManager {
    private static final Map<String, XYSeries> seriesMap = new HashMap<>();

    public static void updateChart(String url, long responseMs) {
        XYSeries series = seriesMap.computeIfAbsent(url, k -> new XYSeries(k));
        if (series.getItemCount() > 100) {
            series.remove(0);
        }
        series.add(series.getItemCount(), responseMs);
    }

    public static JFreeChart getCombinedChart(List<String> urls) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (String url : urls) {
            XYSeries series = seriesMap.get(url);
            if (series != null && series.getItemCount() > 0) {
                dataset.addSeries(series);
            }
        }

        return ChartFactory.createXYLineChart(
                "Biểu đồ phản hồi tổng hợp",
                "Lần kiểm tra", "ms", dataset);
    }
}
