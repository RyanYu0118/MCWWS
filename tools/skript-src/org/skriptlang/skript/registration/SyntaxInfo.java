/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.registration;

import ch.njol.skript.lang.SyntaxElement;
import java.util.Collection;
import java.util.SequencedCollection;
import java.util.function.Supplier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfoImpl;
import org.skriptlang.skript.util.Priority;

public non-sealed interface SyntaxInfo<E extends SyntaxElement>
extends DefaultSyntaxInfos {
    public static final Priority SIMPLE = Priority.base();
    public static final Priority COMBINED = Priority.after(SIMPLE);
    public static final Priority PATTERN_MATCHES_EVERYTHING = Priority.after(COMBINED);

    @Contract(value="_ -> new")
    public static <E extends SyntaxElement> Builder<? extends Builder<?, E>, E> builder(Class<E> type) {
        return new SyntaxInfoImpl.BuilderImpl(type);
    }

    @Contract(value="-> new")
    public Builder<? extends Builder<?, E>, E> toBuilder();

    public Origin origin();

    public Class<E> type();

    @Contract(value="-> new")
    public E instance();

    public @Unmodifiable SequencedCollection<String> patterns();

    public Priority priority();

    public static interface Builder<B extends Builder<B, E>, E extends SyntaxElement> {
        @Contract(value="_ -> this")
        public B origin(Origin var1);

        @Contract(value="_ -> this")
        public B supplier(Supplier<E> var1);

        @Contract(value="_ -> this")
        public B addPattern(String var1);

        @Contract(value="_ -> this")
        public B addPatterns(String ... var1);

        @Contract(value="_ -> this")
        public B addPatterns(Collection<String> var1);

        @Contract(value="-> this")
        public B clearPatterns();

        @Contract(value="_ -> this")
        public B priority(Priority var1);

        @Contract(value="-> new")
        public SyntaxInfo<E> build();

        public void applyTo(Builder<?, ?> var1);
    }
}

