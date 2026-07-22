/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.config;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.VoidNode;

public class InvalidNode
extends VoidNode {
    public InvalidNode(String value, String comment, SectionNode parent, int lineNum) {
        super(value, comment, parent, lineNum);
        ++this.config.errors;
    }
}

