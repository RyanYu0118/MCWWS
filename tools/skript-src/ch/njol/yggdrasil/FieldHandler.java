/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.yggdrasil;

import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;

public interface FieldHandler {
    public boolean excessiveField(Object var1, Fields.FieldContext var2) throws StreamCorruptedException;

    public boolean missingField(Object var1, Field var2) throws StreamCorruptedException;

    public boolean incompatibleField(Object var1, Field var2, Fields.FieldContext var3) throws StreamCorruptedException;
}

