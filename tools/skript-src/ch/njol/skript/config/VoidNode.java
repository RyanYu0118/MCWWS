/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.jetbrains.annotations.Nullable;

public class VoidNode
extends Node {
    VoidNode(String line, String comment, SectionNode parent, int lineNum) {
        super(line.trim(), comment, parent, lineNum);
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public void set(String s) {
        this.key = s;
    }

    @Override
    String save_i() {
        return this.key;
    }

    @Override
    @Nullable
    public Node get(String key) {
        return null;
    }
}

