package websitemonitoring;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class ChartManager {
    // Luu lai phan hoi theo website
    private static final Map<String, XYSeries> seriesMap = new HashMap<>();

    // Them du lieu vao bieu do
    public static void updateChart(String url, long responseMs) {
        XYSeries series = seriesMap.computeIfAbsent(url, k -> new XYSeries(k));
        series.add(series.getItemCount(), responseMs);
    }

    // Hien thi bieu do
    public static void showChart(String url) {
        XYSeries series = seriesMap.get(url);
        if (series == null) return;

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Thời gian phản hồi", "Lần kiểm tra", "ms", dataset);

        ChartPanel chartPanel = new ChartPanel(chart);
        JDialog dialog = new JDialog();
        dialog.setTitle("Biểu đồ - " + url);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(null);
        dialog.add(chartPanel);
        dialog.setVisible(true);
    }
}
