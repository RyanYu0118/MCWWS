/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package ch.njol.skript.lang;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface SyntaxElement {
    default public boolean preInit() {
        return true;
    }

    public boolean init(Expression<?>[] var1, int var2, Kleenean var3, SkriptParser.ParseResult var4);

    default public ParserInstance getParser() {
        return ParserInstance.get();
    }

    @Contract(pure=true)
    @NotNull
    public String getSyntaxTypeName();
}

