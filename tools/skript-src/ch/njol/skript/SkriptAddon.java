/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.util.Utils;
import ch.njol.skript.util.Version;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.localization.Localizer;
import org.skriptlang.skript.registration.SyntaxRegistry;
import org.skriptlang.skript.util.Registry;

@Deprecated(since="2.14", forRemoval=true)
public final class SkriptAddon
implements org.skriptlang.skript.addon.SkriptAddon {
    public final JavaPlugin plugin;
    public final Version version;
    private final String name;
    private final org.skriptlang.skript.addon.SkriptAddon addon;
    @Nullable
    private File file;

    SkriptAddon(JavaPlugin plugin) {
        this(plugin, Skript.instance().registerAddon(plugin.getClass(), plugin.getName()));
    }

    SkriptAddon(JavaPlugin plugin, org.skriptlang.skript.addon.SkriptAddon addon) {
        Version version;
        this.addon = addon;
        this.plugin = plugin;
        this.name = plugin.getName();
        try {
            version = new Version(plugin.getDescription().getVersion());
        }
        catch (IllegalArgumentException e) {
            Matcher m = Pattern.compile("(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?").matcher(plugin.getDescription().getVersion());
            if (!m.find()) {
                throw new IllegalArgumentException("The version of the plugin " + this.name + " does not contain any numbers: " + plugin.getDescription().getVersion());
            }
            version = new Version(Utils.parseInt(m.group(1)), m.group(2) == null ? 0 : Utils.parseInt(m.group(2)), m.group(3) == null ? 0 : Utils.parseInt(m.group(3)));
            Skript.warning("The plugin " + this.name + " uses a non-standard version syntax: '" + plugin.getDescription().getVersion() + "'. Skript will use " + String.valueOf(version) + " instead.");
        }
        this.version = version;
    }

    public final String toString() {
        return this.getName();
    }

    public String getName() {
        return this.name;
    }

    public SkriptAddon loadClasses(String basePackage, String ... subPackages) throws IOException {
        Utils.getClasses((Plugin)this.plugin, basePackage, subPackages);
        return this;
    }

    public SkriptAddon setLanguageFileDirectory(String directory) {
        this.localizer().setSourceDirectories(directory, this.plugin.getDataFolder().getAbsolutePath() + directory);
        return this;
    }

    @Nullable
    public String getLanguageFileDirectory() {
        return this.localizer().languageFileDirectory();
    }

    @Nullable
    public File getFile() {
        if (this.file == null) {
            this.file = Utils.getFile((Plugin)this.plugin);
        }
        return this.file;
    }

    static SkriptAddon fromModern(org.skriptlang.skript.addon.SkriptAddon addon) {
        return new SkriptAddon(JavaPlugin.getProvidingPlugin(addon.source()), addon);
    }

    @Override
    public Class<?> source() {
        return this.addon.source();
    }

    @Override
    public String name() {
        return this.addon.name();
    }

    @Override
    public <R extends Registry<?>> void storeRegistry(Class<R> registryClass, R registry) {
        this.addon.storeRegistry(registryClass, registry);
    }

    @Override
    public void removeRegistry(Class<? extends Registry<?>> registryClass) {
        this.addon.removeRegistry(registryClass);
    }

    @Override
    public boolean hasRegistry(Class<? extends Registry<?>> registryClass) {
        return this.addon.hasRegistry(registryClass);
    }

    @Override
    public <R extends Registry<?>> R registry(Class<R> registryClass) {
        return this.addon.registry(registryClass);
    }

    @Override
    public <R extends Registry<?>> R registry(Class<R> registryClass, Supplier<R> putIfAbsent) {
        return this.addon.registry(registryClass, putIfAbsent);
    }

    @Override
    public SyntaxRegistry syntaxRegistry() {
        return this.addon.syntaxRegistry();
    }

    @Override
    public Localizer localizer() {
        return this.addon.localizer();
    }
}

