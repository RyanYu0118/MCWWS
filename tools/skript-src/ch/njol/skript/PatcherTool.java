/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class PatcherTool {
    private final FileSystem source;
    private final FileSystem target;

    public static void main(String ... args) throws IOException, URISyntaxException {
        Path currentFile = Paths.get(PatcherTool.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path oldFile = Paths.get(args[0], new String[0]);
        System.out.println("Patch source: " + String.valueOf(currentFile));
        System.out.println("Old jar: " + String.valueOf(oldFile));
        Path patchedFile = Paths.get("patched-" + String.valueOf(oldFile.getFileName()), new String[0]);
        Files.copy(oldFile, patchedFile, StandardCopyOption.REPLACE_EXISTING);
        HashSet<Option> options = new HashSet<Option>();
        for (int i = 1; i < args.length; ++i) {
            String option = args[i].substring(2).replace('-', '_').toUpperCase(Locale.ENGLISH);
            assert (option != null);
            options.add(Option.valueOf(option));
        }
        try (FileSystem source = FileSystems.newFileSystem(currentFile, (ClassLoader)null);
             FileSystem target = FileSystems.newFileSystem(patchedFile, (ClassLoader)null);){
            assert (source != null);
            assert (target != null);
            new PatcherTool(source, target).patch(options);
        }
        System.out.println("Successfully patched to " + String.valueOf(patchedFile));
    }

    private PatcherTool(FileSystem source, FileSystem target) {
        this.source = source;
        this.target = target;
    }

    private void patch(Set<Option> options) throws IOException {
        if (options.isEmpty()) {
            System.out.println("Patched nothing, as requested. For minimal changes, use --security.");
        }
        if (options.contains((Object)Option.SECURITY)) {
            this.copy("effects.EffMessage", true);
            this.copy("expressions.ExprArgument", true);
            this.copy("expressions.ExprColored", true);
            this.copy("lang.VariableString", true);
            this.copy("util.chat.BungeeConverter", true);
            this.copy("util.chat.ChatCode", true);
            this.copy("util.chat.ChatMessages", true);
            this.copy("util.chat.SkriptChatCode", true);
        }
        if (options.contains((Object)Option.MEMORY_LEAK)) {
            this.copy("SkriptEventHandler", true);
            this.copy("effects.Delay", true);
            this.copy("lang.Trigger", true);
            this.copy("util.AsyncEffect", true);
        }
    }

    private void copy(String className, boolean overwrite) throws IOException {
        String fileName = "/ch/njol/skript/" + className.replace('.', '/') + ".class";
        Path targetFile = this.target.getPath(fileName, new String[0]);
        if (!overwrite && Files.exists(targetFile, new LinkOption[0])) {
            System.out.println("Not patching " + className + ", it already exists.");
            return;
        }
        Path sourceFile = this.source.getPath(fileName, new String[0]);
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static enum Option {
        SECURITY,
        MEMORY_LEAK;

    }
}

