/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Save World")
@Description(value={"Save all worlds or a given world manually.", "Note: saving many worlds at once may possibly cause the server to freeze."})
@Example.Examples(value={@Example(value="save \"world_nether\""), @Example(value="save all worlds")})
@Since(value={"2.8.0"})
public class EffWorldSave
extends Effect {
    private Expression<World> worlds;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.worlds = exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (World world : this.worlds.getArray(event)) {
            world.save();
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "save the world(s) " + this.worlds.toString(event, debug);
    }

    static {
        Skript.registerEffect(EffWorldSave.class, "save [[the] world[s]] %worlds%");
    }
}

