/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.config;

import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.util.common.AnyValued;
import java.util.Arrays;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class EntryNode
extends Node
implements Map.Entry<String, String>,
AnyValued<String> {
    private String value;

    public EntryNode(String key, String value, String comment, SectionNode parent, int lineNum) {
        super(key, comment, parent, lineNum);
        this.value = value;
    }

    public EntryNode(String key, String value, SectionNode parent) {
        super(key, parent);
        this.value = value;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public @UnknownNullability String value() {
        return this.getValue();
    }

    @Override
    public String setValue(@Nullable String v) {
        if (v == null) {
            return this.value;
        }
        String r = this.value;
        this.value = v;
        return r;
    }

    @Override
    public void changeValue(String value) throws UnsupportedOperationException {
        this.setValue(value);
    }

    @Override
    public Class<String> valueType() {
        return String.class;
    }

    @Override
    public boolean supportsValueChange() {
        return false;
    }

    @Override
    String save_i() {
        return this.key + this.config.getSaveSeparator() + this.value;
    }

    @Override
    @Nullable
    public Node get(String step) {
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof EntryNode)) {
            return false;
        }
        EntryNode other = (EntryNode)object;
        return Arrays.equals(this.getPathSteps(), other.getPathSteps());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.getPathSteps());
    }
}

