/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import org.bukkit.event.Event;

public interface VerboseAssert {
    public String getExpectedMessage(Event var1);

    public String getReceivedMessage(Event var1);

    public static String getExpressionValue(Expression<?> expression, Event event) {
        return Classes.toString(expression.getAll(event), expression.getAnd());
    }
}

