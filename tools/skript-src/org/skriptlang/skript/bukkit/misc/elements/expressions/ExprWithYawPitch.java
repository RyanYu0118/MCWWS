/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.event.Event
 *  org.jspecify.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.misc.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Location with Yaw/Pitch")
@Description(value={"Returns the given locations with the specified yaw and/or pitch."})
@Example(value="set {_location} to player's location with yaw 0 and pitch 0")
@Since(value={"2.15"})
public class ExprWithYawPitch
extends PropertyExpression<Location, Location> {
    private Expression<Number> yaw;
    private Expression<Number> pitch;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprWithYawPitch.class, Location.class).supplier(ExprWithYawPitch::new)).addPattern("%locations% with [a] (:yaw|:pitch) [of] %number%")).addPattern("%locations% with [a] yaw [of] %number% and [a] pitch [of] %number%")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(expressions[0]);
        if (parseResult.hasTag("yaw")) {
            this.yaw = expressions[1];
        } else if (parseResult.hasTag("pitch")) {
            this.pitch = expressions[1];
        } else {
            this.yaw = expressions[1];
            this.pitch = expressions[2];
        }
        return true;
    }

    protected Location[] get(Event event, Location[] source) {
        Number yaw = this.yaw != null ? (Number)this.yaw.getSingle(event) : (Number)null;
        Number pitch = this.pitch != null ? (Number)this.pitch.getSingle(event) : (Number)null;
        return this.get(source, location -> {
            float finalYaw = yaw != null ? yaw.floatValue() : location.getYaw();
            float finalPitch = pitch != null ? pitch.floatValue() : location.getPitch();
            Location clone = location.clone();
            clone.setYaw(finalYaw);
            clone.setPitch(finalPitch);
            return clone;
        });
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return new SyntaxStringBuilder(event, debug).append(this.getExpr(), "with").appendIf(this.yaw != null, "yaw", this.yaw).appendIf(this.yaw != null && this.pitch != null, (Object)"and").appendIf(this.pitch != null, "pitch", this.pitch).toString();
    }
}

