/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Type
 *  org.bukkit.entity.ZombieVillager
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
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Villager Type")
@Description(value={"Represents the type of a villager/zombie villager. This usually represents the biome the villager is from."})
@Example.Examples(value={@Example(value="set {_type} to villager type of {_villager}"), @Example(value="villager type of {_villager} = plains"), @Example(value="set villager type of event-entity to plains")})
@Since(value={"2.10"})
public class ExprVillagerType
extends SimplePropertyExpression<LivingEntity, Villager.Type> {
    @Override
    @Nullable
    public Villager.Type convert(LivingEntity from) {
        if (from instanceof Villager) {
            Villager villager = (Villager)from;
            return villager.getVillagerType();
        }
        if (from instanceof ZombieVillager) {
            ZombieVillager zombie = (ZombieVillager)from;
            return zombie.getVillagerType();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(Villager.Type.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Villager.Type t;
        Object object;
        Villager.Type type;
        Villager.Type type2 = type = delta != null && (object = delta[0]) instanceof Villager.Type ? (t = (Villager.Type)object) : null;
        if (type == null) {
            return;
        }
        for (LivingEntity livingEntity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (livingEntity instanceof Villager) {
                Villager villager = (Villager)livingEntity;
                villager.setVillagerType(type);
                continue;
            }
            if (!(livingEntity instanceof ZombieVillager)) continue;
            ZombieVillager zombie = (ZombieVillager)livingEntity;
            zombie.setVillagerType(type);
        }
    }

    @Override
    protected String getPropertyName() {
        return "villager type";
    }

    @Override
    public Class<? extends Villager.Type> getReturnType() {
        return Villager.Type.class;
    }

    static {
        ExprVillagerType.register(ExprVillagerType.class, Villager.Type.class, "villager type", "livingentities");
    }
}

