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
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Feature;
import ch.njol.util.Kleenean;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.experiment.ExperimentData;
import org.skriptlang.skript.lang.experiment.SimpleExperimentalSyntax;
import org.skriptlang.skript.lang.script.Script;

@Name(value="All Scripts")
@Description(value={"Returns all of the scripts, or just the enabled or disabled ones."})
@Example(value="command /scripts:\n\ttrigger:\n\t\tsend \"All Scripts: %scripts%\" to player\n\t\tsend \"Loaded Scripts: %enabled scripts%\" to player\n\t\tsend \"Unloaded Scripts: %disabled scripts%\" to player\n")
@Since(value={"2.5"})
public class ExprScriptsOld
extends SimpleExpression<String>
implements SimpleExperimentalSyntax {
    private static final ExperimentData EXPERIMENT_DATA = ExperimentData.builder().disallowed(Feature.SCRIPT_REFLECTION).build();
    public static final Path SCRIPTS_PATH;
    private boolean includeEnabled;
    private boolean includeDisabled;
    private boolean noPaths;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.includeEnabled = matchedPattern <= 1;
        this.includeDisabled = matchedPattern != 1;
        this.noPaths = parseResult.mark == 1;
        return true;
    }

    @Override
    public ExperimentData getExperimentData() {
        return EXPERIMENT_DATA;
    }

    protected String[] get(Event event) {
        ArrayList<Path> scripts = new ArrayList<Path>();
        if (this.includeEnabled) {
            for (Script script : ScriptLoader.getLoadedScripts()) {
                scripts.add(script.getConfig().getPath());
            }
        }
        if (this.includeDisabled) {
            scripts.addAll(ScriptLoader.getDisabledScripts().stream().map(File::toPath).collect(Collectors.toList()));
        }
        return this.formatPaths(scripts);
    }

    private String[] formatPaths(List<Path> paths) {
        return (String[])paths.stream().map(path -> {
            if (this.noPaths) {
                return path.getFileName().toString();
            }
            return SCRIPTS_PATH.relativize(path.toAbsolutePath()).toString();
        }).toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        Object text = !this.includeEnabled ? "all disabled scripts" : (!this.includeDisabled ? "all enabled scripts" : "all scripts");
        if (this.noPaths) {
            text = (String)text + " without paths";
        }
        return text;
    }

    static {
        Skript.registerExpression(ExprScriptsOld.class, String.class, ExpressionType.SIMPLE, "[all [of the]|the] scripts [1:without ([subdirectory] paths|parents)]", "[all [of the]|the] (enabled|loaded) scripts [1:without ([subdirectory] paths|parents)]", "[all [of the]|the] (disabled|unloaded) scripts [1:without ([subdirectory] paths|parents)]");
        SCRIPTS_PATH = Skript.getInstance().getScriptsFolder().getAbsoluteFile().toPath();
    }
}

