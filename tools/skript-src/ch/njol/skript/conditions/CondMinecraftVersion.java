/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
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
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SimplifiedCondition;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Version;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Running Minecraft")
@Description(value={"Checks if current Minecraft version is given version or newer."})
@Example(value="running minecraft \"1.14\"")
@Since(value={"2.5"})
public class CondMinecraftVersion
extends Condition {
    private Expression<String> version;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.version = exprs[0];
        this.setNegated(parseResult.mark == 1);
        return true;
    }

    @Override
    public boolean check(Event e) {
        String ver = this.version.getSingle(e);
        return ver != null ? Skript.isRunningMinecraft(new Version(ver)) ^ this.isNegated() : false;
    }

    @Override
    public Condition simplify() {
        if (this.version instanceof Literal) {
            return SimplifiedCondition.fromCondition(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "is running minecraft " + this.version.toString(e, debug);
    }

    static {
        Skript.registerCondition(CondMinecraftVersion.class, "running [(1\u00a6below)] minecraft %string%");
    }
}

