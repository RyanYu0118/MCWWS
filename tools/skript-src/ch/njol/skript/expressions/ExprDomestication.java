/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.AbstractHorse
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Horse Domestication")
@Description(value={"Gets and/or sets the (max) domestication of a horse.", "The domestication of a horse is how close a horse is to becoming tame - the higher the domestication, the closer they are to becoming tame (must be between 1 and the max domestication level of the horse).", "The max domestication of a horse is how long it will take for a horse to become tame (must be greater than 0)."})
@Example(value="function domesticateAndTame(horse: entity, p: offline player, i: int = 10):\n\tadd {_i} to domestication level of {_horse}\n\tif domestication level of {_horse} >= max domestication level of {_horse}:\n\t\ttame {_horse}\n\t\tset tamer of {_horse} to {_p}\n")
@Since(value={"2.10"})
public class ExprDomestication
extends SimplePropertyExpression<LivingEntity, Integer> {
    private boolean max;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.max = parseResult.hasTag("max");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Integer convert(LivingEntity entity) {
        if (entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse)entity;
            return this.max ? horse.getMaxDomestication() : horse.getDomestication();
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.RESET -> CollectionUtils.array(Number.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (mode != Changer.ChangeMode.REMOVE_ALL && mode != Changer.ChangeMode.DELETE);
        int change = delta == null ? 0 : ((Number)delta[0]).intValue();
        for (LivingEntity entity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (!(entity instanceof AbstractHorse)) continue;
            AbstractHorse horse = (AbstractHorse)entity;
            int level = this.max ? horse.getMaxDomestication() : horse.getDomestication();
            switch (mode) {
                case SET: {
                    level = change;
                    break;
                }
                case ADD: {
                    level += change;
                    break;
                }
                case REMOVE: {
                    level -= change;
                    break;
                }
                case RESET: {
                    level = 1;
                    break;
                }
                default: {
                    assert (false);
                    return;
                }
            }
            int n = level = this.max ? Math.max(level, 1) : Math2.fit(1, level, horse.getMaxDomestication());
            if (this.max) {
                horse.setMaxDomestication(level);
                if (horse.getDomestication() <= level) continue;
                horse.setDomestication(level);
                continue;
            }
            horse.setDomestication(level);
        }
    }

    @Override
    public Class<? extends Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.max ? "max " : "") + "domestication level";
    }

    static {
        ExprDomestication.register(ExprDomestication.class, Integer.class, "[:max[imum]] domestication level", "livingentities");
    }
}

