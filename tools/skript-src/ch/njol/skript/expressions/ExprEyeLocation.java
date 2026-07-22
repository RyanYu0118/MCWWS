/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
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
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

@Name(value="Head location")
@Description(value={"The location of an entity's head, mostly useful for players and e.g. looping blocks in the player's line of sight.", "Please note that this location is only accurate for entities whose head is exactly above their center, i.e. players, endermen, zombies, skeletons, etc., but not sheep, pigs or cows."})
@Example.Examples(value={@Example(value="set the block at the player's head to air"), @Example(value="set the block in front of the player's eyes to glass"), @Example(value="loop blocks in front of the player's head:")})
@Since(value={"2.0"})
public class ExprEyeLocation
extends SimplePropertyExpression<LivingEntity, Location> {
    @Override
    public Class<Location> getReturnType() {
        return Location.class;
    }

    @Override
    protected String getPropertyName() {
        return "eye location";
    }

    @Override
    @Nullable
    public Location convert(LivingEntity e) {
        return e.getEyeLocation();
    }

    static {
        ExprEyeLocation.register(ExprEyeLocation.class, Location.class, "(head|eye[s]) [location[s]]", "livingentities");
    }
}

