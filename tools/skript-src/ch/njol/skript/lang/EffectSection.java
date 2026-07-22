/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.SyntaxRegistry;

public abstract class EffectSection
extends Section {
    private boolean hasSection;

    public boolean hasSection() {
        return this.hasSection;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ParserInstance parser = this.getParser();
        Section.SectionContext sectionContext = parser.getData(Section.SectionContext.class);
        EffectSectionContext effectSectionContext = parser.getData(EffectSectionContext.class);
        SectionNode sectionNode = sectionContext.sectionNode;
        if (!effectSectionContext.isNodeForEffectSection) {
            sectionContext.sectionNode = null;
        }
        this.hasSection = sectionContext.sectionNode != null;
        boolean result = super.init(expressions, matchedPattern, isDelayed, parseResult);
        if (!effectSectionContext.isNodeForEffectSection) {
            sectionContext.sectionNode = sectionNode;
        }
        return result;
    }

    @Override
    public abstract boolean init(Expression<?>[] var1, int var2, Kleenean var3, SkriptParser.ParseResult var4, @Nullable SectionNode var5, @Nullable List<TriggerItem> var6);

    @Nullable
    public static EffectSection parse(String input, @Nullable String defaultError, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        return EffectSection.parse(input, defaultError, sectionNode, true, triggerItems);
    }

    @Nullable
    public static EffectSection parse(String input, @Nullable String defaultError, SectionNode sectionNode, boolean isNodeForEffectSection, List<TriggerItem> triggerItems) {
        ParserInstance parser = ParserInstance.get();
        Section.SectionContext sectionContext = parser.getData(Section.SectionContext.class);
        EffectSectionContext effectSectionContext = parser.getData(EffectSectionContext.class);
        boolean wasNodeForEffectSection = effectSectionContext.isNodeForEffectSection;
        effectSectionContext.isNodeForEffectSection = isNodeForEffectSection;
        EffectSection effectSection = sectionContext.modify(sectionNode, triggerItems, () -> {
            Iterator iterator = Skript.instance().syntaxRegistry().syntaxes(SyntaxRegistry.SECTION).stream().filter(info -> EffectSection.class.isAssignableFrom(info.type())).iterator();
            EffectSection parsed = (EffectSection)SkriptParser.parse(input, iterator, defaultError);
            if (parsed != null && sectionNode != null && !sectionContext.claimed()) {
                Skript.error("The line '" + input + "' is a valid statement but cannot function as a section (:) because there is no syntax in the line to manage it.");
                return null;
            }
            return parsed;
        });
        effectSectionContext.isNodeForEffectSection = wasNodeForEffectSection;
        return effectSection;
    }

    static {
        ParserInstance.registerData(EffectSectionContext.class, EffectSectionContext::new);
    }

    private static class EffectSectionContext
    extends ParserInstance.Data {
        public boolean isNodeForEffectSection = true;

        public EffectSectionContext(ParserInstance parserInstance) {
            super(parserInstance);
        }
    }
}

