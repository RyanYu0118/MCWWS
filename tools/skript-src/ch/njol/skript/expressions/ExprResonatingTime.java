/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Bell
 *  org.bukkit.block.Block
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.Timespan;
import org.bukkit.block.Bell;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

@Name(value="Resonating Time")
@Description(value={"Returns the resonating time of a bell.", "A bell will start resonating five game ticks after being rung, and will continue to resonate for 40 game ticks."})
@Example(value="broadcast \"The bell has been resonating for %resonating time of target block%\"")
@Since(value={"2.9.0"})
public class ExprResonatingTime
extends SimplePropertyExpression<Block, Timespan> {
    @Override
    @Nullable
    public Timespan convert(Block from) {
        if (from.getState() instanceof Bell) {
            int resonatingTicks = ((Bell)from.getState(false)).getResonatingTicks();
            return resonatingTicks == 0 ? null : new Timespan(Timespan.TimePeriod.TICK, resonatingTicks);
        }
        return null;
    }

    @Override
    protected String getPropertyName() {
        return "resonating time";
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    static {
        if (Skript.classExists("org.bukkit.block.Bell") && Skript.methodExists(Bell.class, "getResonatingTicks", new Class[0])) {
            ExprResonatingTime.register(ExprResonatingTime.class, Timespan.class, "resonat(e|ing) time", "block");
        }
    }
}

