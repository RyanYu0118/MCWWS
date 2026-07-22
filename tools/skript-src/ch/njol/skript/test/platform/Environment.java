/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.platform;

import ch.njol.skript.test.utils.TestResults;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.jetbrains.annotations.Nullable;

public class Environment {
    private static final Gson gson = new Gson();
    private final String name;
    private final List<Resource> resources;
    @Nullable
    private final List<Resource> downloads;
    @Nullable
    private final List<PaperResource> paperDownloads;
    private final String skriptTarget;
    private final String[] commandLine;

    public Environment(String name, List<Resource> resources, @Nullable List<Resource> downloads, @Nullable List<PaperResource> paperDownloads, String skriptTarget, String ... commandLine) {
        this.name = name;
        this.resources = resources;
        this.downloads = downloads;
        this.paperDownloads = paperDownloads;
        this.skriptTarget = skriptTarget;
        this.commandLine = commandLine;
    }

    public String getName() {
        return this.name;
    }

    public void initialize(Path dataRoot, Path runnerRoot, boolean remake) throws IOException {
        Path env = runnerRoot.resolve(this.name);
        boolean onlyCopySkript = Files.exists(env, new LinkOption[0]) && !remake;
        Path skript = env.resolve(this.skriptTarget);
        Files.createDirectories(skript.getParent(), new FileAttribute[0]);
        try {
            Files.copy(new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toPath(), skript, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (URISyntaxException e) {
            throw new AssertionError((Object)e);
        }
        if (onlyCopySkript) {
            return;
        }
        for (Resource resource : this.resources) {
            final Path source = dataRoot.resolve(resource.getSource());
            final Path target = env.resolve(resource.getTarget());
            if (Files.isDirectory(source, new LinkOption[0])) {
                Files.walkFileTree(source, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path relative = source.relativize(dir);
                        Path dest = target.resolve(relative);
                        Files.createDirectories(dest, new FileAttribute[0]);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relative = source.relativize(file);
                        Path dest = target.resolve(relative);
                        Files.createDirectories(dest.getParent(), new FileAttribute[0]);
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
                continue;
            }
            Files.createDirectories(target.getParent(), new FileAttribute[0]);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        ArrayList<Resource> downloads = new ArrayList<Resource>();
        if (this.downloads != null) {
            downloads.addAll(this.downloads);
        }
        if (this.paperDownloads != null) {
            downloads.addAll(this.paperDownloads);
        }
        for (Resource resource : downloads) {
            assert (resource != null);
            String source = resource.getSource();
            URL url = new URL(source);
            Path target = env.resolve(resource.getTarget());
            Files.createDirectories(target.getParent(), new FileAttribute[0]);
            InputStream is = url.openStream();
            try {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
            finally {
                if (is == null) continue;
                is.close();
            }
        }
    }

    @Nullable
    public TestResults runTests(Path runnerRoot, Path testsRoot, boolean devMode, boolean genDocs, boolean jUnit, boolean debug, String verbosity, long timeout, Set<String> jvmArgs) throws IOException, InterruptedException {
        int code;
        Path env = runnerRoot.resolve(this.name);
        Path resultsPath = env.resolve("test_results.json");
        Files.deleteIfExists(resultsPath);
        ArrayList<String> args = new ArrayList<String>();
        args.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        args.add("-ea");
        args.add("-Dskript.testing.enabled=true");
        args.add("-Dskript.testing.dir=" + String.valueOf(testsRoot));
        args.add("-Dskript.testing.devMode=" + devMode);
        args.add("-Dskript.testing.genDocs=" + genDocs);
        args.add("-Dskript.testing.junit=" + jUnit);
        if (!verbosity.equalsIgnoreCase("null")) {
            args.add("-Dskript.testing.verbosity=" + verbosity);
        }
        if (genDocs) {
            args.add("-Dskript.forceregisterhooks=true");
        }
        args.add("-Dskript.testing.results=test_results.json");
        args.add("-Ddisable.watchdog=true");
        if (debug) {
            args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000");
        }
        args.add("-Duser.language=en");
        args.add("-Duser.country=US");
        args.addAll(jvmArgs);
        args.addAll(Arrays.asList(this.commandLine));
        final Process process = new ProcessBuilder(args).directory(env.toFile()).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).redirectInput(ProcessBuilder.Redirect.INHERIT).start();
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
        if (!devMode && timeout > 0L) {
            new Timer("runner watchdog", true).schedule(new TimerTask(this){

                @Override
                public void run() {
                    if (process.isAlive()) {
                        System.err.println("Test environment is taking too long, failing...");
                        System.exit(1);
                    }
                }
            }, timeout);
        }
        if ((code = process.waitFor()) != 0) {
            throw new IOException("environment returned with code " + code);
        }
        if (!Files.exists(resultsPath, new LinkOption[0])) {
            return null;
        }
        TestResults results = (TestResults)new Gson().fromJson(new String(Files.readAllBytes(resultsPath)), TestResults.class);
        assert (results != null);
        return results;
    }

    public static class Resource {
        private final String source;
        private final String target;

        public Resource(String url, String target) {
            this.source = url;
            this.target = target;
        }

        public String getSource() {
            return this.source;
        }

        public String getTarget() {
            return this.target;
        }
    }

    public static class PaperResource
    extends Resource {
        private final String version;
        @Nullable
        private transient String source;

        public PaperResource(String version, String target) {
            super(null, target);
            this.version = version;
        }

        @Override
        public String getSource() {
            try {
                this.generateSource();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (this.source == null) {
                throw new IllegalStateException();
            }
            return this.source;
        }

        private void generateSource() throws IOException, InterruptedException {
            JsonObject buildObject;
            if (this.source != null) {
                return;
            }
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest buildRequest = HttpRequest.newBuilder().uri(URI.create("https://fill.papermc.io/v3/projects/paper/versions/" + this.version + "/builds/latest")).header("User-Agent", "SkriptLang/Skript/{@version} (admin@skriptlang.org)").GET().build();
            HttpResponse<InputStream> buildResponse = client.send(buildRequest, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStreamReader reader = new InputStreamReader(buildResponse.body(), StandardCharsets.UTF_8);){
                buildObject = (JsonObject)gson.fromJson((Reader)reader, JsonObject.class);
            }
            String downloadURL = buildObject.getAsJsonObject("downloads").getAsJsonObject("server:default").get("url").getAsString();
            assert (downloadURL != null && !downloadURL.isEmpty());
            this.source = downloadURL;
        }
    }
}

