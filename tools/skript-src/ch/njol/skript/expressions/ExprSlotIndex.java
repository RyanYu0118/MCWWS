/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.slot.Slot;
import ch.njol.skript.util.slot.SlotWithIndex;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Nullable;

@Name(value="Slot Index")
@Description(value={"Index of an an inventory slot. Other types of slots may or may not have indices. Note that comparing slots with numbers is also possible; if index of slot is same as the number, comparisonsucceeds. This expression is mainly for the cases where you must for some reason save the slot numbers.", "", "Raw index of slot is unique for the view, see <a href=\"https://wiki.vg/Inventory\">Minecraft Wiki</a>"})
@Example.Examples(value={@Example(value="if index of event-slot is 10:\n\tsend \"You bought a pie!\"\n"), @Example(value="if display name of player's top inventory is \"Custom Menu\": # 3 rows inventory\n\tif raw index of event-slot > 27: # outside custom inventory\n\t\tcancel event\n")})
@Since(value={"2.2-dev35, 2.8.0 (raw index)"})
public class ExprSlotIndex
extends SimplePropertyExpression<Slot, Long> {
    private boolean isRaw;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isRaw = parseResult.hasTag("raw");
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Long convert(Slot slot) {
        if (slot instanceof SlotWithIndex) {
            SlotWithIndex slotWithIndex = (SlotWithIndex)slot;
            return this.isRaw ? slotWithIndex.getRawIndex() : slotWithIndex.getIndex();
        }
        return 0L;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "slot";
    }

    static {
        ExprSlotIndex.register(ExprSlotIndex.class, Long.class, "[raw:(raw|unique)] index", "slots");
    }
}

