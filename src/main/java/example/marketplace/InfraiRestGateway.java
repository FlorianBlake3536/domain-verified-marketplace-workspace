package example.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiRestGateway implements InfraiGateway {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final String baseUrl;
    private final String key;

    public InfraiRestGateway(ObjectMapper json, @Value("${marketplace.infrai.base-url}") String baseUrl) {
        this.json = json;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    }

    public JsonNode addDomain(String domain) {
        return post("/v1/dns/domain/add", Map.of("domain", domain));
    }

    public JsonNode verifyDomain(String domain) {
        return post("/v1/dns/domain/verify", Map.of("domain", domain));
    }

    public JsonNode createUser(Map<String, Object> body) {
        return post("/v1/auth/user/create", body);
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            String payload = json.writeValueAsString(body);
            for (int attempt = 0; attempt < 4; attempt++) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .method("POST", HttpRequest.BodyPublishers.ofString(payload)).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // Decode the business envelope before interpreting the HTTP status.
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    long seconds = response.headers().firstValue("Retry-After")
                            .map(value -> { try { return Long.parseLong(value); } catch (NumberFormatException e) { return 0L; } })
                            .orElse(0L);
                    Thread.sleep(Math.max(seconds * 1000, 500L << attempt));
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new ApiRejected(response.statusCode(), error.path("code").asText("REQUEST_REJECTED"),
                            error.path("message").asText("Request rejected"));
                }
                if (response.statusCode() >= 500) throw new IllegalStateException("Upstream request failed");
                return envelope.path("data");
            }
            throw new IllegalStateException("Retry budget exhausted");
        } catch (IOException e) {
            throw new IllegalStateException("Could not decode response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", e);
        }
    }

    public static class ApiRejected extends RuntimeException {
        private final int status;
        private final String code;
        public ApiRejected(int status, String code, String detail) {
            super(detail);
            this.status = status;
            this.code = code;
        }
        public int status() { return status; }
        public String code() { return code; }
    }
}
