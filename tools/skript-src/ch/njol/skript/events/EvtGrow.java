/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.TreeType
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockGrowEvent
 *  org.bukkit.event.world.StructureGrowEvent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.LiteralList;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.StructureType;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.jetbrains.annotations.Nullable;

public class EvtGrow
extends SkriptEvent {
    private static final int ANY = 0;
    private static final int STRUCTURE = 1;
    private static final int BLOCK = 2;
    private static final int OF = 0;
    private static final int FROM = 1;
    private static final int INTO = 2;
    private static final int FROM_INTO = 3;
    @Nullable
    private Literal<Object> toTypes;
    @Nullable
    private Literal<Object> fromTypes;
    private int eventRestriction;
    private int actionRestriction;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        this.eventRestriction = parseResult.mark;
        this.actionRestriction = matchedPattern;
        switch (this.actionRestriction) {
            case 0: {
                if (this.eventRestriction == 1) {
                    this.fromTypes = args[0];
                    break;
                }
                if (this.eventRestriction != 2) break;
                this.fromTypes = args[1];
                break;
            }
            case 1: {
                this.fromTypes = args[0];
                break;
            }
            case 2: {
                if (this.eventRestriction == 1) {
                    this.toTypes = args[0];
                    break;
                }
                if (this.eventRestriction != 2) break;
                this.toTypes = args[1];
                break;
            }
            case 3: {
                this.fromTypes = args[0];
                if (this.eventRestriction == 1) {
                    this.toTypes = args[1];
                    break;
                }
                if (this.eventRestriction != 2) break;
                this.toTypes = args[2];
                break;
            }
            default: {
                assert (false);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean check(Event event) {
        if (this.fromTypes == null && this.actionRestriction != 2) {
            return this.actionRestriction == 0;
        }
        if (this.eventRestriction == 1 && !(event instanceof StructureGrowEvent)) {
            return false;
        }
        if (this.eventRestriction == 2 && !(event instanceof BlockGrowEvent) && this.actionRestriction != 0) {
            return false;
        }
        switch (this.actionRestriction) {
            case 0: {
                return EvtGrow.checkFrom(event, this.fromTypes) || EvtGrow.checkTo(event, this.fromTypes);
            }
            case 1: {
                return EvtGrow.checkFrom(event, this.fromTypes);
            }
            case 2: {
                return EvtGrow.checkTo(event, this.toTypes);
            }
            case 3: {
                return EvtGrow.checkFrom(event, this.fromTypes) && EvtGrow.checkTo(event, this.toTypes);
            }
        }
        assert (false);
        return false;
    }

    private static boolean checkFrom(Event event, Literal<Object> types) {
        if (types.getAnd() && types instanceof LiteralList) {
            ((LiteralList)types).invertAnd();
        }
        if (event instanceof StructureGrowEvent) {
            Material sapling = ItemUtils.getTreeSapling(((StructureGrowEvent)event).getSpecies());
            return types.check(event, type -> {
                if (type instanceof ItemType) {
                    return ((ItemType)type).isOfType(sapling);
                }
                if (type instanceof BlockData) {
                    return ((BlockData)type).getMaterial() == sapling;
                }
                return false;
            });
        }
        if (event instanceof BlockGrowEvent) {
            BlockState oldState = ((BlockGrowEvent)event).getBlock().getState();
            return types.check(event, type -> {
                if (type instanceof ItemType) {
                    return ((ItemType)type).isOfType(oldState.getBlockData());
                }
                if (type instanceof BlockData) {
                    return ((BlockData)type).matches(oldState.getBlockData());
                }
                return false;
            });
        }
        return false;
    }

    private static boolean checkTo(Event event, Literal<Object> types) {
        if (types.getAnd() && types instanceof LiteralList) {
            ((LiteralList)types).invertAnd();
        }
        if (event instanceof StructureGrowEvent) {
            TreeType species = ((StructureGrowEvent)event).getSpecies();
            return types.check(event, type -> {
                if (type instanceof StructureType) {
                    return ((StructureType)((Object)((Object)type))).is(species);
                }
                return false;
            });
        }
        if (event instanceof BlockGrowEvent) {
            BlockState newState = ((BlockGrowEvent)event).getNewState();
            return types.check(event, type -> {
                if (type instanceof ItemType) {
                    return ((ItemType)type).isOfType(newState.getBlockData());
                }
                if (type instanceof BlockData) {
                    return ((BlockData)type).matches(newState.getBlockData());
                }
                return false;
            });
        }
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (this.fromTypes == null && this.toTypes == null) {
            return "grow";
        }
        switch (this.actionRestriction) {
            case 0: {
                return "grow of " + this.fromTypes.toString(event, debug);
            }
            case 1: {
                return "grow from " + this.fromTypes.toString(event, debug);
            }
            case 2: {
                return "grow into " + this.toTypes.toString(event, debug);
            }
            case 3: {
                return "grow from " + this.fromTypes.toString(event, debug) + " into " + this.toTypes.toString(event, debug);
            }
        }
        return "grow";
    }

    static {
        Skript.registerEvent("Grow", EvtGrow.class, CollectionUtils.array(StructureGrowEvent.class, BlockGrowEvent.class), "grow[th] [of (1:%-structuretypes%|2:%-itemtypes/blockdatas%)]", "grow[th] from %itemtypes/blockdatas%", "grow[th] [in]to (1:%structuretypes%|2:%itemtypes/blockdatas%)", "grow[th] from %itemtypes/blockdatas% [in]to (1:%structuretypes%|2:%itemtypes/blockdatas%)").description("Called when a tree, giant mushroom or plant grows to next stage.", "\"of\" matches any grow event, \"from\" matches only the old state, \"into\" matches only the new state,and \"from into\" requires matching both the old and new states.", "Using \"and\" lists in this event is equivalent to using \"or\" lists. The event will trigger if any one of the elements is what grew.").examples("on grow:", "on grow of tree:", "on grow of wheat[age=7]:", "on grow from a sapling:", "on grow into tree:", "on grow from a sapling into tree:", "on grow of wheat, carrots, or potatoes:", "on grow into tree, giant mushroom, cactus:", "on grow from wheat[age=0] to wheat[age=1] or wheat[age=2]:").since("1.0, 2.2-dev20 (plants), 2.8.0 (from, into, blockdata)");
    }
}

