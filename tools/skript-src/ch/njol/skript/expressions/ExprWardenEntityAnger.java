/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Warden
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.Math2;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Warden;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Warden Anger Level")
@Description(value={"The anger level a warden feels towards an entity.", "A warden can be angry towards multiple entities with different anger levels.", "If an entity reaches an anger level of 80+, the warden will pursue it.", "Anger level maxes out at 150."})
@Example.Examples(value={@Example(value="set the anger level of last spawned warden towards player to 20"), @Example(value="clear the last spawned warden's anger level towards player")})
@Since(value={"2.11"})
public class ExprWardenEntityAnger
extends SimpleExpression<Integer> {
    private Expression<LivingEntity> wardens;
    private Expression<LivingEntity> targets;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.wardens = exprs[0];
        this.targets = exprs[1];
        return true;
    }

    protected Integer @Nullable [] get(Event event) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        Entity[] entities = (Entity[])this.targets.getArray(event);
        for (LivingEntity livingEntity : this.wardens.getArray(event)) {
            if (!(livingEntity instanceof Warden)) continue;
            Warden warden = (Warden)livingEntity;
            for (Entity entity : entities) {
                list.add(warden.getAnger(entity));
            }
        }
        return list.toArray(new Integer[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> CollectionUtils.array(Integer.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        int value = delta != null ? (Integer)delta[0] : 0;
        BiConsumer<Warden, Entity> consumer = switch (mode) {
            case Changer.ChangeMode.SET -> (warden, entity) -> warden.setAnger(entity, Math2.fit(0, value, 150));
            case Changer.ChangeMode.DELETE -> Warden::clearAnger;
            case Changer.ChangeMode.ADD -> (warden, entity) -> {
                int current = warden.getAnger(entity);
                int newValue = Math2.fit(0, current + value, 150);
                warden.setAnger(entity, newValue);
            };
            case Changer.ChangeMode.REMOVE -> (warden, entity) -> {
                int current = warden.getAnger(entity);
                int newValue = Math2.fit(0, current - value, 150);
                warden.setAnger(entity, newValue);
            };
            default -> throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)mode));
        };
        Entity[] entities = (Entity[])this.targets.getArray(event);
        for (LivingEntity livingEntity : this.wardens.getArray(event)) {
            if (!(livingEntity instanceof Warden)) continue;
            Warden warden2 = (Warden)livingEntity;
            for (Entity entity2 : entities) {
                consumer.accept(warden2, entity2);
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.wardens.isSingle() && this.targets.isSingle();
    }

    @Override
    public Class<Integer> getReturnType() {
        return Integer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the anger level of " + this.wardens.toString(event, debug) + " towards " + this.targets.toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprWardenEntityAnger.class, Integer.class, ExpressionType.COMBINED, "[the] anger level [of] %livingentities% towards %livingentities%", "%livingentities%'[s] anger level towards %livingentities%");
    }
}

