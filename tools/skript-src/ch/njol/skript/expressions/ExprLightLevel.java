/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.block.Block
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Light Level")
@Description(value={"Gets the light level at a certain location which ranges from 0 to 15.", "It can be separated into sunlight (15 = direct sunlight, 1-14 = indirect) and block light (torches, glowstone, etc.). The total light level of a block is the maximum of the two different light types."})
@Example(value="# set vampire players standing in bright sunlight on fire\nevery 5 seconds:\n\tloop all players:\n\t\t{vampire::%uuid of loop-player%} is true\n\t\tsunlight level at the loop-player is greater than 10\n\t\tignite the loop-player for 5 seconds\n")
@Since(value={"1.3.4"})
public class ExprLightLevel
extends PropertyExpression<Location, Byte> {
    private final int SKY = 1;
    private final int BLOCK = 2;
    private final int ANY = 3;
    int whatLight = 3;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(Direction.combine(exprs[0], exprs[1]));
        this.whatLight = parseResult.mark == 0 ? 3 : parseResult.mark;
        return true;
    }

    @Override
    public Class<Byte> getReturnType() {
        return Byte.class;
    }

    protected Byte[] get(Event e, Location[] source) {
        return this.get(source, location -> {
            Block block = location.getBlock();
            return this.whatLight == 3 ? block.getLightLevel() : (this.whatLight == 2 ? block.getLightFromBlocks() : block.getLightFromSky());
        });
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return (this.whatLight == 2 ? "block " : (this.whatLight == 1 ? "sky " : "")) + "light level " + this.getExpr().toString(e, debug);
    }

    static {
        Skript.registerExpression(ExprLightLevel.class, Byte.class, ExpressionType.PROPERTY, "[(1\u00a6sky|1\u00a6sun|2\u00a6block)[ ]]light[ ]level [(of|%direction%) %location%]");
    }
}

