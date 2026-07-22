/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.log;

import ch.njol.skript.log.LogHandler;
import java.util.Iterator;
import java.util.LinkedList;
import org.jetbrains.annotations.Nullable;

public class HandlerList
implements Iterable<LogHandler> {
    private final LinkedList<LogHandler> list = new LinkedList();

    public void add(LogHandler h) {
        this.list.addFirst(h);
    }

    @Nullable
    public LogHandler remove() {
        return this.list.pop();
    }

    @Override
    public Iterator<LogHandler> iterator() {
        return this.list.iterator();
    }

    public boolean contains(LogHandler h) {
        return this.list.contains(h);
    }
}

