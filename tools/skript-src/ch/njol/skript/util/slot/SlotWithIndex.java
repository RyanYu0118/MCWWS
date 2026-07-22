/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.util.slot;

import ch.njol.skript.util.slot.Slot;

public abstract class SlotWithIndex
extends Slot {
    public abstract int getIndex();

    public int getRawIndex() {
        return this.getIndex();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean isSameSlot(Slot slot) {
        if (!(slot instanceof SlotWithIndex)) return false;
        SlotWithIndex slotWithIndex = (SlotWithIndex)slot;
        if (this.getRawIndex() != slotWithIndex.getRawIndex()) return false;
        return true;
    }
}

