/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.destroystokyo.paper.profile.PlayerProfile
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Skull
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.profile.PlayerProfile
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.coll.CollectionUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Skull Owner")
@Description(value={"The skull owner of a player skull."})
@Example.Examples(value={@Example(value="set {_owner} to the skull owner of event-block"), @Example(value="set skull owner of {_block} to \"Njol\" parsed as offlineplayer"), @Example(value="set head owner of player's tool to {_player}")})
@Since(value={"2.9.0, 2.10 (of items)"})
public class ExprSkullOwner
extends SimplePropertyExpression<Object, OfflinePlayer> {
    @Override
    @Nullable
    public OfflinePlayer convert(Object object) {
        ItemMeta itemMeta;
        Block block;
        BlockState blockState;
        if (object instanceof Block && (blockState = (block = (Block)object).getState()) instanceof Skull) {
            Skull skull = (Skull)blockState;
            return this.getOfflinePlayer(skull.getPlayerProfile());
        }
        ItemStack skullItem = ItemUtils.asItemStack(object);
        if (skullItem == null || !((itemMeta = skullItem.getItemMeta()) instanceof SkullMeta)) {
            return null;
        }
        SkullMeta skullMeta = (SkullMeta)itemMeta;
        return this.getOfflinePlayer(skullMeta.getPlayerProfile());
    }

    @Nullable
    private OfflinePlayer getOfflinePlayer(PlayerProfile profile) {
        if (profile == null) {
            return null;
        }
        UUID uuid = profile.getId();
        if (uuid != null) {
            return Bukkit.getOfflinePlayer((UUID)uuid);
        }
        String name = profile.getName();
        if (name != null) {
            return Bukkit.getOfflinePlayer((String)name);
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(OfflinePlayer.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        OfflinePlayer offlinePlayer = (OfflinePlayer)delta[0];
        Consumer<Skull> blockChanger = this.getBlockChanger(offlinePlayer);
        Consumer<SkullMeta> metaChanger = this.getMetaChanger(offlinePlayer);
        for (Object object : this.getExpr().getArray(event)) {
            ItemMeta itemMeta;
            Block block;
            BlockState blockState;
            if (object instanceof Block && (blockState = (block = (Block)object).getState()) instanceof Skull) {
                Skull skull = (Skull)blockState;
                blockChanger.accept(skull);
                skull.update(true, false);
                continue;
            }
            ItemStack skullItem = ItemUtils.asItemStack(object);
            if (skullItem == null || !((itemMeta = skullItem.getItemMeta()) instanceof SkullMeta)) continue;
            SkullMeta skullMeta = (SkullMeta)itemMeta;
            metaChanger.accept(skullMeta);
            if (object instanceof Slot) {
                Slot slot = (Slot)object;
                skullItem.setItemMeta((ItemMeta)skullMeta);
                slot.setItem(skullItem);
                continue;
            }
            if (object instanceof ItemType) {
                ItemType itemType = (ItemType)object;
                itemType.setItemMeta((ItemMeta)skullMeta);
                continue;
            }
            if (!(object instanceof ItemStack)) continue;
            ItemStack itemStack = (ItemStack)object;
            itemStack.setItemMeta((ItemMeta)skullMeta);
        }
    }

    private Consumer<Skull> getBlockChanger(OfflinePlayer offlinePlayer) {
        if (offlinePlayer.getName() != null) {
            return skull -> skull.setOwningPlayer(offlinePlayer);
        }
        if (ItemUtils.CAN_CREATE_PLAYER_PROFILE) {
            org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile((UUID)offlinePlayer.getUniqueId(), (String)"");
            return skull -> skull.setOwnerProfile(profile);
        }
        return skull -> skull.setOwner("");
    }

    private Consumer<SkullMeta> getMetaChanger(OfflinePlayer offlinePlayer) {
        if (offlinePlayer.getName() != null) {
            return skullMeta -> skullMeta.setOwningPlayer(offlinePlayer);
        }
        if (ItemUtils.CAN_CREATE_PLAYER_PROFILE) {
            org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile((UUID)offlinePlayer.getUniqueId(), (String)"");
            return skullMeta -> skullMeta.setOwnerProfile(profile);
        }
        return skullMeta -> skullMeta.setOwner("");
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    protected String getPropertyName() {
        return "skull owner";
    }

    static {
        ExprSkullOwner.register(ExprSkullOwner.class, OfflinePlayer.class, "(head|skull) owner", "slots/itemtypes/itemstacks/blocks");
    }
}

