/*
 * Decompiled with CFR 0.152.
 */
package com.btk5h.skriptmirror.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V>
extends LinkedHashMap<K, V> {
    private final int cacheSize;

    public LRUCache(int cacheSize) {
        super(16, 0.75f, true);
        this.cacheSize = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return this.size() >= this.cacheSize;
    }
}

