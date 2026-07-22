/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry.util;

import ch.njol.skript.lang.VariableString;
import ch.njol.skript.util.StringMode;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;

public class VariableStringEntryData
extends KeyValueEntryData<VariableString> {
    private final StringMode stringMode;

    public VariableStringEntryData(String key, @Nullable VariableString defaultValue, boolean optional) {
        this(key, defaultValue, optional, StringMode.MESSAGE);
    }

    public VariableStringEntryData(String key, @Nullable VariableString defaultValue, boolean optional, StringMode stringMode) {
        super(key, defaultValue, optional);
        this.stringMode = stringMode;
    }

    @Override
    @Nullable
    protected VariableString getValue(String value) {
        if (this.stringMode != StringMode.VARIABLE_NAME) {
            value = VariableString.quote(value);
        }
        return VariableString.newInstance(value, this.stringMode);
    }
}

