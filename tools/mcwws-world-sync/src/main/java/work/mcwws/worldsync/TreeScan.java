package work.mcwws.worldsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Hash every allowed file under the sync prefixes. */
public final class TreeScan {
    private TreeScan() {}

    public static Map<String, String> hash(McwwsWorldSyncPlugin plugin) throws IOException {
        Path root = plugin.serverRoot();
        List<Path> files = new ArrayList<>();
        for (String prefix : plugin.config().prefixes) {
            String rel = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            Path p = PathPolicy.resolveUnder(root, rel);
            if (Files.isRegularFile(p)) {
                if (include(plugin, PathPolicy.relative(root, p))) {
                    files.add(p);
                }
            } else if (Files.isDirectory(p)) {
                try (var walk = Files.walk(p)) {
                    for (Path file : walk.filter(Files::isRegularFile).toList()) {
                        try {
                            String path = PathPolicy.relative(root, file);
                            if (include(plugin, path)) {
                                files.add(file);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }
        plugin.getLogger().info("强制推送将扫描 " + files.size() + " 个文件");
        Map<String, String> hashes = new LinkedHashMap<>();
        int n = 0;
        for (Path file : files) {
            String rel = PathPolicy.relative(root, file);
            hashes.put(rel, StagingStore.sha256(file));
            n++;
            if (n == 1 || n == files.size() || n % 500 == 0) {
                plugin.getLogger().info("已计算校验 " + n + "/" + files.size());
            }
        }
        return hashes;
    }

    static boolean include(McwwsWorldSyncPlugin plugin, String rel) {
        if (!PathPolicy.allowed(rel, plugin.config().prefixes, plugin.config().skipGlobs)) {
            return false;
        }
        String path = PathPolicy.posix(rel).toLowerCase(Locale.ROOT);
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.endsWith(".tmp") || name.contains("_corrupted_")) {
            return false;
        }
        if (path.contains("/logs/") || path.contains("/backup/")) {
            return false;
        }
        return true;
    }
}
