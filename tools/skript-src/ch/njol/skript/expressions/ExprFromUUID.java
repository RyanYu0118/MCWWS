/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity/Player/World from UUID")
@Description(value={"Get an entity, player, offline player or world from a UUID.", "Unloaded entities or players that are offline (when using 'player from %uuid%') will return nothing."})
@Example.Examples(value={@Example(value="set {_player} to player from \"a0789aeb-7b46-43f6-86fb-cb671fed5775\" parsed as uuid"), @Example(value="set {_offline player} to offline player from {_some uuid}"), @Example(value="set {_entity} to entity from {_some uuid}"), @Example(value="set {_world} to world from {_some uuid}")})
@Since(value={"2.11"})
public class ExprFromUUID
extends SimpleExpression<Object> {
    private Expression<UUID> uuids;
    private boolean player;
    private boolean offline;
    private boolean world;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.uuids = exprs[0];
        this.player = matchedPattern == 0;
        this.offline = parseResult.hasTag("offline");
        this.world = matchedPattern == 2;
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ArrayList<Object> entities = new ArrayList<Object>();
        for (UUID uuid : this.uuids.getArray(event)) {
            if (this.player) {
                if (this.offline) {
                    entities.add(Bukkit.getOfflinePlayer((UUID)uuid));
                    continue;
                }
                Player player = Bukkit.getPlayer((UUID)uuid);
                if (player == null) continue;
                entities.add(player);
                continue;
            }
            if (this.world) {
                World world = Bukkit.getWorld((UUID)uuid);
                if (world == null) continue;
                entities.add(world);
                continue;
            }
            Entity entity = Bukkit.getEntity((UUID)uuid);
            if (entity == null) continue;
            entities.add(entity);
        }
        if (this.player) {
            if (this.offline) {
                return entities.toArray(OfflinePlayer[]::new);
            }
            return entities.toArray(Player[]::new);
        }
        if (this.world) {
            return entities.toArray(World[]::new);
        }
        return entities.toArray(Entity[]::new);
    }

    @Override
    public boolean isSingle() {
        return this.uuids.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        if (this.world) {
            return World.class;
        }
        if (this.player) {
            if (this.offline) {
                return OfflinePlayer.class;
            }
            return Player.class;
        }
        return Entity.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.world) {
            builder.append((Object)"worlds");
        } else if (this.player) {
            if (this.offline) {
                builder.append((Object)"offline");
            }
            builder.append((Object)"players");
        } else {
            builder.append((Object)"entities");
        }
        builder.append("from", this.uuids);
        return builder.toString();
    }

    static {
        Skript.registerExpression(ExprFromUUID.class, Object.class, ExpressionType.PROPERTY, "[:offline[ ]]player[s] from %uuids%", "entit(y|ies) from %uuids%", "world[s] from %uuids%");
    }
}

