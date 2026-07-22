/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;

@Name(value="Entity is in Liquid")
@Description(value={"Checks whether an entity is in rain, lava, water or a bubble column."})
@Example.Examples(value={@Example(value="if player is in rain:"), @Example(value="if player is in water:"), @Example(value="player is in lava:"), @Example(value="player is in bubble column")})
@Since(value={"2.6.1"})
public class CondEntityIsInLiquid
extends PropertyCondition<Entity> {
    private static final int IN_WATER = 1;
    private static final int IN_LAVA = 2;
    private static final int IN_BUBBLE_COLUMN = 3;
    private static final int IN_RAIN = 4;
    private int mark;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        this.setNegated(matchedPattern == 1);
        this.mark = parseResult.mark;
        return true;
    }

    @Override
    public boolean check(Entity entity) {
        return switch (this.mark) {
            case 1 -> entity.isInWater();
            case 2 -> entity.isInLava();
            case 3 -> entity.isInBubbleColumn();
            case 4 -> entity.isInRain();
            default -> throw new IllegalStateException();
        };
    }

    @Override
    protected String getPropertyName() {
        return switch (this.mark) {
            case 1 -> "in water";
            case 2 -> "in lava";
            case 3 -> "in bubble column";
            case 4 -> "in rain";
            default -> throw new IllegalStateException();
        };
    }

    static {
        StringBuilder patterns = new StringBuilder();
        patterns.append("1\u00a6water");
        if (Skript.methodExists(Entity.class, "isInLava", new Class[0])) {
            patterns.append("|2\u00a6lava|3\u00a6[a] bubble[ ]column|4\u00a6rain");
        }
        CondEntityIsInLiquid.register(CondEntityIsInLiquid.class, "in (" + String.valueOf(patterns) + ")", "entities");
    }
}

