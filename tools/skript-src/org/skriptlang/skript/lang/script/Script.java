/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.lang.script;

import ch.njol.skript.config.Config;
import ch.njol.skript.lang.util.common.AnyNamed;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.script.ScriptData;
import org.skriptlang.skript.lang.script.ScriptWarning;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.util.Validated;
import org.skriptlang.skript.util.event.EventRegistry;

public final class Script
implements Validated,
AnyNamed {
    private final Config config;
    private final List<Structure> structures;
    private final Set<ScriptWarning> suppressedWarnings = new HashSet<ScriptWarning>(ScriptWarning.values().length);
    private final Map<Class<? extends ScriptData>, ScriptData> scriptData = new ConcurrentHashMap<Class<? extends ScriptData>, ScriptData>(5);
    private final EventRegistry<Event> eventRegistry = new EventRegistry();

    @ApiStatus.Internal
    public Script(Config config, List<Structure> structures) {
        this.config = config;
        this.structures = structures;
    }

    public Config getConfig() {
        return this.config;
    }

    public @Unmodifiable List<Structure> getStructures() {
        return Collections.unmodifiableList(this.structures);
    }

    public void suppressWarning(ScriptWarning warning) {
        this.suppressedWarnings.add(warning);
    }

    public void allowWarning(ScriptWarning warning) {
        this.suppressedWarnings.remove((Object)warning);
    }

    public boolean suppressesWarning(ScriptWarning warning) {
        return this.suppressedWarnings.contains((Object)warning);
    }

    @ApiStatus.Experimental
    public void addData(ScriptData data) {
        this.scriptData.put(data.getClass(), data);
    }

    @ApiStatus.Experimental
    public void removeData(Class<? extends ScriptData> dataType) {
        this.scriptData.remove(dataType);
    }

    @ApiStatus.Experimental
    public void clearData() {
        this.scriptData.clear();
    }

    @ApiStatus.Experimental
    @Nullable
    public <Type extends ScriptData> Type getData(Class<Type> dataType) {
        return (Type)this.scriptData.get(dataType);
    }

    @ApiStatus.Experimental
    public <Value extends ScriptData> Value getData(Class<? extends Value> dataType, Supplier<Value> mapper) {
        return (Value)this.scriptData.computeIfAbsent(dataType, clazz -> (ScriptData)mapper.get());
    }

    @Override
    public String name() {
        return this.config.name();
    }

    public String nameAndPath() {
        String name = this.config.getFileName();
        if (name == null) {
            return null;
        }
        if (name.contains(".")) {
            return name.substring(0, name.lastIndexOf(46));
        }
        return name;
    }

    public EventRegistry<Event> eventRegistry() {
        return this.eventRegistry;
    }

    @Override
    public void invalidate() {
        this.config.invalidate();
    }

    @Override
    public boolean valid() {
        if (this.config.valid()) {
            @Nullable File file = this.config.getFile();
            return file == null || file.exists();
        }
        return false;
    }

    public static interface Event
    extends org.skriptlang.skript.util.event.Event {
    }
}

