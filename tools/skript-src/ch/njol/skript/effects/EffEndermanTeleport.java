/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Enderman
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import java.util.function.Consumer;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Enderman Teleport")
@Description(value={"Make an enderman teleport randomly or towards an entity.", "Teleporting towards an entity teleports in the direction to the entity and not to them."})
@Example.Examples(value={@Example(value="make last spawned enderman teleport randomly"), @Example(value="loop 10 times:\n\tmake all endermen teleport towards player\n")})
@RequiredPlugins(value={"Minecraft 1.20.1+"})
@Since(value={"2.11"})
public class EffEndermanTeleport
extends Effect {
    private Expression<LivingEntity> entities;
    @Nullable
    private Expression<Entity> target;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.entities = exprs[0];
        if (matchedPattern >= 2) {
            this.target = exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Entity target;
        Consumer<Enderman> consumer = Enderman::teleport;
        if (this.target != null && (target = this.target.getSingle(event)) != null) {
            consumer = enderman -> enderman.teleportTowards(target);
        }
        for (LivingEntity entity : this.entities.getArray(event)) {
            if (!(entity instanceof Enderman)) continue;
            Enderman enderman2 = (Enderman)entity;
            consumer.accept(enderman2);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.entities);
        if (this.target == null) {
            builder.append((Object)"randomly teleport");
        } else {
            builder.append("teleport towards", this.target);
        }
        return builder.toString();
    }

    static {
        if (Skript.isRunningMinecraft(1, 20, 1)) {
            Skript.registerEffect(EffEndermanTeleport.class, "make %livingentities% (randomly teleport|teleport randomly)", "force %livingentities% to (randomly teleport|teleport randomly)", "make %livingentities% teleport [randomly] towards %entity%", "force %livingentities% to teleport [randomly] towards %entity%");
        }
    }
}

