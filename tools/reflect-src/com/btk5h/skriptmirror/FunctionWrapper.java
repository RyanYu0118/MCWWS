/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.lang.function.Function
 *  ch.njol.skript.lang.function.Functions
 *  ch.njol.skript.lang.parser.ParserInstance
 *  org.jetbrains.annotations.Nullable
 */
package com.btk5h.skriptmirror;

import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.parser.ParserInstance;
import org.jetbrains.annotations.Nullable;

public class FunctionWrapper {
    private final String name;
    private final Object[] arguments;

    public FunctionWrapper(String name, Object[] arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return this.name;
    }

    public Object[] getArguments() {
        return this.arguments;
    }

    @Nullable
    public Function<?> getFunction() {
        String script = null;
        ParserInstance parserInstance = ParserInstance.get();
        if (parserInstance.isActive()) {
            script = parserInstance.getCurrentScript().getConfig().getFileName();
        }
        return Functions.getFunction((String)this.name, script);
    }
}

