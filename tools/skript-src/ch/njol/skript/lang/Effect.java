/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.EffectSectionEffect;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Statement;
import ch.njol.skript.lang.function.EffFunctionCall;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import java.util.Iterator;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public abstract class Effect
extends Statement
implements SyntaxRuntimeErrorProducer {
    private Node node;

    protected Effect() {
    }

    @Override
    public boolean preInit() {
        this.node = this.getParser().getNode();
        return super.preInit();
    }

    protected abstract void execute(Event var1);

    @Override
    public final boolean run(Event event) {
        this.execute(event);
        return true;
    }

    @Nullable
    public static Effect parse(String input, @Nullable String defaultError) {
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            EffFunctionCall functionCall = EffFunctionCall.parse(input);
            if (functionCall != null) {
                log.printLog();
                EffFunctionCall effFunctionCall = functionCall;
                return effFunctionCall;
            }
            if (log.hasError()) {
                log.printError();
                Effect effect = null;
                return effect;
            }
            log.clear();
            EffectSection section = EffectSection.parse(input, null, null, null);
            if (section != null) {
                log.printLog();
                EffectSectionEffect effectSectionEffect = new EffectSectionEffect(section);
                return effectSectionEffect;
            }
            log.clear();
            Iterator<SyntaxInfo<? extends Effect>> iterator = Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.EFFECT).iterator();
            Effect effect = SkriptParser.parse(input, iterator, defaultError);
            if (effect != null) {
                log.printLog();
                Effect effect2 = effect;
                return effect2;
            }
            log.printError();
            Effect effect3 = null;
            return effect3;
        }
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    @NotNull
    public String getSyntaxTypeName() {
        return "effect";
    }
}

