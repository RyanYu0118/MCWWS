/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Profession
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

@Name(value="Villager Profession")
@Description(value={"Represents the profession of a villager/zombie villager."})
@Example.Examples(value={@Example(value="set {_p} to villager profession of event-entity"), @Example(value="villager profession of event-entity = nitwit profession"), @Example(value="set villager profession of {_villager} to librarian profession"), @Example(value="delete villager profession of event-entity")})
@Since(value={"2.10"})
public class ExprVillagerProfession
extends SimplePropertyExpression<LivingEntity, Villager.Profession> {
    @Override
    @Nullable
    public Villager.Profession convert(LivingEntity from) {
        if (from instanceof Villager) {
            Villager villager = (Villager)from;
            return villager.getProfession();
        }
        if (from instanceof ZombieVillager) {
            ZombieVillager zombie = (ZombieVillager)from;
            return zombie.getVillagerProfession();
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> CollectionUtils.array(Villager.Profession.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Villager.Profession pro;
        Object object;
        Villager.Profession profession = delta != null && (object = delta[0]) instanceof Villager.Profession ? (pro = (Villager.Profession)object) : Villager.Profession.NONE;
        for (LivingEntity livingEntity : (LivingEntity[])this.getExpr().getArray(event)) {
            if (livingEntity instanceof Villager) {
                Villager villager = (Villager)livingEntity;
                villager.setProfession(profession);
                continue;
            }
            if (!(livingEntity instanceof ZombieVillager)) continue;
            ZombieVillager zombie = (ZombieVillager)livingEntity;
            zombie.setVillagerProfession(profession);
        }
    }

    @Override
    protected String getPropertyName() {
        return "villager profession";
    }

    @Override
    public Class<? extends Villager.Profession> getReturnType() {
        return Villager.Profession.class;
    }

    static {
        ExprVillagerProfession.register(ExprVillagerProfession.class, Villager.Profession.class, "villager profession", "livingentities");
    }
}

