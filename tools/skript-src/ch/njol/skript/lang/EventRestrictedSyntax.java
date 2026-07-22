/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 */
package ch.njol.skript.lang;

import org.bukkit.event.Event;

@FunctionalInterface
public interface EventRestrictedSyntax {
    public Class<? extends Event>[] supportedEvents();
}

