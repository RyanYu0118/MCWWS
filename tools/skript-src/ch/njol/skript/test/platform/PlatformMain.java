/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonSyntaxException
 *  org.apache.commons.lang.StringUtils
 */
package ch.njol.skript.test.platform;

import ch.njol.skript.test.platform.Environment;
import ch.njol.skript.test.utils.TestResults;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

public class PlatformMain {
    public static void main(String ... args) throws IOException, InterruptedException {
        TestResults results;
        HashSet jvmArgs;
        String[] envPathStrings;
        System.out.println("Initializing Skript test platform...");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path runnerRoot = Paths.get(args[0], new String[0]);
        Path testsRoot = Paths.get(args[1], new String[0]).toAbsolutePath();
        Path dataRoot = Paths.get(args[2], new String[0]);
        ArrayList<Path> envPaths = new ArrayList<Path>();
        String envsArg = args[3];
        envsArg = envsArg.trim();
        for (String envPath : envPathStrings = envsArg.split(",")) {
            envPaths.add(Paths.get(envPath.trim(), new String[0]));
        }
        boolean devMode = "true".equals(args[4]);
        boolean genDocs = "true".equals(args[5]);
        boolean jUnit = "true".equals(args[6]);
        boolean debug = "true".equals(args[7]);
        String verbosity = args[8].toUpperCase(Locale.ENGLISH);
        long timeout = Long.parseLong(args[9]);
        if (timeout < 0L) {
            timeout = 0L;
        }
        if ((jvmArgs = Sets.newHashSet((Object[])Arrays.copyOfRange(args, 10, args.length))).stream().noneMatch(arg -> arg.contains("-Xmx"))) {
            jvmArgs.add("-Xmx5G");
        }
        ArrayList<Environment> envs = new ArrayList<Environment>();
        for (Path envPath : envPaths) {
            if (Files.isDirectory(envPath, new LinkOption[0])) {
                envs.addAll(Files.walk(envPath, new FileVisitOption[0]).filter(path -> !Files.isDirectory(path, new LinkOption[0])).map(path -> {
                    try {
                        return (Environment)gson.fromJson(Files.readString(path), Environment.class);
                    }
                    catch (JsonSyntaxException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toList());
                continue;
            }
            envs.add((Environment)gson.fromJson(Files.readString(envPath), Environment.class));
        }
        System.out.println("Test environments: " + envs.stream().map(Environment::getName).collect(Collectors.joining(", ")));
        HashSet<String> allTests = new HashSet<String>();
        HashMap<String, List> failures = new HashMap<String, List>();
        boolean docsFailed = false;
        Map<Environment, TestResults> collectedResults = Collections.synchronizedMap(new HashMap());
        envs.sort(Comparator.comparing(Environment::getName));
        for (Environment environment : envs) {
            System.out.println("Starting testing on " + environment.getName());
            environment.initialize(dataRoot, runnerRoot, false);
            results = environment.runTests(runnerRoot, testsRoot, devMode, genDocs, jUnit, debug, verbosity, timeout, jvmArgs);
            if (results == null) {
                if (devMode) {
                    System.exit(0);
                    return;
                }
                System.err.println("The test environment '" + environment.getName() + "' failed to produce test results.");
                System.exit(3);
                return;
            }
            collectedResults.put(environment, results);
        }
        for (Map.Entry entry : collectedResults.entrySet()) {
            results = (TestResults)entry.getValue();
            Environment env = (Environment)entry.getKey();
            docsFailed |= results.docsFailed();
            allTests.addAll(results.getSucceeded());
            allTests.addAll(results.getFailed().keySet());
            for (Map.Entry<String, String> fail : results.getFailed().entrySet()) {
                String error = fail.getValue();
                assert (error != null);
                failures.computeIfAbsent(fail.getKey(), k -> new ArrayList()).add(new TestError(env, error));
            }
        }
        if (docsFailed) {
            System.err.println("Documentation templates not found. Cannot generate docs!");
            System.exit(2);
            return;
        }
        if (genDocs) {
            System.exit(0);
            return;
        }
        List succeeded = allTests.stream().filter(name -> !failures.containsKey(name)).sorted().collect(Collectors.toList());
        ArrayList arrayList = new ArrayList(failures.keySet());
        Collections.sort(arrayList);
        StringBuilder output = new StringBuilder(String.format("%s Results %s%n", StringUtils.repeat((String)"-", (int)25), StringUtils.repeat((String)"-", (int)25)));
        output.append("\nTested environments: ").append(envs.stream().map(Environment::getName).collect(Collectors.joining(", ")));
        output.append("\nSucceeded:\n  ").append(String.join((CharSequence)(jUnit ? "\n  " : ", "), succeeded));
        if (!arrayList.isEmpty()) {
            output.append("\nFailed:");
            for (String failed : arrayList) {
                List errors = (List)failures.get(failed);
                output.append("\n  ").append(failed).append(" (on ").append(errors.size()).append(" environment").append(errors.size() == 1 ? "" : "s").append(")");
                for (TestError error : errors) {
                    output.append("\n    ").append(error.message()).append(" (on ").append(error.environment().getName()).append(")");
                }
            }
            output.append(String.format("%n%n%s", StringUtils.repeat((String)"-", (int)60)));
            System.err.print(output);
            System.exit(arrayList.size());
            return;
        }
        output.append(String.format("%n%n%s", StringUtils.repeat((String)"-", (int)60)));
        System.out.print(output);
    }

    private record TestError(Environment environment, String message) {
    }
}

