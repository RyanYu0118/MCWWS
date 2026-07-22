/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameRule
 *  org.bukkit.World
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="PvP")
@Description(value={"Set the PvP state for a given world."})
@Example.Examples(value={@Example(value="enable PvP #(current world only)"), @Example(value="disable PvP in all worlds")})
@Since(value={"1.3.4"})
public class EffPvP
extends Effect {
    private static final boolean PVP_GAME_RULE_EXISTS = Skript.fieldExists(GameRule.class, "PVP");
    private Expression<World> worlds;
    private boolean enable;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.worlds = exprs[0];
        this.enable = matchedPattern == 0;
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (PVP_GAME_RULE_EXISTS) {
            for (World world : this.worlds.getArray(event)) {
                world.setGameRule(GameRule.PVP, (Object)this.enable);
            }
        } else {
            for (World world : this.worlds.getArray(event)) {
                world.setPVP(this.enable);
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.enable ? "enable" : "disable") + " PvP in " + this.worlds.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffPvP.class, "enable PvP [in %worlds%]", "disable PVP [in %worlds%]");
    }
}

