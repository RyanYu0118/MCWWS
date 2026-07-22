/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.bukkit.particles.registration;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ToString {
    public SyntaxStringBuilder toString(Expression<?> @NotNull [] var1, SkriptParser.ParseResult var2, SyntaxStringBuilder var3);
}

