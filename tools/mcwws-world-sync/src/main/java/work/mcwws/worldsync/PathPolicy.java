package work.mcwws.worldsync;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class PathPolicy {
    private PathPolicy() {}

    public static String posix(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\\', '/').trim();
        while (s.startsWith("./")) {
            s = s.substring(2);
        }
        return s;
    }

    public static String relative(Path root, Path file) {
        Path nRoot = root.toAbsolutePath().normalize();
        Path nFile = file.toAbsolutePath().normalize();
        if (!nFile.startsWith(nRoot)) {
            throw new IllegalArgumentException("path escapes server root: " + file);
        }
        return posix(nRoot.relativize(nFile).toString());
    }

    public static boolean allowed(String rel, List<String> prefixes, List<String> skipGlobs) {
        String path = posix(rel);
        if (path.isEmpty() || path.startsWith("/") || path.contains("..")) {
            return false;
        }
        boolean pref = false;
        for (String p : prefixes) {
            if (path.equals(p) || path.startsWith(p.endsWith("/") ? p : p + "/") || path.startsWith(p)) {
                pref = true;
                break;
            }
        }
        if (!pref) {
            return false;
        }
        return !skipped(path, skipGlobs);
    }

    public static boolean skipped(String rel, List<String> skipGlobs) {
        String path = posix(rel);
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        for (String glob : skipGlobs) {
            if (glob == null || glob.isBlank()) {
                continue;
            }
            if (matchesGlob(path, glob) || matchesGlob(name, glob)) {
                return true;
            }
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mv.db")
                || lower.endsWith(".lock")
                || "session.lock".equalsIgnoreCase(name)
                || "uid.dat".equalsIgnoreCase(name);
    }

    /** Very small glob: ** / prefix, * suffix/prefix, exact. */
    static boolean matchesGlob(String path, String glob) {
        String g = posix(glob);
        String p = posix(path);
        if (g.equals("**") || g.equals("**/*")) {
            return true;
        }
        if (g.startsWith("**/")) {
            String rest = g.substring(3);
            if (rest.startsWith("*.")) {
                String ext = rest.substring(1);
                return p.toLowerCase(Locale.ROOT).endsWith(ext.toLowerCase(Locale.ROOT));
            }
            return p.endsWith(rest) || p.contains("/" + rest) || p.equals(rest);
        }
        if (g.startsWith("*.")) {
            return p.toLowerCase(Locale.ROOT).endsWith(g.substring(1).toLowerCase(Locale.ROOT));
        }
        if (g.endsWith("/**")) {
            String prefix = g.substring(0, g.length() - 3);
            if (prefix.startsWith("**/")) {
                prefix = prefix.substring(3);
            }
            return p.equals(prefix) || p.startsWith(prefix.endsWith("/") ? prefix : prefix + "/");
        }
        return p.equals(g);
    }

    public static Path resolveUnder(Path root, String rel) {
        String path = posix(rel);
        if (path.isEmpty() || path.startsWith("/") || path.contains("..")) {
            throw new IllegalArgumentException("unsafe relative path: " + rel);
        }
        Path resolved = root.toAbsolutePath().normalize().resolve(path).normalize();
        if (!resolved.startsWith(root.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("path escapes root: " + rel);
        }
        return resolved;
    }
}
