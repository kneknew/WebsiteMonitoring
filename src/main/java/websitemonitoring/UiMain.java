package websitemonitoring;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

        // Palettes
        Palette light = new Palette(
                Color.decode("#FFFFFF"), // background
                Color.decode("#F5F7FA"), // panel
                Color.BLACK,             // primary (light mode)
                Color.decode("#2E7D32"), // start (green)
                Color.decode("#C62828"), // stop (red)
                Color.decode("#2D6A75"), // button neutral (teal-ish)
                Color.decode("#212121")  // text (dark)
        );
        Palette dark = new Palette(
                Color.decode("#2F3438"), // background (modern dark gray)
                Color.decode("#353A3E"), // panel (slightly lighter)
                Color.WHITE,             // primary (dark mode)
                Color.decode("#7ED957"), // start (bright green)
                Color.decode("#FF6B6B"), // stop (soft red)
                Color.decode("#6B7C86"), // button neutral (muted)
                Color.decode("#E6EEF3")  // text (soft off-white)
        );

        final AtomicReference<Palette> currentRef = new AtomicReference<>(light);
        final AtomicBoolean isDark = new AtomicBoolean(false);

        // Fonts
        final Font titleFont = new Font("Segoe UI", Font.BOLD, 32);
        final Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14);
        final Font monoFont = new Font("Consolas", Font.PLAIN, 15);
        final Font smallFont = new Font("Segoe UI", Font.PLAIN, 13);

        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setBackground(currentRef.get().background);

        JLabel title = new JLabel("WEBSITE MONITORING", SwingConstants.CENTER);
        title.setFont(titleFont);
        title.setForeground(currentRef.get().primary);
        main.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(currentRef.get().panel);

        final DefaultListModel<String> urlListModel = new DefaultListModel<>();
        final JList<String> urlList = new JList<>(urlListModel);
        urlList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        urlList.setBackground(currentRef.get().panel);
        urlList.setForeground(currentRef.get().text);
        urlList.setFixedCellHeight(30);
        urlList.setFont(defaultFont);

        
     // cell renderer: icon + colored text
        urlList.setCellRenderer(new DefaultListCellRenderer() {
            private final Icon iconOnline = circleIcon(new Color(0x43A047), 12);   // xanh lá
            private final Icon iconOffline = circleIcon(new Color(0xE53935), 12);  // đỏ
            private final Icon iconUnknown = circleIcon(new Color(0x9E9E9E), 12);  // xám

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Palette p = currentRef.get();
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(defaultFont);
                String url = value.toString();

                Boolean online = statusMap.get(url);
                if (online == null) {
                    label.setIcon(iconUnknown);
                    label.setForeground(p.buttonNeutral);
                } else if (online) {
                    label.setIcon(iconOnline);
                    label.setForeground(p.start.darker());
                } else {
                    label.setIcon(iconOffline);
                    label.setForeground(p.stop);
                }

                label.setIconTextGap(10);

                if (isSelected) {
                    Color selBg = p.primary.equals(Color.WHITE) ? new Color(0x1E88E5) : p.primary.darker();
                    label.setBackground(selBg);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(p.panel);
                }

                label.setOpaque(true);
                return label;
            }
        });

        // Allow deselect when clicking empty area or clicking same selected item
        urlList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int idx = urlList.locationToIndex(e.getPoint());
                Rectangle cellBounds = (idx == -1) ? null : urlList.getCellBounds(idx, idx);

                if (idx == -1 || cellBounds == null || !cellBounds.contains(e.getPoint())) {
                    urlList.clearSelection();
                    urlList.getSelectionModel().clearSelection();
                    urlList.getParent().requestFocusInWindow(); 
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    urlList.setSelectedIndex(idx);
                    urlList.requestFocusInWindow();
                }
            }
        });

        final JScrollPane urlScroll = new JScrollPane(urlList);
        urlScroll.setPreferredSize(new Dimension(300, 160));
        TitledBorder urlBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(currentRef.get().primary), "List of Websites");
        urlBorder.setTitleColor(currentRef.get().primary);
        urlBorder.setTitleFont(defaultFont);
        urlScroll.setBorder(urlBorder);
        center.add(urlScroll, BorderLayout.WEST);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputRow.setBackground(currentRef.get().panel);

        final JTextField urlField = new JTextField("https://", 30);
        urlField.setBackground(currentRef.get().background);
        urlField.setForeground(currentRef.get().text);
        urlField.setFont(defaultFont);
        Dimension urlFieldPref = urlField.getPreferredSize();
        
        urlField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private final String prefix = "https://";
            private void check() {
                SwingUtilities.invokeLater(() -> {
                    String text = urlField.getText();
                    if (!text.startsWith(prefix)) {
                        urlField.setText(prefix);
                        urlField.setCaretPosition(prefix.length());
                    } else if (urlField.getCaretPosition() < prefix.length()) {
                        urlField.setCaretPosition(prefix.length());
                    }
                });
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { check(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { check(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });


        // Add / Remove buttons 
        final JButton addBtn = new JButton("Add");
        final JButton removeBtn = new JButton("Delete");
        addBtn.setFont(defaultFont);
        removeBtn.setFont(defaultFont);

        // Color bases
        Color addNormal = new Color(0x1976D2); // blue
        Color addHover = addNormal.brighter();
        Color remNormal = new Color(0xEF5350); // red
        Color remHover = remNormal.brighter();

        styleFlatButton(addBtn, addNormal);
        styleFlatButton(removeBtn, remNormal);
        addBtn.setPreferredSize(new Dimension(90, urlFieldPref.height));
        removeBtn.setPreferredSize(new Dimension(90, urlFieldPref.height));
        addHoverEffect(addBtn, addNormal, addHover);
        addHoverEffect(removeBtn, remNormal, remHover);

        inputRow.add(urlField);
        inputRow.add(addBtn);
        inputRow.add(removeBtn);
        JLabel cycleLabel = new JLabel("Cycle (seconds):");
        cycleLabel.setForeground(currentRef.get().text);
        cycleLabel.setFont(smallFont);
        inputRow.add(cycleLabel);

        final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 3600, 1));
        intervalSpinner.setFont(defaultFont);
        Dimension spinnerPref = intervalSpinner.getPreferredSize();
        spinnerPref.height = urlFieldPref.height;
        intervalSpinner.setPreferredSize(spinnerPref);
        setSpinnerColors(intervalSpinner, currentRef.get());
        inputRow.add(intervalSpinner);

        center.add(inputRow, BorderLayout.NORTH);

        final JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(monoFont);
        historyArea.setBackground(currentRef.get().background);
        historyArea.setForeground(currentRef.get().text);
        final JScrollPane historyScroll = new JScrollPane(historyArea);
        TitledBorder historyBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(currentRef.get().primary), "Test History");
        historyBorder.setTitleColor(currentRef.get().primary);
        historyBorder.setTitleFont(defaultFont);
        historyScroll.setBorder(historyBorder);
        historyScroll.setPreferredSize(new Dimension(0, 280));
        center.add(historyScroll, BorderLayout.CENTER);

        final ChartPanel chartPanel = new ChartPanel(null);
        chartPanel.setPreferredSize(new Dimension(0, 320));
        TitledBorder chartBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(currentRef.get().primary), "Summary Response Chart");
        chartBorder.setTitleColor(currentRef.get().primary);
        chartBorder.setTitleFont(defaultFont);
        chartPanel.setBorder(chartBorder);
        chartPanel.setBackground(currentRef.get().background);
        center.add(chartPanel, BorderLayout.SOUTH);

        main.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(currentRef.get().background);

        final JButton startBtn = new JButton("Start");
        final JButton stopBtn = new JButton("Pause");
        stopBtn.setEnabled(false);
        startBtn.setFont(defaultFont);
        stopBtn.setFont(defaultFont);

        styleFlatButton(startBtn, currentRef.get().start);
        styleFlatButton(stopBtn, currentRef.get().stop);
        addHoverEffect(startBtn, currentRef.get().start, currentRef.get().start.brighter());
        addHoverEffect(stopBtn, currentRef.get().stop, currentRef.get().stop.brighter());

        final JButton exportBtn = new JButton("Export TXT file");
        final JButton clearBtn = new JButton("Delete history");
        exportBtn.setFont(defaultFont);
        clearBtn.setFont(defaultFont);
        styleFlatButton(exportBtn, currentRef.get().buttonNeutral);
        styleFlatButton(clearBtn, currentRef.get().buttonNeutral);
        addHoverEffect(exportBtn, currentRef.get().buttonNeutral, currentRef.get().buttonNeutral.brighter());
        addHoverEffect(clearBtn, currentRef.get().buttonNeutral, currentRef.get().buttonNeutral.brighter());

        final JButton themeToggle = new JButton("Dark Mode");
        themeToggle.setFont(defaultFont);
        styleFlatButton(themeToggle, currentRef.get().primary.equals(Color.WHITE) ? new Color(0xB0BEC5) : currentRef.get().primary);
        addHoverEffect(themeToggle, currentRef.get().primary.darker(), currentRef.get().primary);

        bottom.add(startBtn);
        bottom.add(stopBtn);
        bottom.add(exportBtn);
        bottom.add(clearBtn);
        bottom.add(themeToggle);

        main.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(main);
        frame.setVisible(true);

        // Actions
        addBtn.addActionListener(e -> {
            String url = urlField.getText().trim();

            if (url.equals("https://") || url.equals("http://")) {
                JOptionPane.showMessageDialog(frame, "Please enter the correct URL");
                return;
            }
            if (url.isEmpty()) return;

            if (!url.matches("https?://([\\w-]+\\.)+[a-zA-Z]{2,}(/.*)?")) {
                JOptionPane.showMessageDialog(frame, "Invalid URL! Please enter in correct format");
                return;
            }

            for (int i = 0; i < urlListModel.size(); i++) {
                if (urlListModel.get(i).equalsIgnoreCase(url)) {
                    JOptionPane.showMessageDialog(frame, "Website already exists in the list!");
                    return;
                }
            }

            urlListModel.addElement(url);

            // keep https:// after add URL
            if (url.startsWith("https://")) {
                urlField.setText("https://");
            } else {
                urlField.setText("http://");
            }
            urlField.requestFocus();
        });


        removeBtn.addActionListener(e -> {
            int idx = urlList.getSelectedIndex();
            if (idx >= 0) {
                urlListModel.remove(idx);
                urlList.clearSelection();
            }
        });

        startBtn.addActionListener(e -> {
            if (urlListModel.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please add at least one URL");
                return;
            }
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);

            int interval = (int) intervalSpinner.getValue();
            executor = Executors.newScheduledThreadPool(Math.max(2, urlListModel.size()));

            for (int i = 0; i < urlListModel.size(); i++) {
                final String url = urlListModel.get(i);
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
                        JFreeChart chart = ChartManager.getCombinedChart(urls, isDark.get());
                        chartPanel.setChart(chart);
                        chartPanel.setBackground(currentRef.get().background);
                    });
                }, 0, interval, TimeUnit.SECONDS);
                tasks.add(task);
            }
        });

        stopBtn.addActionListener(e -> {
            for (ScheduledFuture<?> t : tasks) t.cancel(true);
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
                JOptionPane.showMessageDialog(frame, "Export successful");
            }
        });

        // Clear history: also clear charts and statuses
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(frame,
            		"Are you sure you want to delete all history?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                HistoryManager.clearHistory();
                historyArea.setText("");

                // Clear chart data and reset panel
                ChartManager.clearAll();
                chartPanel.setChart(null);
                chartPanel.setBackground(currentRef.get().background);

                // clear statuses and selection
                statusMap.clear();
                urlList.clearSelection();

                JOptionPane.showMessageDialog(frame, "Cleared test history and reset chart");
            }
        });

        themeToggle.addActionListener(e -> {
            boolean toDark = "Dark Mode".equals(themeToggle.getText());
            Palette p = toDark ? dark : light;
            currentRef.set(p);
            isDark.set(toDark);
            applyPalette(main, p, defaultFont, monoFont, smallFont);

            ((TitledBorder) urlScroll.getBorder()).setTitleColor(p.primary);
            ((TitledBorder) historyScroll.getBorder()).setTitleColor(p.primary);
            ((TitledBorder) chartPanel.getBorder()).setTitleColor(p.primary);

            title.setForeground(p.primary);

            // Update flat-style buttons to new palette values
            styleFlatButton(startBtn, p.start);
            styleFlatButton(stopBtn, p.stop);
            styleFlatButton(exportBtn, p.buttonNeutral);
            styleFlatButton(clearBtn, p.buttonNeutral);
            styleFlatButton(themeToggle, p.primary.equals(Color.WHITE) ? new Color(0xB0BEC5) : p.primary);

            // Update add/remove to 
            Color addTheme = addNormalForTheme(toDark);
            Color remTheme = remNormalForTheme(toDark);
            styleFlatButton(addBtn, addTheme);
            styleFlatButton(removeBtn, remTheme);
            addBtn.setPreferredSize(new Dimension(90, urlField.getPreferredSize().height));
            removeBtn.setPreferredSize(new Dimension(90, urlField.getPreferredSize().height));
            addHoverEffect(addBtn, addTheme, addTheme.brighter());
            addHoverEffect(removeBtn, remTheme, remTheme.brighter());

            // Update spinner colors
            setSpinnerColors(intervalSpinner, p);

            // refresh chart if any data exists
            List<String> urls = Collections.list(urlListModel.elements());
            if (!urls.isEmpty()) {
                JFreeChart chart = ChartManager.getCombinedChart(urls, isDark.get());
                chartPanel.setChart(chart);
            } else {
                chartPanel.setChart(null);
            }
            chartPanel.setBackground(p.background);

            themeToggle.setText(toDark ? "Light Mode" : "Dark Mode");
            frame.repaint();
        });
    }

    // Create a colored circle icon (static, outside createAndShowGui)
    private static Icon circleIcon(Color color, int size) {
        return new Icon() {
            private final int s = size;
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x, y, s, s);
                g2.setColor(new Color(0, 0, 0, 40));
                g2.drawOval(x, y, s, s);
                g2.dispose();
            }
            @Override public int getIconWidth() { return s; }
            @Override public int getIconHeight() { return s; }
        };
    }

    // Button style
    private static void styleFlatButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(8, 12, 8, 12));
    }

    private static Color addNormalForTheme(boolean dark) {
        return dark ? new Color(0x1E88E5) : new Color(0x1976D2);
    }
    private static Color remNormalForTheme(boolean dark) {
        return dark ? new Color(0xEF5350) : new Color(0xE53935);
    }

    // Hover effect used for general buttons
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

    // Helper to set spinner editor colors to match palette
    private static void setSpinnerColors(JSpinner spinner, Palette p) {
        try {
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setBackground(p.background);
                tf.setForeground(p.text);
                tf.setCaretColor(p.text);
                tf.setBorder(BorderFactory.createLineBorder(p.panel.darker()));
            }
            spinner.setBackground(p.background);
            spinner.setForeground(p.text);
        } catch (Exception ignored) { }
    }

    private static void applyPalette(Container root, Palette p, Font defaultFont, Font monoFont, Font smallFont) {
        root.setBackground(p.background);
        for (Component c : root.getComponents()) applyToComponent(c, p, defaultFont, monoFont, smallFont);
    }

    private static void applyToComponent(Component comp, Palette p, Font defaultFont, Font monoFont, Font smallFont) {
        if (comp instanceof JPanel) {
            comp.setBackground(p.panel);
            for (Component child : ((JPanel) comp).getComponents()) applyToComponent(child, p, defaultFont, monoFont, smallFont);
        } else if (comp instanceof JScrollPane) {
            comp.setBackground(p.panel);
            JViewport vp = ((JScrollPane) comp).getViewport();
            if (vp != null && vp.getView() != null) {
                vp.getView().setBackground(p.background);
                vp.getView().setForeground(p.text);
                vp.getView().setFont(defaultFont);
            }
        } else if (comp instanceof JLabel) {
            ((JLabel) comp).setForeground(p.primary);
            ((JLabel) comp).setFont(defaultFont);
        } else if (comp instanceof JButton) {
            ((JButton) comp).setForeground(Color.WHITE);
            ((JButton) comp).setFont(defaultFont);
            // do not overwrite custom backgrounds (styleFlatButton handles that)
        } else if (comp instanceof JList) {
            ((JList<?>) comp).setBackground(p.panel);
            ((JList<?>) comp).setForeground(p.text);
            ((JList<?>) comp).setFont(defaultFont);
        } else if (comp instanceof JTextArea) {
            comp.setBackground(p.background);
            comp.setForeground(p.text);
            comp.setFont(monoFont);
        } else if (comp instanceof JTextField) {
            comp.setBackground(p.background);
            comp.setForeground(p.text);
            comp.setFont(defaultFont);
        } else if (comp instanceof JSpinner) {
            comp.setBackground(p.background);
            comp.setForeground(p.text);
            comp.setFont(defaultFont);
            setSpinnerColors((JSpinner) comp, p);
        } else if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) applyToComponent(child, p, defaultFont, monoFont, smallFont);
        }
    }

    private static class Palette {
        Color background;
        Color panel;
        Color primary;
        Color start;
        Color stop;
        Color buttonNeutral;
        Color text;

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
