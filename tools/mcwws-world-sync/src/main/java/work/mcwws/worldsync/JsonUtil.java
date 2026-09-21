package work.mcwws.worldsync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Minimal JSON object/array codec for the sync protocol (no extra libraries). */
public final class JsonUtil {
    private JsonUtil() {}

    public static String stringify(Map<String, Object> map) {
        return writeValue(map);
    }

    public static Map<String, Object> parseObject(String json) {
        Object v = new Parser(json).parseValue();
        if (v instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) m;
            return typed;
        }
        throw new IllegalArgumentException("JSON root is not an object");
    }

    public static String str(Map<String, Object> o, String key, String def) {
        Object v = o.get(key);
        return v == null ? def : String.valueOf(v);
    }

    public static boolean bool(Map<String, Object> o, String key, boolean def) {
        Object v = o.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v == null) {
            return def;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    public static long lng(Map<String, Object> o, String key, long def) {
        Object v = o.get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v == null) {
            return def;
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int num(Map<String, Object> o, String key, int def) {
        return (int) lng(o, key, def);
    }

    @SuppressWarnings("unchecked")
    public static List<String> strList(Map<String, Object> o, String key) {
        Object v = o.get(key);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
        }
        return out;
    }

    private static String writeValue(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Boolean || v instanceof Number) {
            return String.valueOf(v);
        }
        if (v instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(quote(String.valueOf(e.getKey()))).append(':').append(writeValue(e.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (v instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object item : it) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(writeValue(item));
            }
            sb.append(']');
            return sb.toString();
        }
        return quote(String.valueOf(v));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s == null ? "" : s.trim();
        }

        Object parseValue() {
            skip();
            if (i >= s.length()) {
                throw new IllegalArgumentException("empty JSON");
            }
            char c = s.charAt(i);
            if (c == '{') {
                return parseObj();
            }
            if (c == '[') {
                return parseArr();
            }
            if (c == '"') {
                return parseStr();
            }
            if (c == 't' || c == 'f') {
                return parseBool();
            }
            if (c == 'n') {
                expect("null");
                return null;
            }
            return parseNum();
        }

        private Map<String, Object> parseObj() {
            expect("{");
            Map<String, Object> map = new LinkedHashMap<>();
            skip();
            if (peek('}')) {
                i++;
                return map;
            }
            while (true) {
                skip();
                String key = parseStr();
                skip();
                expect(":");
                Object val = parseValue();
                map.put(key, val);
                skip();
                if (peek('}')) {
                    i++;
                    return map;
                }
                expect(",");
            }
        }

        private List<Object> parseArr() {
            expect("[");
            List<Object> list = new ArrayList<>();
            skip();
            if (peek(']')) {
                i++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skip();
                if (peek(']')) {
                    i++;
                    return list;
                }
                expect(",");
            }
        }

        private String parseStr() {
            expect("\"");
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (i >= s.length()) {
                        break;
                    }
                    char e = s.charAt(i++);
                    sb.append(switch (e) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> parseHex();
                        default -> e;
                    });
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("unterminated string");
        }

        private char parseHex() {
            if (i + 4 > s.length()) {
                return '?';
            }
            int v = Integer.parseInt(s.substring(i, i + 4), 16);
            i += 4;
            return (char) v;
        }

        private Boolean parseBool() {
            if (s.startsWith("true", i)) {
                i += 4;
                return true;
            }
            expect("false");
            return false;
        }

        private Number parseNum() {
            int start = i;
            if (peek('-')) {
                i++;
            }
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            String n = s.substring(start, i);
            if (n.contains(".")) {
                return Double.parseDouble(n);
            }
            long l = Long.parseLong(n);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private boolean peek(char c) {
            return i < s.length() && s.charAt(i) == c;
        }

        private void expect(String t) {
            skip();
            if (!s.startsWith(t, i)) {
                throw new IllegalArgumentException("expected " + t + " at " + i);
            }
            i += t.length();
        }
    }
}
