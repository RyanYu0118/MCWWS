/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.util;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;

public interface OpenCloseable
extends AutoCloseable {
    public static final OpenCloseable EMPTY = new OpenCloseable(){

        @Override
        public void open() {
        }

        @Override
        public void close() {
        }
    };

    public static OpenCloseable combine(OpenCloseable ... openCloseableArray) {
        final ArrayDeque<OpenCloseable> openCloseables = new ArrayDeque<OpenCloseable>(Arrays.asList(openCloseableArray));
        return new OpenCloseable(){

            @Override
            public void open() {
                for (OpenCloseable openCloseable : openCloseables) {
                    openCloseable.open();
                }
            }

            @Override
            public void close() {
                Iterator openCloseableIterator = openCloseables.descendingIterator();
                while (openCloseableIterator.hasNext()) {
                    ((OpenCloseable)openCloseableIterator.next()).close();
                }
            }
        };
    }

    public void open();

    @Override
    public void close();
}

