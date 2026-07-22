/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.persistence.PersistentDataContainerView
 *  org.bukkit.NamespacedKey
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.pdc.elements.conditions;

import ch.njol.skript.bukkitutil.NamespacedUtils;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import io.papermc.paper.persistence.PersistentDataContainerView;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.pdc.PDCUtils;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Has Persistent Data Tag")
@Description(value={"Checks if the specified objects have persistent data tags with the given keys.\nKeys should be in the format \"namespace:key\" or \"key\". If a namespace is omitted, \"minecraft\" will be used instead.\nIf a key is invalid, it will be ignored and a warning will be logged.\n"})
@Example.Examples(value={@Example(value="if player has persistent data tag \"koth:capturePoint\":\n\tadd 1 to {points::%{team::%player%}%}\n"), @Example(value="if player's tool has persistent data tags \"custom:damage\" and \"custom:owner\":\n\tif data tag \"custom:owner\" of player's tool is not player:\n\t\tbroadcast \"You are not the owner of this tool!\"\n\t\tstop\n\tif data tag \"custom:damage\" of player's tool > 10:\n\t\tbroadcast \"Your tool is heavily damaged!\"\n\telse:\n\t\tbroadcast \"Your tool is in good condition.\"\n")})
@Since(value={"2.15"})
@Keywords(value={"pdc", "persistent data container", "custom data", "nbt"})
public class CondHasPersistentDataTag
extends Condition {
    private Expression<String> keys;
    private Expression<Object> holders;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.CONDITION, PropertyCondition.infoBuilder(CondHasPersistentDataTag.class, PropertyCondition.PropertyType.HAVE, "[persistent] data tag[s] %strings%", "objects").supplier(CondHasPersistentDataTag::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.holders = LiteralUtils.defendExpression(expressions[0]);
        this.keys = expressions[1];
        this.setNegated(matchedPattern == 1);
        return LiteralUtils.canInitSafely(this.holders);
    }

    @Override
    public boolean check(Event event) {
        boolean keysAnd = this.keys.getAnd();
        NamespacedKey[] keys = (NamespacedKey[])this.keys.stream(event).map(key -> NamespacedUtils.checkValidationAndSend(key.toLowerCase(Locale.ENGLISH), this)).toArray(NamespacedKey[]::new);
        if (keys.length == 0) {
            return this.isNegated();
        }
        return this.holders.check(event, holder -> {
            PersistentDataContainerView container = PDCUtils.getPersistentDataContainer(holder);
            if (container == null) {
                return false;
            }
            return SimpleExpression.check(keys, arg_0 -> ((PersistentDataContainerView)container).has(arg_0), false, keysAnd);
        }, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return PropertyCondition.toString(this, PropertyCondition.PropertyType.HAVE, event, debug, this.holders, "persistent data tag " + this.keys.toString(event, debug));
    }
}

