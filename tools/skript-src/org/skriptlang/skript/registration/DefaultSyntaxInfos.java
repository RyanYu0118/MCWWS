/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.registration;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.registration.DefaultSyntaxInfosImpl;
import org.skriptlang.skript.registration.SyntaxInfo;

public sealed interface DefaultSyntaxInfos
permits SyntaxInfo {

    public static interface Structure<E extends org.skriptlang.skript.lang.structure.Structure>
    extends SyntaxInfo<E> {
        @Contract(value="_ -> new")
        public static <E extends org.skriptlang.skript.lang.structure.Structure> Builder<? extends Builder<?, E>, E> builder(Class<E> structureClass) {
            return new DefaultSyntaxInfosImpl.StructureImpl.BuilderImpl(structureClass);
        }

        @Override
        @Contract(value="-> new")
        public Builder<? extends Builder<?, E>, E> toBuilder();

        @Nullable
        public EntryValidator entryValidator();

        public NodeType nodeType();

        public static interface Builder<B extends Builder<B, E>, E extends org.skriptlang.skript.lang.structure.Structure>
        extends SyntaxInfo.Builder<B, E> {
            @Contract(value="_ -> this")
            public B entryValidator(EntryValidator var1);

            public B nodeType(NodeType var1);

            @Override
            @Contract(value="-> new")
            public Structure<E> build();
        }

        public static enum NodeType {
            SIMPLE,
            SECTION,
            BOTH;


            public boolean canBeSimple() {
                return this != SECTION;
            }

            public boolean canBeSection() {
                return this != SIMPLE;
            }
        }
    }

    public static interface Expression<E extends ch.njol.skript.lang.Expression<R>, R>
    extends SyntaxInfo<E> {
        @Contract(value="_, _ -> new")
        public static <E extends ch.njol.skript.lang.Expression<R>, R> Builder<? extends Builder<?, E, R>, E, R> builder(Class<E> expressionClass, Class<R> returnType) {
            return new DefaultSyntaxInfosImpl.ExpressionImpl.BuilderImpl(expressionClass, returnType);
        }

        @Contract(value="-> new")
        public Builder<? extends Builder<?, E, R>, E, R> toBuilder();

        public Class<R> returnType();

        public static interface Builder<B extends Builder<B, E, R>, E extends ch.njol.skript.lang.Expression<R>, R>
        extends SyntaxInfo.Builder<B, E> {
            @Contract(value="-> new")
            public Expression<E, R> build();
        }
    }
}

