/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.ScriptLoader
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.lang.entry.EntryData
 */
package org.skriptlang.reflect.syntax;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

class PatternsEntryData
extends EntryData<List<String>> {
    public PatternsEntryData(String key, @Nullable List<String> defaultValue, boolean optional) {
        super(key, defaultValue, optional);
    }

    public List<String> getValue(Node node) {
        ArrayList<String> patterns = new ArrayList<String>();
        for (Node subNode : (SectionNode)node) {
            String key = subNode.getKey();
            if (key == null) continue;
            patterns.add(key);
        }
        return patterns;
    }

    public boolean canCreateWith(Node node) {
        if (!(node instanceof SectionNode)) {
            return false;
        }
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        key = ScriptLoader.replaceOptions((String)key);
        return this.getKey().equalsIgnoreCase(key);
    }
}

