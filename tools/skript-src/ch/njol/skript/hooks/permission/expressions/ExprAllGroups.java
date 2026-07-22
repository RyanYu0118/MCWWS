/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.permission.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.hooks.VaultHook;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="All Groups")
@Description(value={"All the groups a player can have. This expression requires Vault and a compatible permissions plugin to be installed."})
@Example(value="command /group &lt;text&gt;:\n\ttrigger:\n\t\tif argument is \"list\":\n\t\t\tsend \"%all groups%\"\n")
@Since(value={"2.2-dev35"})
@RequiredPlugins(value={"Vault", "a permission plugin that supports Vault"})
public class ExprAllGroups
extends SimpleExpression<String> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!VaultHook.permission.hasGroupSupport()) {
            Skript.error("The permissions plugin you are using does not support groups.");
            return false;
        }
        return true;
    }

    @Nullable
    protected String[] get(Event e) {
        return VaultHook.permission.getGroups();
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "all groups";
    }

    static {
        Skript.registerExpression(ExprAllGroups.class, String.class, ExpressionType.SIMPLE, "all groups");
    }
}

