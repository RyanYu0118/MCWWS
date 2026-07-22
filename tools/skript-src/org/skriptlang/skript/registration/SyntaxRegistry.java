/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Unmodifiable
 */
package org.skriptlang.skript.registration;

import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.Statement;
import java.util.Collection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistryImpl;
import org.skriptlang.skript.util.Registry;
import org.skriptlang.skript.util.ViewProvider;

public interface SyntaxRegistry
extends ViewProvider<SyntaxRegistry>,
Registry<SyntaxInfo<?>> {
    public static final Key<DefaultSyntaxInfos.Structure<?>> STRUCTURE = Key.of("structure");
    public static final Key<SyntaxInfo<? extends Section>> SECTION = Key.of("section");
    public static final Key<SyntaxInfo<? extends Statement>> STATEMENT = Key.of("statement");
    public static final Key<SyntaxInfo<? extends Effect>> EFFECT = ChildKey.of(STATEMENT, "effect");
    public static final Key<SyntaxInfo<? extends Condition>> CONDITION = ChildKey.of(STATEMENT, "condition");
    public static final Key<DefaultSyntaxInfos.Expression<?, ?>> EXPRESSION = Key.of("expression");

    @Contract(value="-> new")
    public static SyntaxRegistry empty() {
        return new SyntaxRegistryImpl();
    }

    public static SyntaxRegistry withOrigin(SyntaxRegistry syntaxRegistry, Origin origin) {
        return new SyntaxRegistryImpl.OriginApplyingRegistry(syntaxRegistry, origin);
    }

    public <I extends SyntaxInfo<?>> @Unmodifiable Collection<I> syntaxes(Key<I> var1);

    public <I extends SyntaxInfo<?>> void register(Key<I> var1, I var2);

    public void unregister(SyntaxInfo<?> var1);

    public <I extends SyntaxInfo<?>> void unregister(Key<I> var1, I var2);

    @Override
    @Contract(value="-> new")
    default public SyntaxRegistry unmodifiableView() {
        return new SyntaxRegistryImpl.UnmodifiableRegistry(this);
    }

    @Override
    public @Unmodifiable Collection<SyntaxInfo<?>> elements();

    public static interface Key<I extends SyntaxInfo<?>> {
        @Contract(value="_ -> new")
        public static <I extends SyntaxInfo<?>> Key<I> of(String name) {
            return new SyntaxRegistryImpl.KeyImpl(name);
        }

        public String name();
    }

    public static interface ChildKey<I extends P, P extends SyntaxInfo<?>>
    extends Key<I> {
        @Contract(value="_, _ -> new")
        public static <I extends P, P extends SyntaxInfo<?>> ChildKey<I, P> of(Key<P> parent, String name) {
            return new SyntaxRegistryImpl.ChildKeyImpl(parent, name);
        }

        public Key<P> parent();
    }
}

