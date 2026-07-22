/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.util.CachedServerIcon
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.EffLoadServerIcon;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.Nullable;

@Name(value="Last Loaded Server Icon")
@Description(value={"Returns the last loaded server icon with the <a href='#EffLoadServerIcon'>load server icon</a> effect."})
@Example(value="set {server-icon} to the last loaded server icon")
@Since(value={"2.3"})
public class ExprLastLoadedServerIcon
extends SimpleExpression<CachedServerIcon> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Nullable
    public CachedServerIcon[] get(Event e) {
        return CollectionUtils.array(EffLoadServerIcon.lastLoaded);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends CachedServerIcon> getReturnType() {
        return CachedServerIcon.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "the last loaded server icon";
    }

    static {
        Skript.registerExpression(ExprLastLoadedServerIcon.class, CachedServerIcon.class, ExpressionType.SIMPLE, "[the] [last[ly]] loaded server icon");
    }
}

