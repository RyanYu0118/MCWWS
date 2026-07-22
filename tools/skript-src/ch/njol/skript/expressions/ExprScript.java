/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Script")
@Description(value={"The current script, or a script from its (file) name.", "If the script is enabled or disabled (or reloaded) this reference will become invalid.", "Therefore, it is recommended to obtain a script reference <em>when needed</em>."})
@Example.Examples(value={@Example(value="on script load:\n\tbroadcast \"Loaded %the current script%\"\n"), @Example(value="on script load:\n\tset {running::%script%} to true\n"), @Example(value="on script unload:\n\tset {running::%script%} to false\n"), @Example(value="set {script} to the script named \"weather.sk\""), @Example(value="loop the scripts in directory \"quests/\":\n\tenable loop-value\n")})
@Since(value={"2.0"})
public class ExprScript
extends SimpleExpression<Script> {
    @Nullable
    private Script script;
    @Nullable
    private Expression<String> name;
    private boolean isDirectory;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        boolean bl = this.isDirectory = matchedPattern == 2;
        if (matchedPattern == 0) {
            ParserInstance parser = this.getParser();
            if (!parser.isActive()) {
                Skript.error("'the current script' can only be used in a script.");
                return false;
            }
            this.script = parser.getCurrentScript();
        } else {
            this.name = exprs[0];
        }
        return true;
    }

    protected Script[] get(Event event) {
        if (this.script != null) {
            return new Script[]{this.script};
        }
        assert (this.name != null);
        if (this.isDirectory) {
            @Nullable String string = this.name.getSingle(event);
            if (string == null) {
                return new Script[0];
            }
            File folder = new File(Skript.getInstance().getScriptsFolder(), string);
            ArrayList<Script> scripts = new ArrayList<Script>();
            if (!folder.isDirectory()) {
                return new Script[0];
            }
            this.getScripts(folder, scripts);
            return scripts.toArray(new Script[0]);
        }
        return (Script[])this.name.stream(event).map(ScriptLoader::getScriptFromName).map(ExprScript::getHandle).filter(Objects::nonNull).toArray(Script[]::new);
    }

    private void getScripts(File folder, List<Script> scripts) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        FileFilter loaded = ScriptLoader.getLoadedScriptsFilter();
        FileFilter disabled = ScriptLoader.getDisabledScriptsFilter();
        FileFilter filter = f -> loaded.accept(f) || disabled.accept(f);
        for (File file : files) {
            Script handle;
            if (file.isDirectory()) {
                this.getScripts(file, scripts);
                continue;
            }
            if (!filter.accept(file) || (handle = ExprScript.getHandle(file)) == null) continue;
            scripts.add(handle);
        }
    }

    @Override
    public boolean isSingle() {
        return this.script != null || this.name != null && this.name.isSingle() && !this.isDirectory;
    }

    @Override
    public Class<? extends Script> getReturnType() {
        return Script.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.script != null) {
            return "the current script";
        }
        assert (this.name != null);
        if (this.isDirectory) {
            return "the scripts in directory " + this.name.toString(event, debug);
        }
        if (this.name.isSingle()) {
            return "the script named " + this.name.toString(event, debug);
        }
        return "the scripts named " + this.name.toString(event, debug);
    }

    @Nullable
    static Script getHandle(@Nullable File file) {
        if (file == null || file.isDirectory()) {
            return null;
        }
        Script script = ScriptLoader.getScript(file);
        if (script != null) {
            return script;
        }
        return ScriptLoader.createDummyScript(file.getName(), file);
    }

    static {
        Skript.registerExpression(ExprScript.class, Script.class, ExpressionType.SIMPLE, "[the] [current] script", "[the] script[s] [named] %strings%", "[the] scripts in [directory|folder] %string%");
    }
}

