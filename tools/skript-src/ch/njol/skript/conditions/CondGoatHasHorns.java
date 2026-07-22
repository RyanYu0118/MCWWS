/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Goat
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.conditions;

import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.effects.EffGoatHorns;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;

@Name(value="Goat Has Horns")
@Description(value={"Checks to see if a goat has or does not have a left, right, or both horns."})
@Example.Examples(value={@Example(value="\tif last spawned goat does not have both horns:\n\t\tmake last spawned goat have both horns\n"), @Example(value="\tif {_goat} has a right horn:\n\t\tforce {_goat} to not have a right horn\n")})
@Since(value={"2.11"})
public class CondGoatHasHorns
extends PropertyCondition<LivingEntity> {
    private EffGoatHorns.GoatHorn goatHorn = EffGoatHorns.GoatHorn.ANY;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (parseResult.hasTag("left")) {
            this.goatHorn = EffGoatHorns.GoatHorn.LEFT;
        } else if (parseResult.hasTag("right")) {
            this.goatHorn = EffGoatHorns.GoatHorn.RIGHT;
        } else if (parseResult.hasTag("both")) {
            this.goatHorn = EffGoatHorns.GoatHorn.BOTH;
        }
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public boolean check(LivingEntity entity) {
        if (!(entity instanceof Goat)) {
            return false;
        }
        Goat goat = (Goat)entity;
        boolean leftHorn = goat.hasLeftHorn();
        boolean rightHorn = goat.hasRightHorn();
        return switch (this.goatHorn) {
            default -> throw new MatchException(null, null);
            case EffGoatHorns.GoatHorn.ANY -> {
                if (leftHorn || rightHorn) {
                    yield true;
                }
                yield false;
            }
            case EffGoatHorns.GoatHorn.BOTH -> {
                if (leftHorn && rightHorn) {
                    yield true;
                }
                yield false;
            }
            case EffGoatHorns.GoatHorn.LEFT -> leftHorn;
            case EffGoatHorns.GoatHorn.RIGHT -> rightHorn;
        };
    }

    @Override
    protected String getPropertyName() {
        return switch (this.goatHorn) {
            default -> throw new MatchException(null, null);
            case EffGoatHorns.GoatHorn.ANY -> "a horn";
            case EffGoatHorns.GoatHorn.BOTH -> "both horns";
            case EffGoatHorns.GoatHorn.LEFT -> "left horn";
            case EffGoatHorns.GoatHorn.RIGHT -> "right horn";
        };
    }

    static {
        CondGoatHasHorns.register(CondGoatHasHorns.class, PropertyCondition.PropertyType.HAVE, "((any|a) horn|left:[a] left horn[s]|right:[a] right horn[s]|both:both horns)", "livingentities");
    }
}

