/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.player.PlayerHarvestBlockEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EventRestrictedSyntax;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Experience;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name(value="Drops")
@Description(value={"This expression only works in death and harvest events.", "In a death event, will hold the drops of the dying creature.", "Drops can be prevented by removing them with \"remove ... from drops\", e.g. \"remove all pickaxes from the drops\", or \"clear drops\" if you don't want any drops at all."})
@Example.Examples(value={@Example(value="clear drops"), @Example(value="remove 4 planks from the drops")})
@Since(value={"1.0"})
@Events(value={"death"})
public class ExprDrops
extends SimpleExpression<ItemType>
implements EventRestrictedSyntax {
    private boolean isDeathEvent;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (this.getParser().isCurrentEvent((Class<? extends Event>)EntityDeathEvent.class)) {
            this.isDeathEvent = true;
        }
        return true;
    }

    @Override
    public Class<? extends Event>[] supportedEvents() {
        return CollectionUtils.array(EntityDeathEvent.class, PlayerHarvestBlockEvent.class);
    }

    protected ItemType @Nullable [] get(Event event) {
        if (event instanceof EntityDeathEvent) {
            EntityDeathEvent entityDeathEvent = (EntityDeathEvent)event;
            return (ItemType[])entityDeathEvent.getDrops().stream().map(ItemType::new).toArray(ItemType[]::new);
        }
        if (event instanceof PlayerHarvestBlockEvent) {
            PlayerHarvestBlockEvent harvestBlockEvent = (PlayerHarvestBlockEvent)event;
            return (ItemType[])harvestBlockEvent.getItemsHarvested().stream().map(ItemType::new).toArray(ItemType[]::new);
        }
        assert (false);
        return new ItemType[0];
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (this.getParser().getHasDelayBefore().isTrue()) {
            Skript.error("Can't change the drops after the event has already passed");
            return null;
        }
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> {
                if (this.isDeathEvent) {
                    yield CollectionUtils.array(ItemType[].class, Inventory[].class, Experience[].class);
                }
                yield CollectionUtils.array(ItemType[].class, Inventory[].class);
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        List drops = null;
        int originalExperience = 0;
        if (event instanceof EntityDeathEvent) {
            EntityDeathEvent entityDeathEvent = (EntityDeathEvent)event;
            drops = entityDeathEvent.getDrops();
            originalExperience = entityDeathEvent.getDroppedExp();
        } else if (event instanceof PlayerHarvestBlockEvent) {
            PlayerHarvestBlockEvent harvestBlockEvent = (PlayerHarvestBlockEvent)event;
            drops = harvestBlockEvent.getItemsHarvested();
        } else {
            return;
        }
        assert (delta != null);
        int deltaExperience = -1;
        boolean removeAllExperience = false;
        ArrayList<ItemType> deltaDrops = new ArrayList<ItemType>();
        for (Object object : delta) {
            if (object instanceof Experience) {
                Experience experience = (Experience)object;
                if (experience.getInternalXP() == -1 && mode == Changer.ChangeMode.REMOVE || mode == Changer.ChangeMode.REMOVE_ALL) {
                    removeAllExperience = true;
                }
                if (deltaExperience == -1) {
                    deltaExperience = experience.getXP();
                    continue;
                }
                deltaExperience += experience.getXP();
                continue;
            }
            if (object instanceof Inventory) {
                Inventory inventory = (Inventory)object;
                for (ItemStack item : inventory.getContents()) {
                    if (item == null) continue;
                    deltaDrops.add(new ItemType(item));
                }
                continue;
            }
            if (object instanceof ItemType) {
                ItemType itemType = (ItemType)object;
                deltaDrops.add(itemType);
                continue;
            }
            assert (false);
        }
        if (deltaExperience > -1 && event instanceof EntityDeathEvent) {
            EntityDeathEvent entityDeathEvent = (EntityDeathEvent)event;
            switch (mode) {
                case SET: {
                    entityDeathEvent.setDroppedExp(deltaExperience);
                    break;
                }
                case ADD: {
                    entityDeathEvent.setDroppedExp(originalExperience + deltaExperience);
                    break;
                }
                case REMOVE: {
                    entityDeathEvent.setDroppedExp(originalExperience - deltaExperience);
                }
                case REMOVE_ALL: {
                    if (!removeAllExperience) break;
                    entityDeathEvent.setDroppedExp(0);
                    break;
                }
                case DELETE: 
                case RESET: {
                    assert (false);
                    break;
                }
            }
        }
        if (!deltaDrops.isEmpty()) {
            switch (mode) {
                case SET: {
                    drops.clear();
                }
                case ADD: {
                    for (ItemType item : deltaDrops) {
                        item.addTo(drops);
                    }
                    break;
                }
                case REMOVE: {
                    for (ItemType item : deltaDrops) {
                        item.removeFrom(false, drops);
                    }
                    break;
                }
                case REMOVE_ALL: {
                    for (ItemType item : deltaDrops) {
                        item.removeAll(false, drops);
                    }
                    break;
                }
                case DELETE: 
                case RESET: {
                    assert (false);
                    break;
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends ItemType> getReturnType() {
        return ItemType.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "the drops";
    }

    static {
        Skript.registerExpression(ExprDrops.class, ItemType.class, ExpressionType.SIMPLE, "[the] drops");
    }
}

