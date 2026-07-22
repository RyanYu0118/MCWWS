/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
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
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Worlds")
@Description(value={"All worlds of the server, useful for looping."})
@Example(value="loop all worlds:\n\tbroadcast \"You're in %loop-world%\" to loop-world\n")
@Since(value={"1.0"})
public class ExprWorlds
extends SimpleExpression<World> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends World> getReturnType() {
        return World.class;
    }

    @Nullable
    protected World[] get(Event e) {
        return Bukkit.getWorlds().toArray(new World[0]);
    }

    @Override
    @Nullable
    public Iterator<World> iterator(Event e) {
        return Bukkit.getWorlds().iterator();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "worlds";
    }

    static {
        Skript.registerExpression(ExprWorlds.class, World.class, ExpressionType.SIMPLE, "[(all [[of] the]|the)] worlds");
    }
}

