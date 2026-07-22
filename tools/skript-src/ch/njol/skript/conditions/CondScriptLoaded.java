/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import java.io.File;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.Script;

@Name(value="Is Script Loaded")
@Description(value={"Check if the current script, or another script, is currently loaded."})
@Example.Examples(value={@Example(value="script is loaded"), @Example(value="script \"example.sk\" is loaded")})
@Since(value={"2.2-dev31"})
public class CondScriptLoaded
extends Condition {
    @Nullable
    private Expression<String> scripts;
    @Nullable
    private Script currentScript;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.scripts = exprs[0];
        ParserInstance parser = this.getParser();
        if (this.scripts == null) {
            if (parser.isActive()) {
                this.currentScript = parser.getCurrentScript();
            } else {
                Skript.error("The condition 'script loaded' requires a script name argument when used outside of script files");
                return false;
            }
        }
        this.setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.scripts == null) {
            return ScriptLoader.getLoadedScripts().contains(this.currentScript) ^ this.isNegated();
        }
        return this.scripts.check(event, scriptName -> {
            File scriptFile = ScriptLoader.getScriptFromName(scriptName);
            return scriptFile != null && ScriptLoader.getLoadedScripts().contains(ScriptLoader.getScript(scriptFile));
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String scriptName;
        String string = this.scripts == null ? "script" : (scriptName = this.scripts.isSingle() ? "script" : "scripts " + this.scripts.toString(event, debug));
        if (this.scripts == null || this.scripts.isSingle()) {
            return scriptName + (this.isNegated() ? " isn't" : " is") + " loaded";
        }
        return scriptName + (this.isNegated() ? " aren't" : " are") + " loaded";
    }

    static {
        Skript.registerCondition(CondScriptLoaded.class, "script[s] [%-strings%] (is|are) loaded", "script[s] [%-strings%] (isn't|is not|aren't|are not) loaded");
    }
}

