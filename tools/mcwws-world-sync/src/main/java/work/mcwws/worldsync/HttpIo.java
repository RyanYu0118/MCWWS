package work.mcwws.worldsync;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HttpIo {
    private HttpIo() {}

    public static boolean authorize(HttpExchange ex, String token) {
        String header = header(ex.getRequestHeaders(), "Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return tokenEquals(header.substring(7).trim(), token);
        }
        String q = query(ex, "token");
        return tokenEquals(q, token);
    }

    public static String header(Headers headers, String name) {
        List<String> v = headers.get(name);
        if (v == null || v.isEmpty()) {
            return null;
        }
        return v.get(0);
    }

    public static boolean tokenEquals(String a, String b) {
        byte[] x = (a == null ? "" : a).getBytes(StandardCharsets.UTF_8);
        byte[] y = (b == null ? "" : b).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(x, y);
    }

    public static String query(HttpExchange ex, String key) {
        URI uri = ex.getRequestURI();
        String raw = uri.getRawQuery();
        if (raw == null) {
            return null;
        }
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            String k = eq < 0 ? part : part.substring(0, eq);
            String v = eq < 0 ? "" : part.substring(eq + 1);
            if (key.equals(URLDecoder.decode(k, StandardCharsets.UTF_8))) {
                return URLDecoder.decode(v, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public static byte[] readAll(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return in.readAllBytes();
        }
    }

    public static String readUtf8(HttpExchange ex) throws IOException {
        return new String(readAll(ex), StandardCharsets.UTF_8);
    }

    public static void json(HttpExchange ex, int code, Map<String, Object> body) throws IOException {
        byte[] bytes = JsonUtil.stringify(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void text(HttpExchange ex, int code, String msg) throws IOException {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static Map<String, Object> ok(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    public static Map<String, Object> err(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", false);
        m.put("error", message);
        return m;
    }

    public static HttpRequest.Builder authed(String url, String token) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json");
    }
}
