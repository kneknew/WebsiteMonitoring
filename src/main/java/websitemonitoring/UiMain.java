package websitemonitoring;

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
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(12,12));
        main.setBorder(new EmptyBorder(16,16,16,16));
        main.setBackground(Color.WHITE);

        JLabel title = new JLabel("WEBSITE MONITORING", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(33,150,243));
        main.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8,8));

        DefaultListModel<String> urlListModel = new DefaultListModel<>();
        JList<String> urlList = new JList<>(urlListModel);
        urlList.setBorder(BorderFactory.createTitledBorder("Danh sách website"));
        urlList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String url = value.toString();
                Boolean online = statusMap.get(url);
                if (online != null) {
                    label.setForeground(online ? new Color(0, 128, 0) : Color.RED);
                } else {
                    label.setForeground(Color.GRAY);
                }
                return label;
            }
        });
        JScrollPane urlScroll = new JScrollPane(urlList);
        urlScroll.setPreferredSize(new Dimension(300, 150));

        JTextField urlField = new JTextField("https://", 30);
        JButton addBtn = new JButton("Thêm");
        JButton removeBtn = new JButton("Xóa");

        addBtn.addActionListener(e -> {
            String url = urlField.getText().trim();
            if (url.equals("https://") || url.equals("http://")) {
                JOptionPane.showMessageDialog(frame, "Vui lòng nhập chính xác URL");
                return;
            }
            if (url.isEmpty()) return;

            boolean exists = false;
            for (int i = 0; i < urlListModel.size(); i++) {
                if (urlListModel.get(i).equalsIgnoreCase(url)) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                JOptionPane.showMessageDialog(frame, "Website đã tồn tại trong danh sách!");
            } else {
                urlListModel.addElement(url);
            }
        });

        removeBtn.addActionListener(e -> {
            int index = urlList.getSelectedIndex();
            if (index >= 0) urlListModel.remove(index);
        });

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
        inputRow.add(urlField);
        inputRow.add(addBtn);
        inputRow.add(removeBtn);
        inputRow.add(new JLabel("Chu kỳ (giây):"));
        JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 60, 1));
        inputRow.add(intervalSpinner);

        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        stopBtn.setEnabled(false);
        inputRow.add(startBtn);
        inputRow.add(stopBtn);

        center.add(inputRow, BorderLayout.NORTH);
        center.add(urlScroll, BorderLayout.WEST);

        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Lịch sử kiểm tra"));
        center.add(historyScroll, BorderLayout.CENTER);

        main.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportBtn = new JButton("Export TXT");
        JButton clearBtn = new JButton("Xóa lịch sử");

        String[] chartOptions = {"Xem biểu đồ từng website", "Xem biểu đồ tất cả"};
        JComboBox<String> chartModeBox = new JComboBox<>(chartOptions);
        JButton chartBtn = new JButton("Hiển thị biểu đồ");

        bottom.add(chartModeBox);
        bottom.add(chartBtn);
        bottom.add(exportBtn);
        bottom.add(clearBtn);
        main.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(main);
        frame.setVisible(true);

        startBtn.addActionListener(e -> {
            if (urlListModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Vui lòng thêm ít nhất một URL");
                return;
            }

            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            executor = Executors.newScheduledThreadPool(urlListModel.size());
            int interval = (int) intervalSpinner.getValue();

            for (int i = 0; i < urlListModel.size(); i++) {
                String url = urlListModel.get(i);
                ScheduledFuture<?> task = executor.scheduleAtFixedRate(() -> {
                    WebsiteChecker.CheckResult r = WebsiteChecker.check(url);
                    String ts = fmt.format(new Date());
                    String line = String.format("[%s] %s - %s - %d - %dms%n",
                            ts, r.url, r.online ? "Online" : "Offline", r.httpCode, r.responseMs);
                    SwingUtilities.invokeLater(() -> {
                        historyArea.append(line);
                        HistoryManager.addRecord(ts, r);
                        ChartManager.updateChart(url, r.responseMs);
                        statusMap.put(r.url, r.online);
                        urlList.repaint();
                    });
                }, 0, interval, TimeUnit.SECONDS);
                tasks.add(task);
            }
        });

        stopBtn.addActionListener(e -> {
            for (ScheduledFuture<?> task : tasks) task.cancel(true);
            tasks.clear();
            if (executor != null) executor.shutdownNow();
            executor = null;
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        });

        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();
                File fileWithTxt = new File(selectedFile.getAbsolutePath() + ".txt");
                HistoryManager.exportTxt(fileWithTxt);
                JOptionPane.showMessageDialog(frame, "Export thành công");
            }
        });

        chartBtn.addActionListener(e -> {
            String mode = (String) chartModeBox.getSelectedItem();
            if ("Xem biểu đồ từng website".equals(mode)) {
                String selected = urlList.getSelectedValue();
                if (selected != null) {
                    ChartManager.showChart(selected);
                } else {
                    JOptionPane.showMessageDialog(frame, "Vui lòng chọn một website để xem biểu đồ");
                }
            } else {
                if (urlListModel.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Không có website nào để hiển thị biểu đồ");
                    return;
                }
                List<String> urls = Collections.list(urlListModel.elements());
                ChartManager.showCombinedChart(urls);
            }
        });

        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Bạn có chắc muốn xóa toàn bộ lịch sử?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                HistoryManager.clearHistory();
                historyArea.setText("");
                JOptionPane.showMessageDialog(frame, "Đã xóa lịch sử kiểm tra");
            }
        });
    }
}
