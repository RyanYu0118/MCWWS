package work.mcwws.worldsync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class StagingStore {
    private final Path staging;
    private final Path serverRoot;
    private final Path applyFlag;
    private final Logger log;

    public StagingStore(Path dataFolder, Path serverRoot, Logger log) {
        this.staging = dataFolder.resolve("staging");
        this.serverRoot = serverRoot;
        this.applyFlag = dataFolder.resolve("apply-on-boot");
        this.log = log;
    }

    public Path stagingRoot() {
        return staging;
    }

    public Path applyFlag() {
        return applyFlag;
    }

    public void writeStaging(String rel, byte[] data) throws IOException {
        Path dest = PathPolicy.resolveUnder(staging, rel);
        Files.createDirectories(dest.getParent());
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        Files.write(tmp, data);
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public void writeStaging(String rel, InputStream in, long expectedSize) throws IOException {
        Path dest = PathPolicy.resolveUnder(staging, rel);
        Files.createDirectories(dest.getParent());
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }
        if (expectedSize >= 0 && Files.size(tmp) != expectedSize) {
            Files.deleteIfExists(tmp);
            throw new IOException("size mismatch for " + rel);
        }
        try {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void markApplyOnBoot() throws IOException {
        Files.createDirectories(applyFlag.getParent());
        Files.writeString(applyFlag, "1\n");
    }

    public boolean hasApplyFlag() {
        return Files.isRegularFile(applyFlag);
    }

    /**
     * Copy staging onto the live server tree. Must run before worlds load ({@code onLoad}).
     */
    public int applyToLive(SyncConfig cfg) throws IOException {
        if (!Files.isDirectory(staging)) {
            return 0;
        }
        int n = 0;
        try (Stream<Path> walk = Files.walk(staging)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                String rel = PathPolicy.relative(staging, file);
                if (rel.endsWith(".part")) {
                    continue;
                }
                if (!PathPolicy.allowed(rel, cfg.prefixes, cfg.skipGlobs)) {
                    log.warning("skip apply (policy): " + rel);
                    continue;
                }
                Path dest = PathPolicy.resolveUnder(serverRoot, rel);
                Files.createDirectories(dest.getParent());
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                n++;
            }
        }
        Files.deleteIfExists(applyFlag);
        log.info("已将 staging 应用到世界目录，文件数 " + n);
        return n;
    }

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) >= 0) {
                    md.update(buf, 0, r);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
