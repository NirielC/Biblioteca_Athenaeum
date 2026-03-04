package sv.arrupe.biblioteca.services;

import sv.arrupe.biblioteca.util.AppSession;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;


public class ApiClient {

    private final String baseUrl;
    private final HttpClient client;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    private HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");

        if (AppSession.isLoggedIn()) {
            b.header("Authorization", "Bearer " + AppSession.getToken());
        }
        return b;
    }

    public HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = baseRequest(path).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> post(String path, String jsonBody) throws Exception {
        HttpRequest req = baseRequest(path)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> put(String path, String jsonBody) throws Exception {
        HttpRequest req = baseRequest(path)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> patch(String path, String jsonBody) throws Exception {
        HttpRequest req = baseRequest(path)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> postMultipart(String path, File file) throws Exception {
        String boundary = "----JavaFXBoundary" + UUID.randomUUID();
        String CRLF = "\r\n";

        byte[] fileBytes = Files.readAllBytes(file.toPath());

        String partHeader =
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + CRLF +
                        "Content-Type: application/octet-stream" + CRLF + CRLF;

        String partFooter = CRLF + "--" + boundary + "--" + CRLF;

        byte[] headerBytes = partHeader.getBytes();
        byte[] footerBytes = partFooter.getBytes();

        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);

        if (AppSession.isLoggedIn()) {
            b.header("Authorization", "Bearer " + AppSession.getToken());
        }

        HttpRequest req = b.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}