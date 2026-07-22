/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Biome
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
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Biome")
@Description(value={"The biome at a certain location. Please note that biomes are only defined for x/z-columns", "(i.e. the <a href='#ExprAltitude'>altitude</a> (y-coordinate) doesn't matter), up until Minecraft 1.15.x.", "As of Minecraft 1.16, biomes are now 3D (per block vs column)."})
@Example(value="# damage player in deserts constantly\nevery real minute:\n\tloop all players:\n\t\tbiome at loop-player is desert\n\t\tdamage the loop-player by 1\n")
@Since(value={"1.4.4, 2.6.1 (3D biomes)"})
public class ExprBiome
extends PropertyExpression<Location, Biome> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(matchedPattern == 1 ? exprs[0] : Direction.combine(exprs[0], exprs[1]));
        return true;
    }

    protected Biome[] get(Event event, Location[] source) {
        return this.get(source, location -> location.getBlock().getBiome());
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return new Class[]{Biome.class};
        }
        return super.acceptChange(mode);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (mode != Changer.ChangeMode.SET) {
            super.change(event, delta, mode);
            return;
        }
        assert (delta != null);
        Biome biome = (Biome)delta[0];
        for (Location location : (Location[])this.getExpr().getArray(event)) {
            location.getBlock().setBiome(biome);
        }
    }

    @Override
    public Class<? extends Biome> getReturnType() {
        return Biome.class;
    }

    @Override
    public Expression<? extends Biome> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the biome at " + this.getExpr().toString(event, debug);
    }

    @Override
    public boolean setTime(int time) {
        super.setTime(time, this.getExpr(), new Class[0]);
        return true;
    }

    static {
        Skript.registerExpression(ExprBiome.class, Biome.class, ExpressionType.PROPERTY, "[the] biome [(of|%direction%) %locations%]", "%locations%'[s] biome");
    }
}

