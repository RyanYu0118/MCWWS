/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import java.util.Collections;
import java.util.Iterator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeNavigator
extends Iterable<Node> {
    @Override
    default public Iterator<Node> iterator() {
        return Collections.emptyIterator();
    }

    @Nullable
    public Node get(String var1);

    @NotNull
    public Node getCurrentNode();

    @Nullable
    default public Node getNodeAt(String ... steps) {
        Node node = this.getCurrentNode();
        for (String step : steps) {
            if (node == null) {
                return null;
            }
            node = node.get(step);
        }
        return node;
    }

    @Contract(value="null -> this")
    @Nullable
    default public Node getNodeAt(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return this.getCurrentNode();
        }
        if (!path.contains(".")) {
            return this.getNodeAt(new String[]{path});
        }
        return this.getNodeAt(path.split("\\."));
    }

    @Nullable
    default public String getValue(String path) {
        @Nullable Node node = this.getNodeAt(path);
        if (node instanceof EntryNode) {
            EntryNode entryNode = (EntryNode)node;
            return entryNode.getValue();
        }
        return null;
    }
}

