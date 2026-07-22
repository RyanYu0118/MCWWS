/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.registrations.Classes
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 */
package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitCustomEvent
extends Event
implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String name;
    private final Map<ClassInfo<?>, Object> eventValueMap;
    private final Map<String, Object> dataMap;
    private boolean isCancelled = false;

    public BukkitCustomEvent(String name) {
        this(name, !Bukkit.isPrimaryThread());
    }

    public BukkitCustomEvent(String name, boolean isAsync) {
        super(isAsync);
        this.name = name;
        this.eventValueMap = new HashMap();
        this.dataMap = new HashMap<String, Object>();
    }

    public String getName() {
        return this.name;
    }

    public void setEventValue(ClassInfo<?> classInfo, Object value) {
        if (classInfo != null && classInfo.getC().isInstance(value)) {
            this.eventValueMap.put(classInfo, value);
        }
    }

    public void setEventValue(String type, Object value) {
        this.setEventValue(Classes.getClassInfoFromUserInput((String)type), value);
    }

    public Object getEventValue(ClassInfo<?> classInfo) {
        Class clazz = classInfo.getC();
        ClassInfo<?> chosenClassInfo = null;
        for (ClassInfo<?> classInfoKey : this.eventValueMap.keySet()) {
            if (!clazz.isAssignableFrom(classInfoKey.getC())) continue;
            chosenClassInfo = classInfoKey;
            break;
        }
        return chosenClassInfo == null ? null : this.getExactEventValue(chosenClassInfo);
    }

    public Object getExactEventValue(ClassInfo<?> classInfo) {
        return this.eventValueMap.get(classInfo);
    }

    public Object getEventValue(String type) {
        ClassInfo classInfo = Classes.getClassInfoFromUserInput((String)type);
        return classInfo == null ? null : this.getEventValue(classInfo);
    }

    public void setData(String key, Object value) {
        this.dataMap.put(key, value);
    }

    public Object getData(String key) {
        return this.dataMap.get(key);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public boolean isCancelled() {
        return this.isCancelled;
    }

    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }
}

