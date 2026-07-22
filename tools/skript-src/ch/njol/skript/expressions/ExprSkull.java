/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

@Name(value="Player Skull")
@Description(value={"Gets a skull item representing a player. Skulls for other entities are provided by the aliases."})
@Example.Examples(value={@Example(value="give the victim's skull to the attacker"), @Example(value="set the block at the entity to the entity's skull")})
@Since(value={"2.0"})
public class ExprSkull
extends SimplePropertyExpression<OfflinePlayer, ItemType> {
    @Override
    @Nullable
    public ItemType convert(OfflinePlayer player) {
        ItemType skull = new ItemType(Material.PLAYER_HEAD);
        ItemUtils.setHeadOwner(skull, player);
        return skull;
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    protected String getPropertyName() {
        return "skull";
    }

    static {
        ExprSkull.register(ExprSkull.class, ItemType.class, "skull", "offlineplayers");
    }
}

