/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package com.btk5h.skriptmirror.skript.reflect.sections;

import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SectionEvent
extends Event {
    private final Section section;
    private Object[] output;

    public SectionEvent(Section section) {
        this.section = section;
    }

    public Section getSection() {
        return this.section;
    }

    public Object[] getOutput() {
        return this.output;
    }

    public void setOutput(Object[] output) {
        this.output = output;
    }

    public HandlerList getHandlers() {
        throw new IllegalStateException();
    }
}

