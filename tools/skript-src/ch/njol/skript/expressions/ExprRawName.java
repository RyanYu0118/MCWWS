/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.Arrays;
import java.util.Collection;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Raw Name")
@Description(value={"The raw Minecraft material name of the given item. Note that this is not guaranteed to give same results on all servers."})
@Example(value="raw name of tool of player")
@Since(value={"unknown (2.2)"})
public class ExprRawName
extends SimpleExpression<String> {
    private Expression<ItemType> types;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.types = exprs[0];
        return true;
    }

    @Nullable
    protected String[] get(Event e) {
        return (String[])Arrays.stream(this.types.getAll(e)).map(ItemType::getRawNames).flatMap(Collection::stream).toArray(String[]::new);
    }

    @Override
    public boolean isSingle() {
        return this.types.isSingle();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "minecraft name of " + this.types.toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprRawName.class, String.class, ExpressionType.SIMPLE, "(raw|minecraft|vanilla) name[s] of %itemtypes%");
    }
}

