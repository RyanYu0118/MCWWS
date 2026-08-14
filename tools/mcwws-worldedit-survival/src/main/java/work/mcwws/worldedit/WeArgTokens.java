package work.mcwws.worldedit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 拆创世神参数：开关（{@code -a}）与位置参数分开。{@code -m} 会吞掉下一个掩码参数。 */
final class WeArgTokens {

    final List<String> positional = new ArrayList<>();
    private final Set<Character> flags = new HashSet<>();
    final String maskInput;

    private WeArgTokens(String maskInput) {
        this.maskInput = maskInput;
    }

    static WeArgTokens parse(String[] args) {
        String mask = null;
        WeArgTokens tokens = new WeArgTokens(null);
        if (args == null) {
            return tokens;
        }
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token == null || token.isBlank()) {
                continue;
            }
            if (isFlagToken(token)) {
                if (token.equals("-m") || token.startsWith("-m") && token.length() > 2) {
                    if (token.length() > 2 && token.charAt(1) == 'm') {
                        mask = token.substring(2);
                    } else if (i + 1 < args.length) {
                        mask = args[++i];
                    }
                    continue;
                }
                for (int j = 1; j < token.length(); j++) {
                    tokens.flags.add(Character.toLowerCase(token.charAt(j)));
                }
                continue;
            }
            tokens.positional.add(token);
        }
        return new WeArgTokens(mask).copyPositionalAndFlags(tokens);
    }

    private WeArgTokens copyPositionalAndFlags(WeArgTokens other) {
        this.positional.addAll(other.positional);
        this.flags.addAll(other.flags);
        return this;
    }

    boolean has(char flag) {
        return flags.contains(Character.toLowerCase(flag));
    }

    int size() {
        return positional.size();
    }

    String get(int index) {
        return index >= 0 && index < positional.size() ? positional.get(index) : null;
    }

    String get(int index, String fallback) {
        String value = get(index);
        return value == null || value.isBlank() ? fallback : value;
    }

    String joinFrom(int start) {
        if (start >= positional.size()) {
            return "";
        }
        return String.join(" ", positional.subList(start, positional.size()));
    }

    static boolean isInt(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    static boolean isDouble(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    static double[] parseRadii(String token, int expectedMax) throws com.sk89q.worldedit.extension.input.InputParseException {
        if (token == null || token.isBlank()) {
            throw new com.sk89q.worldedit.extension.input.InputParseException("缺少半径");
        }
        String[] parts = token.split(",");
        if (parts.length < 1 || parts.length > expectedMax) {
            throw new com.sk89q.worldedit.extension.input.InputParseException("无效半径: " + token);
        }
        if (expectedMax == 3 && parts.length == 2) {
            throw new com.sk89q.worldedit.extension.input.InputParseException("球体半径应为 1 或 3 个数");
        }
        double[] radii = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                radii[i] = Double.parseDouble(parts[i].trim());
            }
        } catch (NumberFormatException ex) {
            throw new com.sk89q.worldedit.extension.input.InputParseException("无效半径: " + token);
        }
        return radii;
    }

    private static boolean isFlagToken(String token) {
        if (!token.startsWith("-") || token.length() < 2 || token.contains(",")) {
            return false;
        }
        char second = token.charAt(1);
        if (second == '-' || Character.isDigit(second)) {
            return false;
        }
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.equals("-north") || lower.equals("-south") || lower.equals("-east") || lower.equals("-west")) {
            return false;
        }
        return true;
    }
}
