/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.classes.Changer$ChangerUtils
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.expressions.base.EventValueExpression
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.registration.DefaultSyntaxInfos$Expression
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import java.lang.reflect.Array;
import java.util.regex.MatchResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.event.elements.CustomEvent;
import org.skriptlang.reflect.syntax.event.elements.CustomEventUtils;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class ExprCustomEventValue<T>
extends EventValueExpression<T> {
    private ClassInfo<? super T> classInfo;
    private Changer<? super T> changer;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, (SyntaxInfo)((DefaultSyntaxInfos.Expression)DefaultSyntaxInfos.Expression.builder(ExprCustomEventValue.class, Object.class).addPattern("[the] [event-]<.+>").supplier(ExprCustomEventValue::new).priority(SkriptMirror.SHADOW_REALM).build()));
    }

    public ExprCustomEventValue() {
        super(Object.class);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!this.getParser().isCurrentEvent(new Class[]{BukkitCustomEvent.class, EventTriggerEvent.class})) {
            return false;
        }
        EventSyntaxInfo which = CustomEvent.lastWhich;
        if (which == null) {
            return false;
        }
        String stringClass = ((MatchResult)parseResult.regexes.get(0)).group();
        this.classInfo = Classes.getClassInfoFromUserInput((String)stringClass);
        if (this.classInfo == null) {
            return false;
        }
        if (!CustomEventUtils.hasEventValue(which, this.classInfo)) {
            Skript.error((String)("There is no " + CustomEventUtils.getName(this.classInfo) + " in the custom event " + CustomEventUtils.getName(which)));
            return false;
        }
        return true;
    }

    public T[] get(Event event) {
        if (!(event instanceof BukkitCustomEvent) && !(event instanceof EventTriggerEvent)) {
            return null;
        }
        BukkitCustomEvent bukkitCustomEvent = event instanceof BukkitCustomEvent ? (BukkitCustomEvent)event : (BukkitCustomEvent)((EventTriggerEvent)event).getDirectEvent();
        Object[] tArray = (Object[])Array.newInstance(this.classInfo.getC(), 1);
        tArray[0] = bukkitCustomEvent.getEventValue(this.classInfo);
        return tArray;
    }

    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        this.changer = this.classInfo.getChanger();
        return this.changer == null ? null : this.changer.acceptChange(mode);
    }

    public void change(Event e, Object[] delta, Changer.ChangeMode mode) {
        if (this.changer == null) {
            throw new UnsupportedOperationException();
        }
        Changer.ChangerUtils.change(this.changer, (Object[])this.getArray(e), (Object[])delta, (Changer.ChangeMode)mode);
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "event-" + this.classInfo.getCodeName();
    }

    public Class<T> getReturnType() {
        return this.classInfo.getC();
    }
}

