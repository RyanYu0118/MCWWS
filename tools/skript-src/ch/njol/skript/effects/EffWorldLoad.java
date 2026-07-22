/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.WorldCreator
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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Load World")
@Description(value={"Load your worlds or unload your worlds", "The load effect will create a new world if world doesn't already exist.", "When attempting to load a normal vanilla world you must define it's environment i.e \"world_nether\" must be loaded with nether environment"})
@Example.Examples(value={@Example(value="load world \"world_nether\" with environment nether"), @Example(value="load the world \"myCustomWorld\""), @Example(value="unload \"world_nether\""), @Example(value="unload \"world_the_end\" without saving"), @Example(value="unload all worlds")})
@Since(value={"2.8.0"})
public class EffWorldLoad
extends Effect {
    private boolean save;
    private boolean load;
    private Expression<?> worlds;
    @Nullable
    private Expression<World.Environment> environment;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.worlds = exprs[0];
        boolean bl = this.load = matchedPattern == 0;
        if (this.load) {
            this.environment = exprs[1];
        } else {
            this.save = !parseResult.hasTag("without saving");
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        World.Environment environment = this.environment != null ? this.environment.getSingle(event) : null;
        for (Object world : this.worlds.getArray(event)) {
            if (this.load && world instanceof String) {
                WorldCreator worldCreator = new WorldCreator((String)world);
                if (environment != null) {
                    worldCreator.environment(environment);
                }
                worldCreator.createWorld();
                continue;
            }
            if (this.load || !(world instanceof World)) continue;
            Bukkit.unloadWorld((World)((World)world), (boolean)this.save);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.load) {
            return "load the world(s) " + this.worlds.toString(event, debug) + (String)(this.environment == null ? "" : " with environment " + this.environment.toString(event, debug));
        }
        return "unload the world(s) " + this.worlds.toString(event, debug) + " " + (this.save ? "with saving" : "without saving");
    }

    static {
        Skript.registerEffect(EffWorldLoad.class, "load [the] world[s] %strings% [with environment %-environment%]", "unload [[the] world[s]] %worlds% [:without saving]");
    }
}

