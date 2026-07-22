/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Force Enchantment Glint")
@Description(value={"Forces the items to glint or not, or removes its existing enchantment glint enforcement."})
@Example.Examples(value={@Example(value="force {_items::*} to glint"), @Example(value="force the player's tool to stop glinting")})
@RequiredPlugins(value={"Spigot 1.20.5+"})
@Since(value={"2.10"})
public class EffForceEnchantmentGlint
extends Effect {
    private Expression<ItemType> itemTypes;
    private int pattern;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.itemTypes = expressions[0];
        this.pattern = matchedPattern;
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (ItemType itemType : this.itemTypes.getArray(event)) {
            ItemMeta meta = itemType.getItemMeta();
            Boolean glint = this.pattern == 0 ? Boolean.valueOf(true) : (this.pattern == 1 ? Boolean.valueOf(false) : null);
            meta.setEnchantmentGlintOverride(glint);
            itemType.setItemMeta(meta);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.pattern > 1) {
            return "clear the enchantment glint override of " + this.itemTypes.toString(event, debug);
        }
        return "force " + this.itemTypes.toString(event, debug) + " to " + (this.pattern == 0 ? "start" : "stop") + " glinting";
    }

    static {
        if (Skript.methodExists(ItemMeta.class, "setEnchantmentGlintOverride", Boolean.class)) {
            Skript.registerEffect(EffForceEnchantmentGlint.class, "(force|make) %itemtypes% [to] [start] glint[ing]", "(force|make) %itemtypes% [to] (not|stop) glint[ing]", "(clear|delete|reset) [the] enchantment glint override of %itemtypes%", "(clear|delete|reset) %itemtypes%'s enchantment glint override");
        }
    }
}

