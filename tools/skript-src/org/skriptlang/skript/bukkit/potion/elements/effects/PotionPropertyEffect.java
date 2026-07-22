/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.potion.elements.effects;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.potion.util.SkriptPotionEffect;

abstract class PotionPropertyEffect
extends Effect {
    private Expression<SkriptPotionEffect> potions;
    private boolean isNegated;

    PotionPropertyEffect() {
    }

    public static String[] getPatterns(Type type, String property) {
        String[] stringArray;
        switch (type.ordinal()) {
            default: {
                throw new MatchException(null, null);
            }
            case 0: {
                String[] stringArray2 = new String[1];
                stringArray = stringArray2;
                stringArray2[0] = "make %skriptpotioneffects% [:not] " + property;
                break;
            }
            case 1: {
                String[] stringArray3 = new String[2];
                stringArray3[0] = "(show|not:hide) [the] [potion] " + property + " [(of|for) %skriptpotioneffects%]";
                stringArray = stringArray3;
                stringArray3[1] = "(show|not:hide) %skriptpotioneffects%'[s] " + property;
            }
        }
        return stringArray;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.potions = expressions[0];
        this.isNegated = parseResult.hasTag("not");
        return SkriptPotionEffect.isChangeable(this.potions);
    }

    @Override
    protected void execute(Event event) {
        for (SkriptPotionEffect potionEffect : this.potions.getArray(event)) {
            this.modify(potionEffect, this.isNegated);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        switch (this.getPropertyType().ordinal()) {
            case 0: {
                builder.append("make", this.potions);
                if (this.isNegated) {
                    builder.append((Object)"not");
                }
                builder.append((Object)this.getPropertyName());
                break;
            }
            case 1: {
                if (this.isNegated) {
                    builder.append((Object)"hide");
                } else {
                    builder.append((Object)"show");
                }
                builder.append("the potion", this.getPropertyName(), "of", this.potions);
            }
        }
        return builder.toString();
    }

    public abstract void modify(SkriptPotionEffect var1, boolean var2);

    public abstract Type getPropertyType();

    public abstract String getPropertyName();

    public static enum Type {
        MAKE,
        SHOW;

    }
}

