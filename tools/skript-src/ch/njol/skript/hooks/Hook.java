/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 */
package ch.njol.skript.hooks;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Documentation;
import ch.njol.skript.localization.ArgsMessage;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public abstract class Hook<P extends Plugin> {
    private static final ArgsMessage m_hooked = new ArgsMessage("hooks.hooked");
    private static final ArgsMessage m_hook_error = new ArgsMessage("hooks.error");
    public final P plugin;

    public final P getPlugin() {
        return this.plugin;
    }

    public Hook() throws IOException {
        Plugin p = Bukkit.getPluginManager().getPlugin(this.getName());
        this.plugin = p;
        if (p == null) {
            if (Documentation.canGenerateUnsafeDocs()) {
                this.loadClasses();
                if (Skript.logHigh()) {
                    Skript.info(m_hooked.toString(this.getName()));
                }
            }
            return;
        }
        if (!this.init()) {
            Skript.error(m_hook_error.toString(p.getName()));
            return;
        }
        this.loadClasses();
        if (Skript.logHigh()) {
            Skript.info(m_hooked.toString(p.getName()));
        }
    }

    protected void loadClasses() throws IOException {
        Skript.getAddonInstance().loadClasses(this.getClass().getPackage().getName(), new String[0]);
    }

    public abstract String getName();

    protected boolean init() {
        return true;
    }
}

