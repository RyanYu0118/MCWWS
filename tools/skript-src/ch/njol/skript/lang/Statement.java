/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.EffectSectionEffect;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.function.EffFunctionCall;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public abstract class Statement
extends TriggerItem
implements SyntaxElement {
    @Nullable
    public static Statement parse(String input, String defaultError) {
        return Statement.parse(input, null, defaultError);
    }

    @Nullable
    public static Statement parse(String input, @Nullable List<TriggerItem> items, String defaultError) {
        return Statement.parse(input, defaultError, null, items);
    }

    @Nullable
    public static Statement parse(String input, @Nullable String defaultError, @Nullable SectionNode node, @Nullable List<TriggerItem> items) {
        try (ParseLogHandler log = SkriptLogger.startParseLogHandler();){
            Statement statement;
            Statement statement2;
            final Section.SectionContext sectionContext = ParserInstance.get().getData(Section.SectionContext.class);
            EffFunctionCall functionCall = node != null ? sectionContext.modify(node, items, () -> {
                EffFunctionCall parsed = EffFunctionCall.parse(input);
                if (parsed != null && !sectionContext.claimed()) {
                    Skript.error("The line '" + input + "' is a valid function call but cannot function as a section (:) because there is no parameter to manage it.");
                    return null;
                }
                return parsed;
            }) : EffFunctionCall.parse(input);
            if (functionCall != null) {
                log.printLog();
                EffFunctionCall effFunctionCall = functionCall;
                return effFunctionCall;
            }
            if (log.hasError()) {
                log.printError();
                Statement statement3 = null;
                return statement3;
            }
            log.clear();
            EffectSection section = EffectSection.parse(input, null, node, false, items);
            if (section != null) {
                log.printLog();
                EffectSectionEffect effectSectionEffect = new EffectSectionEffect(section);
                return effectSectionEffect;
            }
            log.clear();
            final Iterator<SyntaxInfo<? extends Statement>> iterator = Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.STATEMENT).iterator();
            if (node != null) {
                Iterator<Object> wrappedIterator = new Iterator<Object>(){

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public SyntaxInfo<? extends Statement> next() {
                        sectionContext.owner = null;
                        return (SyntaxInfo)iterator.next();
                    }
                };
                statement2 = sectionContext.modify(node, items, () -> Statement.lambda$parse$1(input, wrappedIterator, defaultError, sectionContext));
            } else {
                statement2 = sectionContext.modify(null, null, () -> (Statement)SkriptParser.parse(input, iterator, defaultError));
            }
            if (statement2 != null) {
                log.printLog();
                statement = statement2;
                return statement;
            }
            log.printError();
            statement = null;
            return statement;
        }
    }

    private static /* synthetic */ Statement lambda$parse$1(String input, 1 wrappedIterator, String defaultError, Section.SectionContext sectionContext) {
        Statement parsed = (Statement)SkriptParser.parse(input, wrappedIterator, defaultError);
        if (parsed != null && !sectionContext.claimed()) {
            Skript.error("The line '" + input + "' is a valid statement but cannot function as a section (:) because there is no syntax in the line to manage it.");
            return null;
        }
        return parsed;
    }
}

