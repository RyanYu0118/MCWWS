/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntitySnapshot
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityType;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Direction;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Spawn")
@Description(value={"Spawns entities. This can be used as an effect and as a section.", "", "If it is used as a section, the section is run before the entity is added to the world.", "You can modify the entity in this section, using for example 'event-entity' or 'cow'. ", "Do note that other event values, such as 'player', won't work in this section.", "", "If you're spawning a display and want it to be empty on initialization, like not having a block display be stone, set hidden config node 'spawn empty displays' to true.", "", "Note that when spawning an entity via entity snapshots, the code within the section will not run instantaneously as compared to spawning normally (via 'a zombie')."})
@Example.Examples(value={@Example(value="spawn 3 creepers at the targeted block"), @Example(value="spawn a ghast 5 meters above the player"), @Example(value="spawn a zombie at the player:\n\tset name of the zombie to \"\"\n"), @Example(value="spawn a block display of a ladder[waterlogged=true] at location above player:\n\tset billboard of event-display to center # allows the display to rotate around the center axis\n")})
@RequiredPlugins(value={"Minecraft 1.20.2+ (entity snapshots)"})
@Since(value={"1.0, 2.6.1 (with section), 2.8.6 (dropped items), 2.10 (entity snapshots)"})
public class EffSecSpawn
extends EffectSection {
    private Expression<Location> locations;
    private Expression<?> types;
    @Nullable
    private Expression<Number> amount;
    @Nullable
    public static Entity lastSpawned;
    @Nullable
    private Trigger trigger;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        this.amount = matchedPattern == 0 ? null : exprs[0];
        this.types = exprs[matchedPattern];
        this.locations = Direction.combine(exprs[1 + matchedPattern], exprs[2 + matchedPattern]);
        if (sectionNode != null) {
            this.trigger = SectionUtils.loadLinkedCode("spawn", (beforeLoading, afterLoading) -> this.loadCode(sectionNode, "spawn", (Runnable)beforeLoading, (Runnable)afterLoading, (Class<? extends Event>)SpawnEvent.class));
            return this.trigger != null;
        }
        return true;
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        Integer numberAmount;
        lastSpawned = null;
        Consumer<Entity> consumer = this.trigger != null ? entity -> {
            lastSpawned = entity;
            SpawnEvent spawnEvent = new SpawnEvent((Entity)entity);
            Variables.withLocalVariables(event, spawnEvent, () -> TriggerItem.walk(this.trigger, spawnEvent));
        } : null;
        Number number = numberAmount = this.amount != null ? (Number)this.amount.getSingle(event) : (Number)1;
        if (numberAmount != null) {
            double amount = ((Number)numberAmount).doubleValue();
            ?[] types = this.types.getArray(event);
            for (Location location : this.locations.getArray(event)) {
                for (Object type : types) {
                    if (type instanceof EntityType) {
                        EntityType entityType = (EntityType)type;
                        double typeAmount = amount * (double)entityType.getAmount();
                        int i = 0;
                        while ((double)i < typeAmount) {
                            if (consumer != null) {
                                entityType.data.spawn(location, consumer);
                            } else {
                                lastSpawned = entityType.data.spawn(location);
                            }
                            ++i;
                        }
                        continue;
                    }
                    if (!(type instanceof EntitySnapshot)) continue;
                    EntitySnapshot snapshot = (EntitySnapshot)type;
                    int i = 0;
                    while ((double)i < amount) {
                        Entity entity2 = snapshot.createEntity(location);
                        if (consumer != null) {
                            consumer.accept(entity2);
                        } else {
                            lastSpawned = entity2;
                        }
                        ++i;
                    }
                }
            }
        }
        return super.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "spawn " + (String)(this.amount != null ? this.amount.toString(event, debug) + " of " : "") + this.types.toString(event, debug) + " " + this.locations.toString(event, debug);
    }

    static {
        String acceptedTypes = "%entitytypes%";
        if (Skript.classExists("org.bukkit.entity.EntitySnapshot")) {
            acceptedTypes = "%entitytypes/entitysnapshots%";
        }
        Skript.registerSection(EffSecSpawn.class, "(spawn|summon) " + acceptedTypes + " [%directions% %locations%]", "(spawn|summon) %number% of " + acceptedTypes + " [%directions% %locations%]");
        EventValues.registerEventValue(SpawnEvent.class, Entity.class, SpawnEvent::getEntity);
    }

    public static class SpawnEvent
    extends Event {
        private final Entity entity;

        public SpawnEvent(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return this.entity;
        }

        @NotNull
        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}

