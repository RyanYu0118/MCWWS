/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class SectionSkriptEvent
extends SkriptEvent {
    private final String name;
    private final Section section;

    public SectionSkriptEvent(String name, Section section) {
        this.name = name;
        this.section = section;
    }

    public Section getSection() {
        return this.section;
    }

    public final boolean isSection(Class<? extends Section> section) {
        return section.isInstance(this.section);
    }

    @SafeVarargs
    public final boolean isSection(Class<? extends Section> ... sections) {
        for (Class<? extends Section> section : sections) {
            if (!this.isSection(section)) continue;
            return true;
        }
        return false;
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult) {
        throw new SkriptAPIException("init should never be called for a SectionSkriptEvent.");
    }

    @Override
    public boolean check(Event event) {
        throw new SkriptAPIException("check should never be called for a SectionSkriptEvent.");
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.name;
    }
}

