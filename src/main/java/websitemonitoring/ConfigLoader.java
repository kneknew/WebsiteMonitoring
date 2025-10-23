package websitemonitoring;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {}

    public static Properties loadConfig() {
        try {
            // 1 - system property -Dconfig=/path/to/config.properties
            String sys = System.getProperty("config");
            if (sys != null && !sys.isBlank()) {
                Path p = Paths.get(sys);
                if (Files.exists(p)) return loadFromPath(p);
            }
            // 2 - env CONFIG_PATH
            String env = System.getenv("CONFIG_PATH");
            if (env != null && !env.isBlank()) {
                Path p = Paths.get(env);
                if (Files.exists(p)) return loadFromPath(p);
            }
            // 3 - ./config.properties
            Path wd = Paths.get("config.properties");
            if (Files.exists(wd)) return loadFromPath(wd);
            // 4 - ~/.myapp/config.properties
            Path home = Paths.get(System.getProperty("user.home"), ".myapp", "config.properties");
            if (Files.exists(home)) return loadFromPath(home);
            // 5 - /etc/websitemonitoring/config.properties
            Path etc = Paths.get("/etc/websitemonitoring/config.properties");
            if (Files.exists(etc)) return loadFromPath(etc);
        } catch (Exception ignored) {}
        return new Properties();
    }

    private static Properties loadFromPath(Path p) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        }
        return props;
    }

    public static String getApiKey(Properties p) {
        String v = p.getProperty("google.api.key");
        if (v != null && !v.isBlank()) return v.trim();
        String env = System.getenv("GOOGLE_API_KEY");
        return (env == null || env.isBlank()) ? null : env.trim();
    }

    public static String getModel(Properties p) {
        String model = p.getProperty("gemini.model", "gemini-pro").trim();
        // --- DÒNG DEBUG ---
        System.out.println("--- [DEBUG] ConfigLoader: Đã đọc model name: '" + model + "'");
        // ------------------
        return model;
    }
}