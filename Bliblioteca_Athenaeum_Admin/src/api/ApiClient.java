package api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {

    // AJUSTA esto si tu API usa otro puerto
    private static final String BASE_URL = "http://localhost:5145";

    private static final HttpClient http = HttpClient.newHttpClient();

    private static HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json");

        // Si ya hay token (logueado), manda Authorization
        if (Session.token != null && !Session.token.isBlank()) {
            b.header("Authorization", "Bearer " + Session.token);
        }
        return b;
    }

    private static String send(HttpRequest req) throws Exception {
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        int code = res.statusCode();

        if (code >= 200 && code < 300) return res.body();

        // Lanza error con status + body
        throw new RuntimeException("HTTP " + code + " -> " + res.body());
    }

    public static String get(String path) throws Exception {
        HttpRequest req = baseRequest(path).GET().build();
        return send(req);
    }

    public static String postJson(String path, String jsonBody) throws Exception {
        HttpRequest req = baseRequest(path)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return send(req);
    }

    public static String putJson(String path, String jsonBody) throws Exception {
        HttpRequest req = baseRequest(path)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return send(req);
    }

    public static String delete(String path) throws Exception {
        HttpRequest req = baseRequest(path).DELETE().build();
        return send(req);
    }
}