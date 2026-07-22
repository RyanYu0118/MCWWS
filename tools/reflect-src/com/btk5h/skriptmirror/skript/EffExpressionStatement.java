/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.effects.Delay
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.util.Kleenean
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Event
 *  org.bukkit.plugin.Plugin
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.reflect.ExprJavaCall;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.MatchResult;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class EffExpressionStatement
extends Effect {
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Expression<?> arg;
    private boolean isAsynchronous;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EFFECT, SyntaxInfo.builder(EffExpressionStatement.class).addPattern("[1:await] <.+>").supplier(EffExpressionStatement::new).priority(SkriptMirror.SHADOW_REALM).build());
    }

    protected void execute(Event e) {
        this.arg.getAll(e);
    }

    protected TriggerItem walk(Event e) {
        if (this.isAsynchronous) {
            Delay.addDelayedEvent((Event)e);
            Object localVariables = SkriptReflection.getLocals(e);
            CompletableFuture.runAsync(() -> {
                SkriptReflection.putLocals(localVariables, e);
                this.execute(e);
            }, threadPool).thenAccept(res -> Bukkit.getScheduler().runTask((Plugin)SkriptMirror.getInstance(), () -> {
                if (this.getNext() != null) {
                    TriggerItem.walk((TriggerItem)this.getNext(), (Event)e);
                }
                SkriptReflection.removeLocals(e);
            }));
            return null;
        }
        return super.walk(e);
    }

    public String toString(Event e, boolean debug) {
        return this.arg.toString(e, debug);
    }

    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        String rawInput = ((MatchResult)parseResult.regexes.getFirst()).group();
        this.arg = ExprJavaCall.parse(rawInput);
        if (this.arg == null) {
            return false;
        }
        boolean bl = this.isAsynchronous = (parseResult.mark & 1) == 1;
        if (this.isAsynchronous) {
            this.getParser().setHasDelayBefore(Kleenean.TRUE);
        }
        return SkriptUtil.canInitSafely(this.arg);
    }
}

