/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

public class SectionEntryData
extends EntryData<SectionNode> {
    public SectionEntryData(String key, @Nullable SectionNode defaultValue, boolean optional) {
        super(key, defaultValue, optional);
    }

    public SectionEntryData(String key, @Nullable SectionNode defaultValue, boolean optional, boolean multiple) {
        super(key, defaultValue, optional, multiple);
    }

    @Override
    @Nullable
    public SectionNode getValue(Node node) {
        assert (node instanceof SectionNode);
        return (SectionNode)node;
    }

    @Override
    public boolean canCreateWith(Node node) {
        if (!(node instanceof SectionNode)) {
            return false;
        }
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        key = ScriptLoader.replaceOptions(key);
        return this.getKey().equalsIgnoreCase(key);
    }
}

