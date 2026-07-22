/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

@Name(value="Block Hardness")
@Description(value={"Obtains the block's hardness level (also known as \"strength\"). This number is used to calculate the time required to break each block."})
@Example.Examples(value={@Example(value="set {_hard} to block hardness of target block"), @Example(value="if block hardness of target block > 5:")})
@RequiredPlugins(value={"Minecraft 1.13+"})
@Since(value={"2.6"})
public class ExprBlockHardness
extends SimplePropertyExpression<ItemType, Number> {
    @Override
    @Nullable
    public Number convert(ItemType itemType) {
        Material material = itemType.getMaterial();
        if (material.isBlock()) {
            return Float.valueOf(material.getHardness());
        }
        return null;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "block hardness";
    }

    static {
        if (Skript.methodExists(Material.class, "getHardness", new Class[0])) {
            ExprBlockHardness.register(ExprBlockHardness.class, Number.class, "[block] hardness", "itemtypes");
        }
    }
}

