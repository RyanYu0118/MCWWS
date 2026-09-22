package work.mcwws.worldsync;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/** Aliyun OSS signature v1. Both servers only make outbound HTTPS calls. */
public final class OssClient {
    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);

    private final String scheme;
    private final String endpointHost;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final boolean virtualHost;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public OssClient(String endpoint, String bucket, String accessKey, String secretKey, boolean virtualHost) {
        URI uri = URI.create(endpoint);
        this.scheme = uri.getScheme() == null ? "https" : uri.getScheme();
        this.endpointHost = uri.getHost();
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.virtualHost = virtualHost;
    }

    public void putFile(String key, Path file) throws IOException, InterruptedException {
        String date = now();
        String url = url(key);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Date", date)
                .header("Content-Type", "application/octet-stream")
                .header("Authorization", auth("PUT", "application/octet-stream", date, key))
                .PUT(HttpRequest.BodyPublishers.ofFile(file))
                .build();
        send(req, "PUT " + key);
    }

    public void putBytes(String key, byte[] body, String contentType) throws IOException, InterruptedException {
        String date = now();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url(key)))
                .timeout(Duration.ofMinutes(2))
                .header("Date", date)
                .header("Content-Type", contentType)
                .header("Authorization", auth("PUT", contentType, date, key))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        send(req, "PUT " + key);
    }

    /** @return null when the object does not exist */
    public byte[] getBytes(String key) throws IOException, InterruptedException {
        String date = now();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url(key)))
                .timeout(Duration.ofMinutes(2))
                .header("Date", date)
                .header("Authorization", auth("GET", "", date, key))
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() == 404) {
            return null;
        }
        if (resp.statusCode() >= 400) {
            throw new IOException("GET " + key + " HTTP " + resp.statusCode());
        }
        return resp.body();
    }

    public boolean getToFile(String key, Path dest) throws IOException, InterruptedException {
        String date = now();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url(key)))
                .timeout(Duration.ofMinutes(10))
                .header("Date", date)
                .header("Authorization", auth("GET", "", date, key))
                .GET()
                .build();
        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() == 404) {
            resp.body().close();
            return false;
        }
        if (resp.statusCode() >= 400) {
            resp.body().close();
            throw new IOException("GET " + key + " HTTP " + resp.statusCode());
        }
        Files.createDirectories(dest.getParent());
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        try (InputStream in = resp.body()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    private void send(HttpRequest req, String what) throws IOException, InterruptedException {
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            String body = resp.body() == null ? "" : resp.body();
            if (body.length() > 300) {
                body = body.substring(0, 300);
            }
            throw new IOException(what + " HTTP " + resp.statusCode() + " " + body);
        }
    }

    private String auth(String verb, String contentType, String date, String key) throws IOException {
        String stringToSign = verb + "\n\n" + contentType + "\n" + date + "\n/" + bucket + "/" + key;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            String sig = Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
            return "OSS " + accessKey + ":" + sig;
        } catch (Exception e) {
            throw new IOException("OSS 签名失败", e);
        }
    }

    private String url(String key) {
        String host = virtualHost ? bucket + "." + endpointHost : endpointHost;
        String path = virtualHost ? encodeKey(key) : bucket + "/" + encodeKey(key);
        return scheme + "://" + host + "/" + path;
    }

    private static String encodeKey(String key) {
        StringBuilder sb = new StringBuilder();
        for (String part : key.split("/", -1)) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(java.net.URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    private static String now() {
        return HTTP_DATE.format(ZonedDateTime.now(ZoneOffset.UTC));
    }
}
