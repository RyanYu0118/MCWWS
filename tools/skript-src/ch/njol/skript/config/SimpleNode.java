/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.jetbrains.annotations.Nullable;

public class SimpleNode
extends Node {
    public SimpleNode(String value, String comment, int lineNum, SectionNode parent) {
        super(value, comment, parent, lineNum);
    }

    public SimpleNode(Config c) {
        super(c);
    }

    @Override
    String save_i() {
        return this.key;
    }

    public void set(String s) {
        this.key = s;
    }

    @Override
    @Nullable
    public Node get(String key) {
        return null;
    }
}

