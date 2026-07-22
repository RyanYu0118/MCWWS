/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.Nullable;

public class TestMode {
    private static final String ROOT = "skript.testing.";
    public static final boolean ENABLED = "true".equals(System.getProperty("skript.testing.enabled"));
    public static final Path TEST_DIR = ENABLED ? Paths.get(System.getProperty("skript.testing.dir"), new String[0]) : null;
    public static final boolean DEV_MODE = ENABLED && "true".equals(System.getProperty("skript.testing.devMode"));
    public static final boolean GEN_DOCS = "true".equals(System.getProperty("skript.testing.genDocs"));
    @Nullable
    public static final String VERBOSITY = ENABLED ? System.getProperty("skript.testing.verbosity") : null;
    public static final Path RESULTS_FILE = ENABLED ? Paths.get(System.getProperty("skript.testing.results"), new String[0]) : null;
    public static final boolean JUNIT = "true".equals(System.getProperty("skript.testing.junit"));
    @Nullable
    public static File lastTestFile;
    public static boolean docsFailed;
}

