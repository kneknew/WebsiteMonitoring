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

    // Xu ly luong kiem tra dinh ki
    private static ScheduledExecutorService executor;

    // Danh sach tac vu kiem tra dang chay
    private static final List<ScheduledFuture<?>> tasks = new ArrayList<>();

    public static void createAndShowGui() {
        // Main window
        JFrame frame = new JFrame("Website Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(12,12));
        main.setBorder(new EmptyBorder(16,16,16,16));
        main.setBackground(Color.WHITE);

        // Tiêu đề
        JLabel title = new JLabel("WEBSITE MONITORING", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(33,150,243));
        main.add(title, BorderLayout.NORTH);


        JPanel center = new JPanel(new BorderLayout(8,8));

        // Danh sach website
        DefaultListModel<String> urlListModel = new DefaultListModel<>();
        JList<String> urlList = new JList<>(urlListModel);
        urlList.setBorder(BorderFactory.createTitledBorder("Danh sách website"));
        JScrollPane urlScroll = new JScrollPane(urlList);
        urlScroll.setPreferredSize(new Dimension(300, 150));

        // Input url, add, delete
        JTextField urlField = new JTextField("https://", 30);
        JButton addBtn = new JButton("Thêm");
        JButton removeBtn = new JButton("Xóa");

        // Duplicate check khi them URL
        addBtn.addActionListener(e -> {
            String url = urlField.getText().trim();

            if (url.equals("https://") || url.equals("http://")) {
                JOptionPane.showMessageDialog(frame, "Vui long nhap chinh xac url");
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

        // Xoa url
        removeBtn.addActionListener(e -> {
            int index = urlList.getSelectedIndex();
            if (index >= 0) urlListModel.remove(index);
        });

        // Nhap va xu ly chu trinh kiem tra
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT,10,10));
        inputRow.add(urlField);
        inputRow.add(addBtn);
        inputRow.add(removeBtn);
        inputRow.add(new JLabel("Chu kỳ (giây):"));
        JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 60, 1));
        inputRow.add(intervalSpinner);

        // Start va Stop
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        stopBtn.setEnabled(false);
        inputRow.add(startBtn);
        inputRow.add(stopBtn);

        center.add(inputRow, BorderLayout.NORTH);
        center.add(urlScroll, BorderLayout.WEST);

        // Hien thi lich su kiem tra
        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Lịch sử kiểm tra"));
        center.add(historyScroll, BorderLayout.CENTER);

        main.add(center, BorderLayout.CENTER);

        // Khu vuc bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exportBtn = new JButton("Export TXT");
        JButton chartBtn = new JButton("Xem biểu đồ");
        JButton clearBtn = new JButton("Xóa lịch sử");
        bottom.add(chartBtn);
        bottom.add(exportBtn);
        bottom.add(clearBtn);
        main.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(main);
        frame.setVisible(true);

        // Su kien xu ly
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
                    });
                }, 0, interval, TimeUnit.SECONDS);
                tasks.add(task);
            }
        });

        // Stop
        stopBtn.addActionListener(e -> {
            for (ScheduledFuture<?> task : tasks) task.cancel(true);
            tasks.clear();
            if (executor != null) executor.shutdownNow();
            executor = null;
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        });

//         Export ra txt
        exportBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fc.getSelectedFile();
                // Luôn thêm ".txt" vào tên file
                File fileWithTxt = new File(selectedFile.getAbsolutePath() + ".txt");
                HistoryManager.exportTxt(fileWithTxt);
                JOptionPane.showMessageDialog(frame, "Export thành công");
            }
        });

//        exportBtn.addActionListener(e -> {
//            JFileChooser fc = new JFileChooser();
//            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
//                File selectedFile = fc.getSelectedFile();
//                // Luôn thêm ".txt" vào tên file
//                File fileWithTxt = new File(selectedFile.getAbsolutePath() + ".txt");
//                HistoryManager.exportTxt(fileWithTxt);
//                JOptionPane.showMessageDialog(frame, "Export thành công");
//            }
//        });


        // Hien thi bieu do
        chartBtn.addActionListener(e -> {
            String selected = urlList.getSelectedValue();
            if (selected != null) {
                ChartManager.showChart(selected);
            } else {
                JOptionPane.showMessageDialog(frame, "Vui lòng chọn một website để xem biểu đồ");
            }
        });

        // Xoa lich su
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