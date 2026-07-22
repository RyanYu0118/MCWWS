/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.test.runner;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.test.runner.SkriptTestEvent;
import ch.njol.skript.test.runner.TestMode;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

public class StructTestEntryContainer
extends Structure {
    private EntryContainer entryContainer;
    static final /* synthetic */ boolean $assertionsDisabled;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        if (!$assertionsDisabled && entryContainer == null) {
            throw new AssertionError();
        }
        this.entryContainer = entryContainer;
        if (entryContainer.hasEntry("has entry") && entryContainer.hasEntry("has multiple entries")) {
            return true;
        }
        if (!$assertionsDisabled) {
            throw new AssertionError();
        }
        return false;
    }

    @Override
    public boolean load() {
        SectionNode section = (SectionNode)this.entryContainer.get("has entry", SectionNode.class, false);
        ArrayList<TriggerItem> triggerItems = ScriptLoader.loadItems(section);
        Script script = this.getParser().getCurrentScript();
        Trigger trigger = new Trigger(script, "entry container test", null, triggerItems);
        trigger.execute(new SkriptTestEvent());
        List multipleSections = this.entryContainer.getAll("has multiple entries", SectionNode.class, false);
        for (SectionNode multipleSection : multipleSections) {
            triggerItems = ScriptLoader.loadItems(multipleSection);
            trigger = new Trigger(script, "entry container test", null, triggerItems);
            trigger.execute(new SkriptTestEvent());
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "test entry container";
    }

    static {
        boolean bl = $assertionsDisabled = !StructTestEntryContainer.class.desiredAssertionStatus();
        if (TestMode.ENABLED) {
            Skript.registerStructure(StructTestEntryContainer.class, EntryValidator.builder().addSection("has entry", true).addSection("has multiple entries", true, true).build(), "test entry container");
        }
    }
}

