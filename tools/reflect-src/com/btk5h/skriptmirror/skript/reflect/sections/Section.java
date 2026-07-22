/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.Variable
 *  org.bukkit.event.Event
 */
package com.btk5h.skriptmirror.skript.reflect.sections;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import com.btk5h.skriptmirror.skript.reflect.sections.SectionEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import java.util.List;
import org.bukkit.event.Event;

public class Section {
    private final Trigger trigger;
    private final Object variablesMap;
    private final List<Variable<?>> argumentVariables;
    private Object[] output;

    public Section(Trigger trigger, Object variablesMap, List<Variable<?>> argumentVariables) {
        this.trigger = trigger;
        this.variablesMap = variablesMap;
        this.argumentVariables = argumentVariables;
    }

    public Section(Trigger trigger, Event event, List<Variable<?>> argumentVariables) {
        this(trigger, SkriptReflection.copyLocals(SkriptReflection.getLocals(event)), argumentVariables);
    }

    public SectionEvent run(Object[][] arguments) {
        this.output = null;
        SectionEvent sectionEvent = new SectionEvent(this);
        SkriptReflection.putLocals(SkriptReflection.copyLocals(this.variablesMap), sectionEvent);
        for (int i = 0; i < arguments.length && i < this.argumentVariables.size(); ++i) {
            Object[] currentArg = arguments[i];
            if (currentArg.length == 0) {
                this.argumentVariables.get(i).change((Event)sectionEvent, null, Changer.ChangeMode.DELETE);
                continue;
            }
            this.argumentVariables.get(i).change((Event)sectionEvent, currentArg, Changer.ChangeMode.SET);
        }
        TriggerItem.walk((TriggerItem)this.trigger, (Event)sectionEvent);
        SkriptReflection.removeLocals(sectionEvent);
        return sectionEvent;
    }

    public Object[] getOutput() {
        return this.output;
    }

    public void setOutput(Object[] output) {
        this.output = output;
    }
}

