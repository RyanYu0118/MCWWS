package work.mcwws.worldsync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Files the listen-node holder wants the connect-node standby to pull. */
public final class OutboxStore {
    private final Path root;

    public OutboxStore(Path dataFolder) {
        this.root = dataFolder.resolve("outbox");
    }

    public Path root() {
        return root;
    }

    public void put(String rel, Path source) throws IOException {
        Path dest = PathPolicy.resolveUnder(root, rel);
        Files.createDirectories(dest.getParent());
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path resolve(String rel) {
        return PathPolicy.resolveUnder(root, rel);
    }

    public void forget(String rel) {
        try {
            Files.deleteIfExists(PathPolicy.resolveUnder(root, rel));
        } catch (Exception ignored) {
        }
    }

    public List<String> list() throws IOException {
        List<String> out = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return out;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                out.add(PathPolicy.relative(root, file));
            }
        }
        return out;
    }
}
