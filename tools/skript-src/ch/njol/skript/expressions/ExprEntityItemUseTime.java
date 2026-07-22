/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import org.bukkit.entity.LivingEntity;

@Name(value="Active Item Use Time")
@Description(value={"Returns the time that the entities have either spent using an item, or the time left for them to finish using an item.", "If an entity is not using any item, this will return 0 seconds."})
@Example(value="on right click:\n\tbroadcast player's remaining item use time\n\twait 1 second\n\tbroadcast player's item use time\n")
@Since(value={"2.8.0"})
public class ExprEntityItemUseTime
extends SimplePropertyExpression<LivingEntity, Timespan> {
    private boolean remaining;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.remaining = parseResult.hasTag("remaining");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    public Timespan convert(LivingEntity livingEntity) {
        if (this.remaining) {
            return new Timespan(Timespan.TimePeriod.TICK, livingEntity.getItemUseRemainingTime());
        }
        return new Timespan(Timespan.TimePeriod.TICK, livingEntity.getHandRaisedTime());
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    protected String getPropertyName() {
        return (this.remaining ? "remaining" : "elapsed") + " item usage time";
    }

    static {
        if (Skript.methodExists(LivingEntity.class, "getItemUseRemainingTime", new Class[0])) {
            ExprEntityItemUseTime.register(ExprEntityItemUseTime.class, Timespan.class, "[elapsed|:remaining] (item|tool) us[ag]e time", "livingentities");
        }
    }
}

