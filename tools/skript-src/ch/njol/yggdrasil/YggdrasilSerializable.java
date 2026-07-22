/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.Fields;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;

public interface YggdrasilSerializable {

    public static interface YggdrasilExtendedSerializable
    extends YggdrasilSerializable {
        public Fields serialize() throws NotSerializableException;

        public void deserialize(@NotNull Fields var1) throws StreamCorruptedException, NotSerializableException;
    }

    public static interface YggdrasilRobustEnum {
        public Enum<?> excessiveConstant(String var1);
    }

    public static interface YggdrasilRobustSerializable
    extends YggdrasilSerializable {
        public boolean incompatibleField(@NotNull Field var1, @NotNull Fields.FieldContext var2) throws StreamCorruptedException;

        public boolean excessiveField(@NotNull Fields.FieldContext var1) throws StreamCorruptedException;

        public boolean missingField(@NotNull Field var1) throws StreamCorruptedException;
    }
}

