/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.expressions.base.EventValueExpression
 *  ch.njol.skript.registrations.Classes
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.registrations.Classes;
import java.lang.reflect.Array;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.event.elements.CustomEvent;
import org.skriptlang.reflect.syntax.event.elements.CustomEventUtils;

public class ExprReplacedEventValue<T>
extends EventValueExpression<T> {
    private final EventValueExpression<T> original;

    public ExprReplacedEventValue(EventValueExpression<T> original) {
        super(original.getReturnType());
        this.original = original;
    }

    @Nullable
    protected T[] get(Event e) {
        if (e instanceof BukkitCustomEvent || e instanceof EventTriggerEvent) {
            BukkitCustomEvent bukkitCustomEvent = e instanceof BukkitCustomEvent ? (BukkitCustomEvent)e : (BukkitCustomEvent)((EventTriggerEvent)e).getDirectEvent();
            Class valueClass = this.original.getReturnType();
            Object[] tArray = (Object[])Array.newInstance(valueClass, 1);
            tArray[0] = bukkitCustomEvent.getEventValue(Classes.getSuperClassInfo((Class)valueClass));
            return tArray;
        }
        return this.original.getArray(e);
    }

    public boolean init() {
        if (this.getParser().isCurrentEvent(new Class[]{BukkitCustomEvent.class, EventTriggerEvent.class})) {
            EventSyntaxInfo which = CustomEvent.lastWhich;
            ClassInfo classInfo = Classes.getSuperClassInfo((Class)this.getReturnType());
            return CustomEventUtils.hasEventValue(which, classInfo);
        }
        return this.original.init();
    }

    public String toString(@Nullable Event e, boolean debug) {
        return this.original.toString(e, debug);
    }

    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return this.original.acceptChange(mode);
    }

    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        this.original.change(e, delta, mode);
    }

    public boolean setTime(int time) {
        return this.original.setTime(time);
    }
}

