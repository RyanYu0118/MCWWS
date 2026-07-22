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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.ContainerEntryData;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

public class StructTestSubValidators
extends Structure {
    private EntryContainer entryContainer;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        this.entryContainer = entryContainer;
        return true;
    }

    @Override
    public boolean load() {
        EntryContainer subEntry1 = (EntryContainer)this.entryContainer.get("sub validator 1", EntryContainer.class, false);
        SectionNode section1 = (SectionNode)subEntry1.get("sub section", SectionNode.class, false);
        EntryContainer subEntry2 = (EntryContainer)this.entryContainer.get("sub validator 2", EntryContainer.class, false);
        EntryContainer subSubEntry = (EntryContainer)subEntry2.get("sub sub validator", EntryContainer.class, false);
        SectionNode section2 = (SectionNode)subSubEntry.get("sub sub section", SectionNode.class, false);
        ArrayList<TriggerItem> items1 = ScriptLoader.loadItems(section1);
        ArrayList<TriggerItem> items2 = ScriptLoader.loadItems(section2);
        Script script = this.getParser().getCurrentScript();
        Trigger trigger1 = new Trigger(script, "sub section", null, items1);
        Trigger trigger2 = new Trigger(script, "sub sub section", null, items2);
        trigger1.execute(new SkriptTestEvent());
        trigger2.execute(new SkriptTestEvent());
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "test sub entry validators";
    }

    static {
        if (TestMode.ENABLED) {
            Skript.registerStructure(StructTestSubValidators.class, EntryValidator.builder().addEntryData(new ContainerEntryData("sub validator 1", false, EntryValidator.builder().addSection("sub section", false))).addEntryData(new ContainerEntryData("sub validator 2", false, EntryValidator.builder().addEntryData(new ContainerEntryData("sub sub validator", false, EntryValidator.builder().addSection("sub sub section", false))))).build(), "test sub validators");
        }
    }
}

