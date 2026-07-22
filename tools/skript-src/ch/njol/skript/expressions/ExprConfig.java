/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.config.Config;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.experiments.ReflectionExperimentSyntax;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Config")
@Description(value={"The Skript config.", "This can be reloaded, or navigated to retrieve options."})
@Example(value="set {_node} to node \"language\" in the skript config\nif text value of {_node} is \"french\":\n\tbroadcast \"Bonjour!\"\n")
@Since(value={"2.10"})
public class ExprConfig
extends SimpleExpression<Config>
implements ReflectionExperimentSyntax {
    @Nullable
    private Config config;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.config = SkriptConfig.getConfig();
        if (this.config == null) {
            Skript.warning("The main config is unavailable here!");
            return false;
        }
        return true;
    }

    protected Config[] get(Event event) {
        if (this.config == null || !this.config.valid()) {
            this.config = SkriptConfig.getConfig();
        }
        if (this.config != null && this.config.valid()) {
            return new Config[]{this.config};
        }
        return new Config[0];
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Config> getReturnType() {
        return Config.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the skript config";
    }

    static {
        Skript.registerExpression(ExprConfig.class, Config.class, ExpressionType.SIMPLE, "[the] [skript] config");
    }
}

