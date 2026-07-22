/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.bukkit.util.BoundingBox
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.AABB;
import ch.njol.util.Kleenean;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

@Name(value="Is Within")
@Description(value={"Whether a location is within something else. The \"something\" can be a block, an entity, a chunk, a world, or a cuboid formed by two other locations.", "Note that using the <a href='#CondCompare'>is between</a> condition will refer to a straight line between locations, while this condition will refer to the cuboid between locations."})
@Example.Examples(value={@Example(value="if player's location is within {_loc1} and {_loc2}:\n\tsend \"You are in a PvP zone!\" to player\n"), @Example(value="if player is in world(\"world\"):\n\tsend \"You are in the overworld!\" to player\n"), @Example(value="if attacker's location is inside of victim:\n\tcancel event\n\tsend \"Back up!\" to attacker and victim\n"), @Example(value="if player is in world \"world1\" or world \"world2\":\n\tkill player\n"), @Example(value="if player is in world \"world\" and chunk at location(0, 0, 0):\n\tgive player 1 diamond\n")})
@Since(value={"2.7, 2.11 (world borders)"})
@RequiredPlugins(value={"MC 1.17+ (within block)"})
public class CondIsWithin
extends Condition {
    private Expression<Location> locsToCheck;
    private Expression<Location> loc1;
    private Expression<Location> loc2;
    private Expression<?> area;
    private boolean withinLocations;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setNegated(matchedPattern % 2 == 1);
        this.locsToCheck = exprs[0];
        if (matchedPattern <= 1) {
            this.withinLocations = true;
            this.loc1 = exprs[1];
            this.loc2 = exprs[2];
        } else {
            this.withinLocations = false;
            this.area = exprs[1];
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.withinLocations) {
            Location one = this.loc1.getSingle(event);
            Location two = this.loc2.getSingle(event);
            if (one == null || two == null || one.getWorld() != two.getWorld()) {
                return this.isNegated();
            }
            AABB box = new AABB(one, two);
            return this.locsToCheck.check(event, box::contains, this.isNegated());
        }
        Object[] areas = this.area.getAll(event);
        return this.locsToCheck.check(event, location -> SimpleExpression.check(areas, object -> {
            if (object instanceof Entity) {
                Entity entity = (Entity)object;
                BoundingBox entityBox = entity.getBoundingBox();
                return entityBox.contains(location.toVector());
            }
            if (object instanceof Block) {
                Block block = (Block)object;
                for (BoundingBox blockBox : block.getCollisionShape().getBoundingBoxes()) {
                    Vector blockVector = block.getLocation().toVector();
                    if (!blockBox.contains(location.toVector().subtract(blockVector))) continue;
                    return true;
                }
                return false;
            }
            if (object instanceof Chunk) {
                Chunk chunk = (Chunk)object;
                return location.getChunk().equals((Object)chunk);
            }
            if (object instanceof World) {
                World world = (World)object;
                return location.getWorld().equals((Object)world);
            }
            if (object instanceof WorldBorder) {
                WorldBorder worldBorder = (WorldBorder)object;
                return worldBorder.isInside(location);
            }
            return false;
        }, false, this.area.getAnd()), this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append(this.locsToCheck, "is within");
        if (this.withinLocations) {
            builder.append(this.loc1, "and", this.loc2);
        } else {
            builder.append((Object)this.area);
        }
        return builder.toString();
    }

    static {
        Skript.registerCondition(CondIsWithin.class, "%locations% (is|are) within %location% and %location%", "%locations% (isn't|is not|aren't|are not) within %location% and %location%", "%locations% (is|are) (within|in[side [of]]) %entities/chunks/worlds/worldborders/blocks%", "%locations% (isn't|is not|aren't|are not) (within|in[side [of]]) %entities/chunks/worlds/worldborders/blocks%");
    }
}

