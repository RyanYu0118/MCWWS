/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.registration;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegister;
import org.skriptlang.skript.registration.SyntaxRegistry;

final class SyntaxRegistryImpl
implements SyntaxRegistry {
    private final Map<SyntaxRegistry.Key<?>, SyntaxRegister<?>> registers = new ConcurrentHashMap();

    SyntaxRegistryImpl() {
    }

    @Override
    public <I extends SyntaxInfo<?>> @Unmodifiable Collection<I> syntaxes(SyntaxRegistry.Key<I> key) {
        return this.register(key).syntaxes();
    }

    @Override
    public <I extends SyntaxInfo<?>> void register(SyntaxRegistry.Key<I> key, I info) {
        this.register(key).add(info);
        if (key instanceof SyntaxRegistry.ChildKey) {
            this.register(((SyntaxRegistry.ChildKey)key).parent(), info);
        }
    }

    public void unregister(SyntaxInfo info) {
        for (SyntaxRegistry.Key<?> key : this.registers.keySet()) {
            this.unregister(key, info);
        }
    }

    @Override
    public <I extends SyntaxInfo<?>> void unregister(SyntaxRegistry.Key<I> key, I info) {
        this.register(key).remove(info);
        if (key instanceof SyntaxRegistry.ChildKey) {
            this.unregister(((SyntaxRegistry.ChildKey)key).parent(), info);
        }
    }

    private <I extends SyntaxInfo<?>> SyntaxRegister<I> register(SyntaxRegistry.Key<I> key) {
        return this.registers.computeIfAbsent(key, k -> new SyntaxRegister());
    }

    @Override
    public Collection<SyntaxInfo<?>> elements() {
        ImmutableSet.Builder builder = ImmutableSet.builder();
        this.registers.values().forEach(register -> {
            Set set = register.syntaxes;
            synchronized (set) {
                builder.addAll(register.syntaxes);
            }
        });
        return builder.build();
    }

    static final class ChildKeyImpl<T extends P, P extends SyntaxInfo<?>>
    extends KeyImpl<T>
    implements SyntaxRegistry.ChildKey<T, P> {
        private final SyntaxRegistry.Key<P> parent;

        ChildKeyImpl(SyntaxRegistry.Key<P> parent, String name) {
            super(name);
            this.parent = parent;
        }

        @Override
        public SyntaxRegistry.Key<P> parent() {
            return this.parent;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SyntaxRegistry.ChildKey)) return false;
            SyntaxRegistry.ChildKey key = (SyntaxRegistry.ChildKey)other;
            if (!super.equals(other)) return false;
            if (!this.parent().equals(key.parent())) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.parent());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("name", (Object)this.name()).add("parent", this.parent()).toString();
        }
    }

    static class KeyImpl<T extends SyntaxInfo<?>>
    implements SyntaxRegistry.Key<T> {
        protected final String name;

        KeyImpl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return this.name;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SyntaxRegistry.Key)) return false;
            SyntaxRegistry.Key key = (SyntaxRegistry.Key)other;
            if (!this.name().equals(key.name())) return false;
            return true;
        }

        public int hashCode() {
            return this.name().hashCode();
        }

        public String toString() {
            return MoreObjects.toStringHelper((Object)this).add("name", (Object)this.name()).toString();
        }
    }

    static final class UnmodifiableRegistry
    implements SyntaxRegistry {
        private final SyntaxRegistry registry;

        UnmodifiableRegistry(SyntaxRegistry registry) {
            this.registry = registry;
        }

        @Override
        public @Unmodifiable Collection<SyntaxInfo<?>> elements() {
            return this.registry.elements();
        }

        @Override
        public <I extends SyntaxInfo<?>> @Unmodifiable Collection<I> syntaxes(SyntaxRegistry.Key<I> key) {
            return this.registry.syntaxes(key);
        }

        @Override
        public <I extends SyntaxInfo<?>> void register(SyntaxRegistry.Key<I> key, I info) {
            throw new UnsupportedOperationException("Cannot register syntax infos with an unmodifiable syntax registry.");
        }

        @Override
        public void unregister(SyntaxInfo<?> info) {
            throw new UnsupportedOperationException("Cannot unregister syntax infos from an unmodifiable syntax registry.");
        }

        @Override
        public <I extends SyntaxInfo<?>> void unregister(SyntaxRegistry.Key<I> key, I info) {
            throw new UnsupportedOperationException("Cannot unregister syntax infos from an unmodifiable syntax registry.");
        }

        @Override
        public SyntaxRegistry unmodifiableView() {
            return this;
        }
    }

    static final class OriginApplyingRegistry
    implements SyntaxRegistry {
        private final SyntaxRegistry syntaxRegistry;
        private final Origin origin;

        OriginApplyingRegistry(SyntaxRegistry syntaxRegistry, Origin origin) {
            this.syntaxRegistry = syntaxRegistry;
            this.origin = origin;
        }

        @Override
        public <I extends SyntaxInfo<?>> @Unmodifiable Collection<I> syntaxes(SyntaxRegistry.Key<I> key) {
            return this.syntaxRegistry.syntaxes(key);
        }

        @Override
        public <I extends SyntaxInfo<?>> void register(SyntaxRegistry.Key<I> key, I info) {
            if (info.origin() == Origin.UNKNOWN) {
                info = info.toBuilder().origin(this.origin).build();
            }
            this.syntaxRegistry.register(key, info);
        }

        @Override
        public void unregister(SyntaxInfo<?> info) {
            this.syntaxRegistry.unregister(info);
        }

        @Override
        public <I extends SyntaxInfo<?>> void unregister(SyntaxRegistry.Key<I> key, I info) {
            this.syntaxRegistry.unregister(key, info);
        }

        @Override
        public @Unmodifiable Collection<SyntaxInfo<?>> elements() {
            return this.syntaxRegistry.elements();
        }
    }
}

