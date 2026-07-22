/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry.util;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

public class TriggerEntryData
extends EntryData<Trigger> {
    public TriggerEntryData(String key, @Nullable Trigger defaultValue, boolean optional) {
        super(key, defaultValue, optional);
    }

    public TriggerEntryData(String key, @Nullable Trigger defaultValue, boolean optional, boolean multiple) {
        super(key, defaultValue, optional, multiple);
    }

    @Override
    @Nullable
    public Trigger getValue(Node node) {
        assert (node instanceof SectionNode);
        return new Trigger(ParserInstance.get().getCurrentScript(), "entry with key: " + this.getKey(), new SimpleEvent(), ScriptLoader.loadItems((SectionNode)node));
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

