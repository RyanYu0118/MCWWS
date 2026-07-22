/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.ScriptLoader
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SimpleNode
 *  ch.njol.skript.registrations.Classes
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.lang.entry.KeyValueEntryData
 */
package org.skriptlang.reflect.syntax.event;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.registrations.Classes;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

public class EventValuesEntryData
extends KeyValueEntryData<List<ClassInfo<?>>> {
    private static final String listSplitPattern = "\\s*,?\\s+(and|n?or)\\s+|\\s*,\\s*";

    public EventValuesEntryData(String key, @Nullable List<ClassInfo<?>> defaultValue, boolean optional) {
        super(key, defaultValue, optional);
    }

    @Nullable
    protected List<ClassInfo<?>> getValue(String value) {
        String[] stringClasses = value.split(listSplitPattern);
        ArrayList classInfos = new ArrayList(stringClasses.length);
        for (String stringClass : stringClasses) {
            ClassInfo classInfo = Classes.getClassInfoFromUserInput((String)stringClass);
            if (classInfo == null) {
                Skript.error((String)("The type " + stringClass + " doesn't exist"));
                return null;
            }
            classInfos.add(classInfo);
        }
        return classInfos;
    }

    public final boolean canCreateWith(Node node) {
        if (!(node instanceof SimpleNode)) {
            return false;
        }
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        return this.canCreateWith(ScriptLoader.replaceOptions((String)key));
    }

    protected boolean canCreateWith(String node) {
        return node.startsWith(this.getKey() + this.getSeparator());
    }
}

