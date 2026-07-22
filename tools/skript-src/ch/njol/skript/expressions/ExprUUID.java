/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

@Name(value="UUID")
@Description(value={"The UUID of a player, entity or world."})
@Example.Examples(value={@Example(value="# prevents people from joining the server if they use the name of a player\n# who has played on this server at least once since this script has been added\non login:\n\tif {uuid::%name of player%} exists:\n\t\t{uuid::%name of player%} is not uuid of player\n\t\tkick player due to \"Someone with your name has played on this server before\"\n\telse:\n\t\tset {uuid::%name of player%} to uuid of player\n"), @Example(value="command /what-is-my-uuid:\n\ttrigger:\n\t\tset {_uuid} to uuid of player\n\t\tsend \"Your UUID is '%string within {_uuid}%'\"\n")})
@Since(value={"2.1.2, 2.2 (offline players' uuids), 2.2-dev24 (other entities' uuids)"})
public class ExprUUID
extends SimplePropertyExpression<Object, UUID> {
    @Override
    @Nullable
    public UUID convert(Object object) {
        if (object instanceof OfflinePlayer) {
            OfflinePlayer player = (OfflinePlayer)object;
            try {
                return player.getUniqueId();
            }
            catch (UnsupportedOperationException e) {
                Skript.warning("A script tried to get uuid of an offline player, which was faked by another plugin (probably ProtocolLib).");
                e.printStackTrace();
                return null;
            }
        }
        if (object instanceof Entity) {
            Entity entity = (Entity)object;
            return entity.getUniqueId();
        }
        if (object instanceof World) {
            World world = (World)object;
            return world.getUID();
        }
        return null;
    }

    @Override
    public Class<? extends UUID> getReturnType() {
        return UUID.class;
    }

    @Override
    protected String getPropertyName() {
        return "UUID";
    }

    static {
        ExprUUID.register(ExprUUID.class, UUID.class, "UUID", "offlineplayers/worlds/entities");
    }
}

