/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.pdc.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.pdc.PDCUtils;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="All Persistent Data Keys")
@Description(value={"Returns all persistent data keys stored in the specified objects.\nThis is not limited to tags set by Skript, but includes all keys regardless of their origin.\n"})
@Example.Examples(value={@Example(value="set {_keys::*} to persistent data keys of player's tool\nif size of {_keys::*} > 0:\n    broadcast \"The tool has the following persistent data keys: %{_keys::*}%\"\nelse:\n    broadcast \"The tool has no persistent data keys.\"\n"), @Example(value="for each {_key} in persistent data keys of player's tool:\n    broadcast \"Persistent data tag %{_key}%: %data tag {_key} of player's tool%\"\n")})
@Since(value={"2.15"})
@Keywords(value={"pdc", "persistent data container", "custom data", "nbt"})
public class ExprAllPersistentDataKeys
extends PropertyExpression<Object, String> {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprAllPersistentDataKeys.class, String.class).addPatterns("[all [[of] the]] [persistent] data [tag] keys of %objects%", "[all of] %objects%'[s] [persistent] data [tag] keys")).priority(DEFAULT_PRIORITY)).supplier(ExprAllPersistentDataKeys::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(LiteralUtils.defendExpression(expressions[0]));
        return LiteralUtils.canInitSafely(this.getExpr());
    }

    protected String[] get(Event event, Object[] source) {
        ArrayList keys = new ArrayList();
        for (Object obj : source) {
            PDCUtils.getPersistentDataContainer(obj, container -> {
                for (NamespacedKey key : container.getKeys()) {
                    keys.add(key.toString());
                }
            });
        }
        return keys.toArray(new String[0]);
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "persistent data keys of " + this.getExpr().toString(event, debug);
    }
}

