package websitemonitoring;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class UiMain {
    private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");

    private static ScheduledExecutorService executor;
    private static final List<ScheduledFuture<?>> tasks = new ArrayList<>();
    private static final Map<String, Boolean> statusMap = new ConcurrentHashMap<>();

    public static void createAndShowGui() {
        JFrame frame = new JFrame("Website Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 750);
        frame.setLocationRelativeTo(null);

        // Initial palettes (light by default)
        Palette light = new Palette(
                Color.decode("#FFFFFF"), // background
                Color.decode("#F5F7FA"), // panel
                Color.decode("#1976D2"), // primary (buttons/accents)
                Color.decode("#43A047"), // start
                Color.decode("#E53935"), // stop
                Color.decode("#455A64"), // button neutral
                Color.decode("#212121")  // text
        );
        Palette dark = new Palette(
                Color.decode("#263238"), // background
                Color.decode("#2E3B40"), // panel
                Color.decode("#29B6F6"), // primary (accent)
                Color.decode("#66BB6A"), // start
                Color.decode("#EF5350"), // stop
                Color.decode("#546E7A"), // button neutral
                Color.decode("#ECEFF1")  // text
        );

        Palette current = light;

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setBackground(current.background);

        JLabel title = new JLabel("WEBSITE MONITORING", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(current.primary);
        main.add(title, BorderLayout.NORTH);

        // Center layout similar to previous simpler layout:
        // Left: URL list + controls. Center: history. South of center: chart.
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(current.panel);

        // URL list
        DefaultListModel<String> urlListModel = new DefaultListModel<>();
        JList<String> urlList = new JList<>(urlListModel);
        urlList.setBorder(BorderFactory.createTitledBorder("Danh sách website"));
        urlList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urlList.setBackground(current.panel);
        urlList.setForeground(current.text);
        urlList.setFixedCellHeight(26);
        urlList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String url = value.toString();
                Boolean online = statusMap.get(url);
                if (online != null) {
                    label.setForeground(online ? current.start.darker() : current.stop);
                } else {
                    label.setForeground(current.buttonNeutral);
                }
                if (isSelected) {
                    label.setBackground(current.primary.darker());
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(current.panel);
                }
                label.setOpaque(true);
                return label;
            }
        });

        JScrollPane urlScroll = new JScrollPane(urlList);
        urlScroll.setPreferredSize(new Dimension(300, 150));
        center.add(urlScroll, BorderLayout.WEST);

        // Input row (top of center)
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputRow.setBackground(current.panel);

        JTextField urlField = new JTextField("https://", 30);
        urlField.setBackground(current.background);
        urlField.setForeground(current.text);

        JButton addBtn = new JButton("Thêm");
        JButton removeBtn = new JButton("Xóa");

        addHoverEffect(addBtn, current.primary, current.primary.brighter());
        addHoverEffect(removeBtn, current.buttonNeutral, current.buttonNeutral.brighter());

        inputRow.add(urlField);
        inputRow.add(addBtn);
        inputRow.add(removeBtn);
        inputRow.add(new JLabel("Chu kỳ (giây):") {{ setForeground(current.text); }});
        JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 3600, 1));
        inputRow.add(intervalSpinner);

        center.add(inputRow, BorderLayout.NORTH);

        // History area center
        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        historyArea.setBackground(current.background);
        historyArea.setForeground(current.text);
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Lịch sử kiểm tra"));
        center.add(historyScroll, BorderLayout.CENTER);

        // Chart panel at south of center
        ChartPanel chartPanel = new ChartPanel(null);
        chartPanel.setPreferredSize(new Dimension(600, 300));
        chartPanel.setBorder(BorderFactory.createTitledBorder("Biểu đồ phản hồi tổng hợp"));
        center.add(chartPanel, BorderLayout.SOUTH);

        main.add(center, BorderLayout.CENTER);

        // Bottom panel with Start/Stop and actions (keep simple as previous)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(current.background);

        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        stopBtn.setEnabled(false);

        addHoverEffect(startBtn, current.start, current.start.brighter());
        addHoverEffect(stopBtn, current.stop, current.stop.brighter());

        JButton exportBtn = new JButton("Export TXT");
        JButton clearBtn = new JButton("Xóa lịch sử");
        addHoverEffect(exportBtn, current.buttonNeutral, current.buttonNeutral.brighter());
        addHoverEffect(clearBtn, current.buttonNeutral, current.buttonNeutral.brighter());

        // Theme toggle (keeps hover)
        JButton themeToggle = new JButton("Dark Mode");
        addHoverEffect(themeToggle, current.primary.darker(), current.primary);

        bottom.add(startBtn);
        bottom.add(stopBtn);
        bottom.add(exportBtn);
        bottom.add(clearBtn);
        bottom.add(themeToggle);

        main.add(bottom, BorderLayout.SOUTH);

        // Populate behavior of add/remove
        addBtn.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.equals("https://") || url.equals("http://")) {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập chính xác URL");
                return;
            }
            if (url.isEmpty()) return;
            for (int i = 0; i < urlListModel.size(); i++) {
                if (urlListModel.get(i).equalsIgnoreCase(url)) {
                    JOptionPane.showMessageDialog(frame, "Website đã tồn tại trong danh sách!");
                    return;
                }
            }
            urlListModel.addElement(url);
        });

        removeBtn.addActionListener(e -> {
            int idx = urlList.getSelectedIndex();
            if (idx >= 0) urlListModel.remove(idx);
        });

        // Start action: similar to previous implementation, keep hover and update chartPanel
        startBtn.addActionListener(e -> {
            if (urlListModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Vui lòng thêm ít nhất một URL");
                return;
            }
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);

            int interval = (int) intervalSpinner.getValue();
            executor = Executors.newScheduledThreadPool(Math.max(2, urlListModel.size()));

            for (int i = 0; i < urlListModel.size(); i++) {
                String url = urlListModel.get(i);
                ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> {
                    WebsiteChecker.CheckResult r = WebsiteChecker.check(url);
                    String ts = fmt.format(new Date());
                    String line = String.format("[%s] %s - %s - %d - %dms%n",
                            ts, r.url, r.online ? "Online" : "Offline", r.httpCode, r.responseMs);
                    SwingUtilities.invokeLater(() -> {
                        historyArea.append(line);
                        historyArea.setCaretPosition(historyArea.getDocument().getLength());
                        HistoryManager.addRecord(ts, r);
                        ChartManager.updateChart(url, r.responseMs);
                        statusMap.put(r.url, r.online);
                        urlList.repaint();

                        List<String> urls = Collections.list(urlListModel.elements());
                        JFreeChart chart = ChartManager.getCombinedChart(urls);
                        chartPanel.setChart(chart);
                    });
                }, 0, interval, TimeUnit.SECONDS);
                tasks.add(task);
            }
        });

        // Stop
        stopBtn.addActionListener(e -> {
            for (ScheduledFuture<?> t : tasks) t.cancel(true);
            tasks.clear();
            if (executor != null) executor.shutdownNow();
            executor = null;
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        });

        // Export
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();
                File fileWithTxt = new File(selectedFile.getAbsolutePath() + ".txt");
                HistoryManager.exportTxt(fileWithTxt);
                JOptionPane.showMessageDialog(frame, "Export thành công");
            }
        });

        // Clear history
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Bạn có chắc muốn xóa toàn bộ lịch sử?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                HistoryManager.clearHistory();
                historyArea.setText("");
                JOptionPane.showMessageDialog(frame, "Đã xóa lịch sử kiểm tra");
            }
        });

        // Theme toggle: swap palettes (keeps hover behavior)
        themeToggle.addActionListener(e -> {
            boolean toDark = "Dark Mode".equals(themeToggle.getText());
            Palette p = toDark ? dark : light;
            applyPalette(main, p);
            themeToggle.setText(toDark ? "Light Mode" : "Dark Mode");
            frame.repaint();
        });

        frame.setContentPane(main);
        frame.setVisible(true);
    }

    private static void addHoverEffect(JButton button, Color normal, Color hover) {
        button.setBackground(normal);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(normal);
            }
        });
    }

    private static void applyPalette(Container root, Palette p) {
        root.setBackground(p.background);
        for (Component c : root.getComponents()) applyToComponent(c, p);
    }

    private static void applyToComponent(Component comp, Palette p) {
        if (comp instanceof JPanel) {
            comp.setBackground(p.panel);
            for (Component child : ((JPanel) comp).getComponents()) applyToComponent(child, p);
        } else if (comp instanceof JScrollPane) {
            comp.setBackground(p.panel);
            JViewport vp = ((JScrollPane) comp).getViewport();
            if (vp != null && vp.getView() != null) {
                vp.getView().setBackground(p.background);
                vp.getView().setForeground(p.text);
            }
        } else if (comp instanceof JLabel) {
            ((JLabel) comp).setForeground(p.primary);
        } else if (comp instanceof JButton) {
            ((JButton) comp).setForeground(Color.WHITE);
            // keep existing background (hover handles it)
        } else if (comp instanceof JList) {
            ((JList<?>) comp).setBackground(p.panel);
            ((JList<?>) comp).setForeground(p.text);
        } else if (comp instanceof JTextArea) {
            comp.setBackground(p.background);
            comp.setForeground(p.text);
        } else if (comp instanceof JTextField) {
            comp.setBackground(p.background);
            comp.setForeground(p.text);
        } else if (comp instanceof JSpinner) {
            comp.setBackground(p.background);
            comp.setForeground(p.text);
        } else if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) applyToComponent(child, p);
        }
    }

    private static class Palette {
        final Color background;
        final Color panel;
        final Color primary;
        final Color start;
        final Color stop;
        final Color buttonNeutral;
        final Color text;

        Palette(Color background, Color panel, Color primary, Color start, Color stop, Color buttonNeutral, Color text) {
            this.background = background;
            this.panel = panel;
            this.primary = primary;
            this.start = start;
            this.stop = stop;
            this.buttonNeutral = buttonNeutral;
            this.text = text;
        }
    }
}
