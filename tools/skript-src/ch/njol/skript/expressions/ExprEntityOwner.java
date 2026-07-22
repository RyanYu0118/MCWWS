/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.AnimalTamer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Tameable
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
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Owner")
@Description(value={"The owner of a tameable entity (i.e. horse or wolf)."})
@Example(value="\tset owner of last spawned wolf to player\n\tif the owner of last spawned wolf is player:\n")
@Since(value={"2.5"})
public class ExprEntityOwner
extends SimplePropertyExpression<Entity, OfflinePlayer> {
    @Override
    @Nullable
    public OfflinePlayer convert(Entity entity) {
        AnimalTamer animalTamer;
        Tameable tameable;
        if (entity instanceof Tameable && (tameable = (Tameable)entity).isTamed() && (animalTamer = tameable.getOwner()) instanceof OfflinePlayer) {
            OfflinePlayer owner = (OfflinePlayer)animalTamer;
            return owner;
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> CollectionUtils.array(OfflinePlayer.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        OfflinePlayer newPlayer = delta == null ? null : (OfflinePlayer)delta[0];
        for (Entity entity : (Entity[])this.getExpr().getArray(event)) {
            if (!(entity instanceof Tameable)) continue;
            Tameable tameable = (Tameable)entity;
            tameable.setOwner((AnimalTamer)newPlayer);
        }
    }

    @Override
    public Class<OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    protected String getPropertyName() {
        return "owner";
    }

    static {
        Skript.registerExpression(ExprEntityOwner.class, OfflinePlayer.class, ExpressionType.PROPERTY, "[the] (owner|tamer) of %livingentities%", "%livingentities%'[s] (owner|tamer)");
    }
}

