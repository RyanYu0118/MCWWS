/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.World
 *  org.bukkit.event.Event
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
import ch.njol.util.Kleenean;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Do Respawn Anchors Work")
@Description(value={"Checks whether or not respawn anchors work in a world."})
@Example(value="respawn anchors work in world \"world_nether\"")
@RequiredPlugins(value={"Minecraft 1.16+"})
@Since(value={"2.7"})
public class CondAnchorWorks
extends Condition {
    private Expression<World> worlds;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.worlds = exprs[0];
        this.setNegated(parseResult.mark == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        return this.worlds.check(event, World::isRespawnAnchorWorks, this.isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "respawn anchors " + (this.isNegated() ? " do" : " don't") + " work in " + this.worlds.toString(event, debug);
    }

    static {
        Skript.registerCondition(CondAnchorWorks.class, "respawn anchors [do[1:(n't| not)]] work in %worlds%");
    }
}

