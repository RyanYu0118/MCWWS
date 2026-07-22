/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Allay
 *  org.bukkit.entity.LivingEntity
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.Location;
import org.bukkit.entity.Allay;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Name(value="Allay Target Jukebox")
@Description(value={"The location of the jukebox an allay is set to."})
@Example(value="set {_loc} to the target jukebox of last spawned allay")
@Since(value={"2.11"})
public class ExprAllayJukebox
extends SimplePropertyExpression<LivingEntity, Location> {
    @Override
    @Nullable
    public Location convert(LivingEntity entity) {
        Location location;
        if (entity instanceof Allay) {
            Allay allay = (Allay)entity;
            location = allay.getJukebox();
        } else {
            location = null;
        }
        return location;
    }

    @Override
    public Class<Location> getReturnType() {
        return Location.class;
    }

    @Override
    protected String getPropertyName() {
        return "target jukebox";
    }

    static {
        ExprAllayJukebox.registerDefault(ExprAllayJukebox.class, Location.class, "target jukebox", "livingentities");
    }
}

