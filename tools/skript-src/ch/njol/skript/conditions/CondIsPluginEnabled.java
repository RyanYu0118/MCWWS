/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Plugin Enabled")
@Description(value={"Check if a plugin is enabled/disabled on the server.", "Plugin names can be found in the plugin's 'plugin.yml' file or by using the '/plugins' command, they are NOT the name of the plugin's jar file.", "When checking if a plugin is not enabled, this will return true if the plugin is either disabled or not on the server. ", "When checking if a plugin is disabled, this will return true if the plugin is on the server and is disabled."})
@Example.Examples(value={@Example(value="if plugin \"Vault\" is enabled:"), @Example(value="if plugin \"WorldGuard\" is not enabled:"), @Example(value="if plugins \"Essentials\" and \"Vault\" are enabled:"), @Example(value="if plugin \"MyBrokenPlugin\" is disabled:")})
@Since(value={"2.6"})
public class CondIsPluginEnabled
extends Condition {
    private Expression<String> plugins;
    private int pattern;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.plugins = exprs[0];
        this.pattern = matchedPattern;
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.plugins.check(e, plugin -> {
            Plugin p = Bukkit.getPluginManager().getPlugin(plugin);
            switch (this.pattern) {
                case 1: {
                    return p == null || !p.isEnabled();
                }
                case 2: {
                    return p != null && !p.isEnabled();
                }
            }
            return p != null && p.isEnabled();
        });
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        String plural;
        String plugin = this.plugins.isSingle() ? "plugin " : "plugins ";
        String string = plural = this.plugins.isSingle() ? " is" : " are";
        String pattern = this.pattern == 0 ? " enabled" : (this.pattern == 1 ? " not enabled" : " disabled");
        return plugin + this.plugins.toString(e, debug) + plural + pattern;
    }

    static {
        Skript.registerCondition(CondIsPluginEnabled.class, "plugin[s] %strings% (is|are) enabled", "plugin[s] %strings% (is|are)(n't| not) enabled", "plugin[s] %strings% (is|are) disabled");
    }
}

