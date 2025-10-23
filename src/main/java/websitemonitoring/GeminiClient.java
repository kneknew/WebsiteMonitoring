package websitemonitoring;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GeminiClient {
    // URL này đã đúng (khớp với API bạn đã bật)
    private static final String BASE = "https://generativelanguage.googleapis.com/v1/models/";
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // Tên hàm giữ nguyên, nhưng logic bên trong thay đổi
    public Map<String,Object> generateWithUrlContext(String model, String userPrompt, String url, int maxTokens, double temperature) throws Exception {

        String endpoint = BASE + model + ":generateContent?key=" + apiKey;

        // --- CẤU TRÚC JSON MỚI ---
        Map<String,Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", userPrompt)
                        })
                },
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", maxTokens
                )
        );
        // --- KẾT THÚC CẤU TRÚC MỚI ---

        String json = mapper.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                // --- ĐÂY LÀ DÒNG ĐÃ SỬA (BodyPublishers) ---
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return mapper.readValue(resp.body(), Map.class);
    }
}