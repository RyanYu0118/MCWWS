/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Sign
 *  org.bukkit.inventory.meta.BlockStateMeta
 *  org.bukkit.inventory.meta.ItemMeta
 */
package ch.njol.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.conditions.base.PropertyCondition;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

@Name(value="Has Glowing Text")
@Description(value={"Checks whether a sign (either a block or an item) has glowing text"})
@Example(value="if target block has glowing text")
@Since(value={"2.8.0"})
public class CondGlowingText
extends PropertyCondition<Object> {
    @Override
    public boolean check(Object obj) {
        ItemType itemType;
        ItemMeta meta;
        if (obj instanceof Block) {
            Sign sign;
            Block block = (Block)obj;
            BlockState state = block.getState();
            return state instanceof Sign && (sign = (Sign)state).isGlowingText();
        }
        if (obj instanceof ItemType && (meta = (itemType = (ItemType)obj).getItemMeta()) instanceof BlockStateMeta) {
            Sign sign;
            BlockStateMeta blockStateMeta = (BlockStateMeta)meta;
            BlockState state = blockStateMeta.getBlockState();
            return state instanceof Sign && (sign = (Sign)state).isGlowingText();
        }
        return false;
    }

    @Override
    protected PropertyCondition.PropertyType getPropertyType() {
        return PropertyCondition.PropertyType.HAVE;
    }

    @Override
    protected String getPropertyName() {
        return "glowing text";
    }

    static {
        if (Skript.methodExists(Sign.class, "isGlowingText", new Class[0])) {
            CondGlowingText.register(CondGlowingText.class, PropertyCondition.PropertyType.HAVE, "glowing text", "blocks/itemtypes");
        }
    }
}

