package websitemonitoring;

import java.net.HttpURLConnection;
import java.net.URL;

public class WebsiteChecker {
    public static class CheckResult {
        public final String url;
        public final boolean online;
        public final int httpCode;
        public final long responseMs;
        public CheckResult(String url, boolean online, int httpCode, long responseMs) {
            this.url = url; this.online = online; this.httpCode = httpCode; this.responseMs = responseMs;
        }
    }

    public static CheckResult check(String urlStr) {
        long start = System.currentTimeMillis();
        int code = -1;
        boolean ok = false;
        try {
            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                urlStr = "http://" + urlStr;
            }
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.connect();
            code = conn.getResponseCode();
            ok = (code >= 200 && code < 400);
            conn.disconnect();
        } catch (Exception e) {
            ok = false;
        }
        long end = System.currentTimeMillis();
        return new CheckResult(urlStr, ok, code, end - start);
    }
}
