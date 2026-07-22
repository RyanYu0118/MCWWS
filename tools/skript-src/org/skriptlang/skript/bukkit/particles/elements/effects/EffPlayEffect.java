/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.EntityEffect
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.particles.elements.effects;

import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Direction;
import ch.njol.util.Kleenean;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.particles.GameEffect;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Play or Draw an Effect")
@Description(value={"Plays or draws a specific effect at a location, to a player, or on an entity.\nEffects can be:\n* Particles.\n* Game effects, which consist of combinations of particles and sounds, like the bone meal particles, the sound of footsteps on a specific block, or the particles and sound of breaking a splash potion.\n* Entity effects, which are particles or animations that are entity-specific and can only be played on a compatible entity. For example, the ravager attack animation can be played with this effect.\n\nAll effects vary significantly in availability from version to version, and some may simply not function on your version of Minecraft. Some effects, like the death animation entity effect, may cause client glitches and should be used carefully!\n"})
@Example.Examples(value={@Example(value="draw 2 smoke particles at player"), @Example(value="force draw 10 red dust particles of size 3 for player"), @Example(value="play blue instant splash potion break effect with a view radius of 10"), @Example(value="play ravager attack animation on player's target")})
@Since(value={"2.14"})
public class EffPlayEffect
extends Effect {
    private Expression<?> toDraw;
    @Nullable
    private Expression<Location> locations;
    @Nullable
    private Expression<Player> toPlayers;
    @Nullable
    private Expression<Player> asPlayer;
    @Nullable
    private Expression<Number> radius;
    private boolean force;
    @Nullable
    private Expression<Entity> entities;
    private Node node;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffPlayEffect.class).addPatterns("[:force] (play|show|draw) %gameeffects/particles% [%-directions% %locations%] [as %-player%]", "[:force] (play|draw) %gameeffects/particles% [%-directions% %locations%] (for|to) %-players% [as %-player%]", "(play|show|draw) %gameeffects% [%-directions% %locations%] (in|with) [a] [view] (radius|range) [of] %number%", "(play|show|draw) %entityeffects% on %entities%").supplier(EffPlayEffect::new).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.force = parseResult.hasTag("force");
        this.toDraw = expressions[0];
        switch (matchedPattern) {
            case 0: {
                this.locations = Direction.combine(expressions[1], expressions[2]);
                this.asPlayer = expressions[3];
                break;
            }
            case 1: {
                this.locations = Direction.combine(expressions[1], expressions[2]);
                this.toPlayers = expressions[3];
                this.asPlayer = expressions[4];
                break;
            }
            case 2: {
                this.locations = Direction.combine(expressions[1], expressions[2]);
                this.radius = expressions[3];
                break;
            }
            case 3: {
                this.entities = expressions[1];
            }
        }
        this.node = this.getParser().getNode();
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (this.entities != null) {
            Entity[] entities = this.entities.getArray(event);
            EntityEffect[] effects = (EntityEffect[])this.toDraw.getArray(event);
            this.drawEntityEffects(effects, entities);
            return;
        }
        assert (this.locations != null);
        Number radius = this.radius != null ? (Number)this.radius.getSingle(event) : (Number)null;
        Location[] locations = this.locations.getArray(event);
        ?[] toDraw = this.toDraw.getArray(event);
        Player[] players = this.toPlayers != null ? this.toPlayers.getArray(event) : null;
        Player asPlayer = this.asPlayer != null ? this.asPlayer.getSingle(event) : null;
        for (Object draw : toDraw) {
            if (draw instanceof GameEffect) {
                GameEffect gameEffect = (GameEffect)draw;
                if (players == null) {
                    for (Location location : locations) {
                        gameEffect.draw(location, radius);
                    }
                    continue;
                }
                for (Location location : players) {
                    for (Location location2 : locations) {
                        gameEffect.drawForPlayer(location2, (Player)location);
                    }
                }
                continue;
            }
            if (!(draw instanceof ParticleEffect)) continue;
            ParticleEffect particleEffect = (ParticleEffect)draw;
            particleEffect = particleEffect.copy();
            if (asPlayer != null) {
                particleEffect.source(asPlayer);
            }
            particleEffect.force(this.force);
            particleEffect.receivers(players);
            for (Location location : locations) {
                particleEffect.spawn(location);
            }
        }
    }

    private void drawEntityEffects(EntityEffect @NotNull [] effects, Entity @NotNull [] entities) {
        for (EntityEffect effect : effects) {
            boolean played = false;
            for (Entity entity : entities) {
                if (!effect.isApplicableTo(entity)) continue;
                entity.playEffect(effect);
                played = true;
            }
            if (entities.length <= 0 || played) continue;
            Object[] applicableClasses = (String[])effect.getApplicableClasses().stream().map(EntityData::toString).distinct().toArray(String[]::new);
            assert (this.entities != null);
            this.warning("The '" + Classes.toString(effect) + "' is not applicable to any of the given entities (" + Classes.toString(entities, this.entities.getAnd()) + "), only to " + Classes.toString(applicableClasses, false) + ".");
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return new SyntaxStringBuilder(event, debug).append("play", this.toDraw).appendIf(this.locations != null, (Object)this.locations).appendIf(this.toPlayers != null, "for", this.toPlayers).toString();
    }

    @Override
    public Node getNode() {
        return this.node;
    }
}

