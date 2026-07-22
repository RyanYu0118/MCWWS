/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Beehive
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
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
import org.bukkit.Location;
import org.bukkit.block.Beehive;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Beehive Target Flower")
@Description(value={"The flower a beehive has selected to pollinate from."})
@Example.Examples(value={@Example(value="set the target flower of {_beehive} to location(0, 0, 0)"), @Example(value="clear the target flower of {_beehive}")})
@Since(value={"2.11"})
public class ExprBeehiveFlower
extends SimplePropertyExpression<Block, Location> {
    @Override
    @Nullable
    public Location convert(Block block) {
        BlockState blockState = block.getState();
        if (!(blockState instanceof Beehive)) {
            return null;
        }
        Beehive beehive = (Beehive)blockState;
        return beehive.getFlower();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(Location.class, Block.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Location location = null;
        if (delta != null) {
            Object object = delta[0];
            if (object instanceof Location) {
                Location loc;
                location = loc = (Location)object;
            } else {
                object = delta[0];
                if (object instanceof Block) {
                    Block block = (Block)object;
                    location = block.getLocation();
                }
            }
        }
        for (Block block : (Block[])this.getExpr().getArray(event)) {
            BlockState blockState = block.getState();
            if (!(blockState instanceof Beehive)) continue;
            Beehive beehive = (Beehive)blockState;
            beehive.setFlower(location);
            beehive.update(true, false);
        }
    }

    @Override
    public Class<Location> getReturnType() {
        return Location.class;
    }

    @Override
    protected String getPropertyName() {
        return "target flower";
    }

    static {
        ExprBeehiveFlower.registerDefault(ExprBeehiveFlower.class, Location.class, "target flower", "blocks");
    }
}

