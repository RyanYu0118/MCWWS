/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.config;

import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class NodeMap {
    private final Map<String, Node> map = new HashMap<String, Node>();

    public static boolean inMap(Node n) {
        return n instanceof EntryNode || n instanceof SectionNode;
    }

    private static String getKey(Node n) {
        String key = n.getKey();
        if (key == null) {
            assert (false) : n;
            return "";
        }
        return key.toLowerCase(Locale.ENGLISH);
    }

    private static String getKey(String key) {
        return key.toLowerCase(Locale.ENGLISH);
    }

    public void put(Node n) {
        if (!NodeMap.inMap(n)) {
            return;
        }
        this.map.put(NodeMap.getKey(n), n);
    }

    @Nullable
    public Node remove(Node n) {
        return this.remove(NodeMap.getKey(n));
    }

    @Nullable
    public Node remove(@Nullable String key) {
        if (key == null) {
            return null;
        }
        return this.map.remove(NodeMap.getKey(key));
    }

    @Nullable
    public Node get(@Nullable String key) {
        if (key == null) {
            return null;
        }
        return this.map.get(NodeMap.getKey(key));
    }
}

