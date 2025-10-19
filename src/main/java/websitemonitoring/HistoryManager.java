package websitemonitoring;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final List<String> records = new ArrayList<>();

    public static void addRecord(String timestamp, WebsiteChecker.CheckResult r) {
        String line = String.format("%s,%s,%s,%d,%d", timestamp, r.url, r.online ? "Online" : "Offline", r.httpCode, r.responseMs);
        records.add(line);
    }

    public static void exportCsv(File file) {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("timestamp,url,status,httpCode,responseMs\n");
            for (String s : records) fw.write(s + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
