package work.mcwws.buildbridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 编解码，仅覆盖本插件 HTTP 请求/响应所需结构。
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    public static Object parse(String json) {
        if (json == null) {
            return null;
        }
        Parser p = new Parser(json.trim());
        Object value = p.parseValue();
        p.skipWs();
        if (!p.eof()) {
            throw new IllegalArgumentException("Trailing JSON content at " + p.index);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected JSON object");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("Expected JSON array");
        }
        return (List<Object>) value;
    }

    public static String str(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        return v == null ? null : String.valueOf(v);
    }

    public static String str(Map<String, Object> obj, String key, String fallback) {
        String v = str(obj, key);
        return v == null || v.isBlank() ? fallback : v;
    }

    public static int i(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v == null) {
            throw new IllegalArgumentException("Missing number: " + key);
        }
        return Integer.parseInt(String.valueOf(v));
    }

    public static Integer iOrNull(Map<String, Object> obj, String key) {
        if (!obj.containsKey(key) || obj.get(key) == null) {
            return null;
        }
        return i(obj, key);
    }

    public static double d(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v == null) {
            throw new IllegalArgumentException("Missing number: " + key);
        }
        return Double.parseDouble(String.valueOf(v));
    }

    public static boolean bool(Map<String, Object> obj, String key, boolean fallback) {
        Object v = obj.get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (value instanceof Iterable<?> it) {
            sb.append('[');
            boolean first = true;
            for (Object item : it) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, item);
            }
            sb.append(']');
        } else {
            writeString(sb, String.valueOf(value));
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int index;

        private Parser(String s) {
            this.s = s;
        }

        private boolean eof() {
            return index >= s.length();
        }

        private void skipWs() {
            while (!eof()) {
                char c = s.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }

        private char peek() {
            skipWs();
            if (eof()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            return s.charAt(index);
        }

        private char next() {
            char c = peek();
            index++;
            return c;
        }

        private Object parseValue() {
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWs();
            if (peek() == '}') {
                next();
                return map;
            }
            while (true) {
                String key = parseString();
                expect(':');
                map.put(key, parseValue());
                skipWs();
                char c = next();
                if (c == '}') {
                    break;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at " + index);
                }
            }
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWs();
            if (peek() == ']') {
                next();
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = next();
                if (c == ']') {
                    break;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at " + index);
                }
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!eof()) {
                char c = s.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (eof()) {
                        throw new IllegalArgumentException("Bad escape");
                    }
                    char e = s.charAt(index++);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > s.length()) {
                                throw new IllegalArgumentException("Bad unicode escape");
                            }
                            int code = Integer.parseInt(s.substring(index, index + 4), 16);
                            index += 4;
                            sb.append((char) code);
                        }
                        default -> throw new IllegalArgumentException("Bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Object parseNumber() {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            while (!eof() && Character.isDigit(s.charAt(index))) {
                index++;
            }
            boolean isDouble = false;
            if (!eof() && s.charAt(index) == '.') {
                isDouble = true;
                index++;
                while (!eof() && Character.isDigit(s.charAt(index))) {
                    index++;
                }
            }
            if (!eof() && (s.charAt(index) == 'e' || s.charAt(index) == 'E')) {
                isDouble = true;
                index++;
                if (!eof() && (s.charAt(index) == '+' || s.charAt(index) == '-')) {
                    index++;
                }
                while (!eof() && Character.isDigit(s.charAt(index))) {
                    index++;
                }
            }
            String raw = s.substring(start, index);
            if (isDouble) {
                return Double.parseDouble(raw);
            }
            long v = Long.parseLong(raw);
            if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                return (int) v;
            }
            return v;
        }

        private Object parseLiteral(String literal, Object value) {
            if (!s.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected " + literal);
            }
            index += literal.length();
            return value;
        }

        private void expect(char expected) {
            char c = next();
            if (c != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at " + (index - 1));
            }
        }
    }
}
