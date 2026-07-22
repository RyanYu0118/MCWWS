/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Name(value="Vectors - Location Vector Offset")
@Description(value={"Returns the location offset by vectors. Supports both global and local axes. When using local axes, the vector is applied relative to the direction the location is facing."})
@Example.Examples(value={@Example(value="set {_loc} to {_loc} ~ {_v}"), @Example(value="# spawn a tnt 5 blocks in front of player\nset {_l} to player's location offset by vector(0, 1, 5) using local axes\nspawn tnt at {_l}\n")})
@Since(value={"2.2-dev28, 2.14 (local axes)"})
public class ExprLocationVectorOffset
extends SimpleExpression<Location> {
    private Expression<Location> location;
    private Expression<Vector> vectors;
    private boolean usingLocalAxes;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.location = exprs[0];
        this.vectors = exprs[1];
        this.usingLocalAxes = parseResult.hasTag("facingrelative");
        return true;
    }

    protected Location[] get(Event event) {
        Location location = this.location.getSingle(event);
        if (location == null) {
            return null;
        }
        Location clone = location.clone();
        for (Vector vector : this.vectors.getArray(event)) {
            if (this.usingLocalAxes) {
                vector = ExprLocationVectorOffset.getFacingRelativeOffset(clone, vector);
            }
            clone.add(vector);
        }
        return new Location[]{clone};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public Expression<? extends Location> simplify() {
        if (this.location instanceof Literal && this.vectors instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return new SyntaxStringBuilder(event, debug).append((Object)this.location).append((Object)"offset by").append((Object)this.vectors).appendIf(this.usingLocalAxes, (Object)"using local axes").toString();
    }

    private static Vector getFacingRelativeOffset(Location loc, Vector offset) {
        float yawRad = (float)Math.toRadians(-loc.getYaw());
        float pitchRad = (float)Math.toRadians(loc.getPitch());
        float rollRad = 0.0f;
        Quaternionf rotation = new Quaternionf().rotateYXZ(yawRad, pitchRad, rollRad);
        Vector3f localOffset = offset.toVector3f();
        rotation.transform(localOffset);
        return new Vector(localOffset.x, localOffset.y, localOffset.z);
    }

    static {
        Skript.registerExpression(ExprLocationVectorOffset.class, Location.class, ExpressionType.PROPERTY, "%location% offset by [[the] vectors] %vectors% [facingrelative:using local axes]", "%location%[ ]~[~][ ]%vectors%");
    }
}

