/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.PlayerPickBlockEvent
 *  io.papermc.paper.event.player.PlayerPickEntityEvent
 *  io.papermc.paper.event.player.PlayerPickItemEvent
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.player.elements.expressions;

import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import java.util.Locale;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Picked Item/Block/Entity")
@Description(value={"The item, block, or entity picked by a player using the pick block key (default middle mouse button)."})
@Example(value="on player pick item:\n\tsend \"You picked %the picked item%!\" to the player\n")
@Since(value={"2.15"})
@RequiredPlugins(value={"1.21.5+"})
@Keywords(value={"pick", "picked", "picked item", "picked block", "picked entity"})
public class ExprPickedItem
extends SimpleExpression<Object>
implements EventRestrictedSyntax {
    private PickType pickType;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprPickedItem.class, Object.class).supplier(ExprPickedItem::new)).addPattern("[the] picked (item|1:block|2:entity)")).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.pickType = PickType.values()[parseResult.mark];
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        ItemType[] itemTypeArray;
        switch (this.pickType.ordinal()) {
            default: {
                throw new MatchException(null, null);
            }
            case 0: {
                if (event instanceof PlayerPickBlockEvent) {
                    PlayerPickBlockEvent pickBlockEvent = (PlayerPickBlockEvent)event;
                    ItemType[] itemTypeArray2 = new ItemType[1];
                    itemTypeArray = itemTypeArray2;
                    itemTypeArray2[0] = new ItemType(pickBlockEvent.getBlock());
                    break;
                }
                if (event instanceof PlayerPickEntityEvent) {
                    PlayerPickEntityEvent pickEntityEvent = (PlayerPickEntityEvent)event;
                    ItemType[] itemTypeArray3 = new ItemType[1];
                    itemTypeArray = itemTypeArray3;
                    itemTypeArray3[0] = new ItemType(pickEntityEvent.getEntity().getPickItemStack());
                    break;
                }
                itemTypeArray = null;
                break;
            }
            case 1: {
                if (event instanceof PlayerPickBlockEvent) {
                    PlayerPickBlockEvent pickBlockEvent = (PlayerPickBlockEvent)event;
                    Block[] blockArray = new Block[1];
                    itemTypeArray = blockArray;
                    blockArray[0] = pickBlockEvent.getBlock();
                    break;
                }
                itemTypeArray = null;
                break;
            }
            case 2: {
                if (event instanceof PlayerPickEntityEvent) {
                    PlayerPickEntityEvent pickEntityEvent = (PlayerPickEntityEvent)event;
                    Entity[] entityArray = new Entity[1];
                    itemTypeArray = entityArray;
                    entityArray[0] = pickEntityEvent.getEntity();
                    break;
                }
                itemTypeArray = null;
            }
        }
        return itemTypeArray;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return switch (this.pickType.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> ItemType.class;
            case 1 -> Block.class;
            case 2 -> Entity.class;
        };
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(PlayerPickItemEvent.class);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the picked " + this.pickType.name().toLowerCase(Locale.ENGLISH);
    }

    private static enum PickType {
        ITEM,
        BLOCK,
        ENTITY;

    }
}

