/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.effects;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.util.FileUtils;
import ch.njol.util.Kleenean;
import ch.njol.util.OpenCloseable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Enable/Disable/Unload/Reload Script")
@Description(value={"Enables, disables, unloads, or reloads a script.\n\nDisabling a script unloads it and prepends - to its name so it will not be loaded the next time the server restarts.\nIf the script reflection experiment is enabled: unloading a script terminates it and removes it from memory, but does not alter the file."})
@Example.Examples(value={@Example(value="reload script \"test\""), @Example(value="enable script file \"testing\""), @Example(value="unload script file \"script.sk\""), @Example(value="set {_script} to the script \"MyScript.sk\"\nreload {_script}\n")})
@Since(value={"2.4, 2.10 (unloading)"})
public class EffScriptFile
extends Effect {
    private static final int ENABLE = 1;
    private static final int RELOAD = 2;
    private static final int DISABLE = 3;
    private static final int UNLOAD = 4;
    private int mark;
    private @UnknownNullability Expression<String> scriptNameExpression;
    private @UnknownNullability Expression<Script> scriptExpression;
    private boolean scripts;
    private boolean hasReflection;
    private boolean printErrors;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.mark = parseResult.mark;
        this.printErrors = parseResult.hasTag("print");
        switch (matchedPattern) {
            case 0: 
            case 1: {
                this.scriptNameExpression = exprs[0];
                break;
            }
            case 2: {
                this.scriptExpression = exprs[0];
                this.scripts = true;
            }
        }
        this.hasReflection = this.getParser().hasExperiment(Feature.SCRIPT_REFLECTION);
        return true;
    }

    @Override
    protected void execute(Event event) {
        HashSet<CommandSender> recipients = new HashSet<CommandSender>();
        if (this.printErrors) {
            recipients.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> player.hasPermission("skript.reloadnotify")).collect(Collectors.toSet()));
        }
        RedirectingLogHandler logHandler = new RedirectingLogHandler(recipients, "").start();
        if (this.scripts) {
            for (Script script : this.scriptExpression.getArray(event)) {
                @Nullable File file = script.getConfig().getFile();
                this.handle(file, script.getConfig().getFileName(), logHandler);
            }
        } else {
            String name = this.scriptNameExpression.getSingle(event);
            if (name != null) {
                this.handle(ScriptLoader.getScriptFromName(name), name, logHandler);
            }
        }
        logHandler.close();
    }

    private void handle(@Nullable File scriptFile, @Nullable String name, OpenCloseable openCloseable) {
        if (scriptFile == null || !scriptFile.exists()) {
            return;
        }
        if (name == null) {
            name = scriptFile.getName();
        }
        FileFilter filter = ScriptLoader.getDisabledScriptsFilter();
        switch (this.mark) {
            case 1: {
                if (ScriptLoader.getLoadedScripts().contains(ScriptLoader.getScript(scriptFile))) {
                    return;
                }
                if (filter.accept(scriptFile)) {
                    try {
                        scriptFile = FileUtils.move(scriptFile, new File(scriptFile.getParentFile(), scriptFile.getName().substring(ScriptLoader.DISABLED_SCRIPT_PREFIX_LENGTH)), false);
                    }
                    catch (IOException ex) {
                        Skript.exception((Throwable)ex, "Error while enabling script file: " + name);
                        return;
                    }
                }
                ScriptLoader.loadScripts(scriptFile, openCloseable);
                break;
            }
            case 2: {
                if (filter.accept(scriptFile)) {
                    return;
                }
                this.unloadScripts(scriptFile);
                ScriptLoader.loadScripts(scriptFile, openCloseable);
                break;
            }
            case 4: {
                if (this.hasReflection) {
                    if (!ScriptLoader.getLoadedScriptsFilter().accept(scriptFile)) {
                        return;
                    }
                    this.unloadScripts(scriptFile);
                    break;
                }
            }
            case 3: {
                if (filter.accept(scriptFile)) {
                    return;
                }
                this.unloadScripts(scriptFile);
                try {
                    FileUtils.move(scriptFile, new File(scriptFile.getParentFile(), "-" + scriptFile.getName()), false);
                    break;
                }
                catch (IOException ex) {
                    Skript.exception((Throwable)ex, "Error while disabling script file: " + name);
                    return;
                }
            }
            default: {
                assert (false);
                break;
            }
        }
    }

    private void unloadScripts(File file) {
        Set<Script> loaded = ScriptLoader.getLoadedScripts();
        if (file.isDirectory()) {
            Set<Script> scripts = ScriptLoader.getScripts(file);
            if (scripts.isEmpty()) {
                return;
            }
            scripts.retainAll(loaded);
            ScriptLoader.unloadScripts(scripts);
        } else {
            Script script = ScriptLoader.getScript(file);
            if (!loaded.contains(script)) {
                return;
            }
            if (script != null) {
                ScriptLoader.unloadScript(script);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String start = (switch (this.mark) {
            case 1 -> "enable";
            case 3 -> "disable";
            case 2 -> "reload";
            default -> "unload";
        }) + " ";
        if (this.scripts) {
            return start + this.scriptExpression.toString(event, debug);
        }
        return start + "script file " + this.scriptNameExpression.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffScriptFile.class, "(1:(enable|load)|2:reload|3:disable|4:unload) script [file|named] %string% [print:with errors]", "(1:(enable|load)|2:reload|3:disable|4:unload) skript file %string% [print:with errors]", "(1:(enable|load)|2:reload|3:disable|4:unload) %scripts% [print:with errors]");
    }
}

