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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="World from Name")
@Description(value={"Returns the world from a string."})
@Example.Examples(value={@Example(value="world named {game::world-name}"), @Example(value="the world \"world\"")})
@Since(value={"2.6.1"})
public class ExprWorldFromName
extends SimpleExpression<World> {
    private Expression<String> worldName;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.worldName = exprs[0];
        return true;
    }

    @Nullable
    protected World[] get(Event e) {
        String worldName = this.worldName.getSingle(e);
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            return null;
        }
        return new World[]{world};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<World> getReturnType() {
        return World.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the world with name " + this.worldName.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprWorldFromName.class, World.class, ExpressionType.SIMPLE, "[the] world [(named|with name)] %string%");
    }
}

