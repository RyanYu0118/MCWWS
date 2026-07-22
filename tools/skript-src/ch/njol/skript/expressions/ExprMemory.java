/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Locale;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Free / Max / Total Memory")
@Description(value={"The free, max or total memory of the server in Megabytes."})
@Example(value="while player is online:\n\tsend action bar \"Memory left: %free memory%/%max memory%MB\" to player\n\twait 5 ticks\n")
@Since(value={"2.8.0"})
public class ExprMemory
extends SimpleExpression<Double> {
    private static final double BYTES_IN_MEGABYTES = 1.0E-6;
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private Type type;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.type = parseResult.hasTag("free") ? Type.FREE : (parseResult.hasTag("max") ? Type.MAXIMUM : Type.TOTAL);
        return true;
    }

    protected Double[] get(Event event) {
        double memory = 0.0;
        switch (this.type.ordinal()) {
            case 0: {
                memory = RUNTIME.freeMemory();
                break;
            }
            case 1: {
                memory = RUNTIME.maxMemory();
                break;
            }
            case 2: {
                memory = RUNTIME.totalMemory();
            }
        }
        return CollectionUtils.array(memory * 1.0E-6);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Double> getReturnType() {
        return Double.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.type.name().toLowerCase(Locale.ENGLISH) + " memory";
    }

    static {
        Skript.registerExpression(ExprMemory.class, Double.class, ExpressionType.SIMPLE, "[the] [server] (:free|max:max[imum]|total) (memory|ram)");
    }

    private static enum Type {
        FREE,
        MAXIMUM,
        TOTAL;

    }
}

