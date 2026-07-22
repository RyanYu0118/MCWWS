/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.util.SimpleEvent
 *  ch.njol.skript.log.SkriptLogger
 *  org.skriptlang.skript.lang.entry.EntryContainer
 *  org.skriptlang.skript.lang.entry.EntryValidator
 *  org.skriptlang.skript.lang.script.Script
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package org.skriptlang.reflect.syntax.effect.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.effect.EffectSyntaxInfo;
import org.skriptlang.reflect.syntax.effect.EffectTriggerEvent;
import org.skriptlang.reflect.syntax.effect.elements.CustomEffect;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class StructCustomEffect
extends CustomSyntaxStructure<EffectSyntaxInfo> {
    public static boolean customEffectsUsed = false;
    public static final CustomSyntaxStructure.DataTracker<EffectSyntaxInfo> dataTracker;
    static final Map<EffectSyntaxInfo, Trigger> effectHandlers;
    static final Map<EffectSyntaxInfo, Trigger> parserHandlers;
    static final Map<EffectSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers;
    static final Map<EffectSyntaxInfo, Boolean> parseSectionLoaded;
    private SectionNode parseNode;

    @Override
    public CustomSyntaxStructure.DataTracker<EffectSyntaxInfo> getDataTracker() {
        return dataTracker;
    }

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        Script script;
        customEffectsUsed = true;
        List patterns = (List)entryContainer.getOptional("patterns", List.class, false);
        Object object = script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;
        if (matchedPattern != 1 && patterns != null) {
            Skript.error((String)"A custom effect with an inline pattern cannot have a 'patterns' entry too");
            return false;
        }
        switch (matchedPattern) {
            case 0: {
                this.register(EffectSyntaxInfo.create(script, ((MatchResult)parseResult.regexes.get(0)).group(), 1));
                break;
            }
            case 1: {
                if (patterns == null) {
                    Skript.error((String)"A custom effect without an inline pattern must have a 'patterns' entry");
                    return false;
                }
                int i = 1;
                for (String pattern : patterns) {
                    this.register(EffectSyntaxInfo.create(script, pattern, i++));
                }
                break;
            }
        }
        return this.checkHasPatterns();
    }

    @Override
    public boolean preLoad() {
        super.preLoad();
        EntryContainer entryContainer = this.getEntryContainer();
        SectionNode[] parseNode = this.getParseNode();
        if (parseNode == null) {
            return false;
        }
        this.parseNode = parseNode[0];
        this.whichInfo.forEach(which -> parseSectionLoaded.put((EffectSyntaxInfo)which, this.parseNode == null));
        SectionNode usableInNode = (SectionNode)entryContainer.getOptional("usable in", SectionNode.class, false);
        return usableInNode == null || this.handleUsableEntry(usableInNode, usableSuppliers);
    }

    @Override
    public boolean load() {
        if (this.parseNode != null) {
            SkriptLogger.setNode((Node)this.parseNode);
            SyntaxParseEvent.register(this.parseNode, this.whichInfo, parserHandlers);
            this.whichInfo.forEach(which -> parseSectionLoaded.put((EffectSyntaxInfo)which, true));
        }
        SectionNode triggerNode = (SectionNode)this.getEntryContainer().get("trigger", SectionNode.class, false);
        SkriptLogger.setNode((Node)triggerNode);
        this.getParser().setCurrentEvent("custom effect trigger", new Class[]{EffectTriggerEvent.class});
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(triggerNode);
        this.whichInfo.forEach(which -> effectHandlers.put((EffectSyntaxInfo)which, new Trigger(this.getParser().getCurrentScript(), "effect " + String.valueOf(which), (SkriptEvent)new SimpleEvent(), items)));
        SkriptLogger.setNode(null);
        return true;
    }

    public static EffectSyntaxInfo lookup(Script script, int matchedPattern) {
        return dataTracker.lookup(script, matchedPattern);
    }

    static {
        String[] syntax = new String[]{"[:local] effect <.+>", "[:local] effect"};
        Skript.registerStructure(StructCustomEffect.class, (EntryValidator)StructCustomEffect.customSyntaxValidator().addSection("trigger", false).build(), (String[])syntax);
        dataTracker = new CustomSyntaxStructure.DataTracker();
        effectHandlers = new HashMap<EffectSyntaxInfo, Trigger>();
        parserHandlers = new HashMap<EffectSyntaxInfo, Trigger>();
        usableSuppliers = new HashMap<EffectSyntaxInfo, List<Supplier<Boolean>>>();
        parseSectionLoaded = new HashMap<EffectSyntaxInfo, Boolean>();
        Skript.registerEffect(CustomEffect.class, (String[])new String[]{"this is here because at least one pattern is required"});
        Optional<SyntaxInfo> info = SkriptMirror.getAddonInstance().syntaxRegistry().syntaxes(SyntaxRegistry.EFFECT).stream().filter(i -> i.type() == CustomEffect.class).findFirst();
        info.ifPresent(dataTracker::setInfo);
        dataTracker.setSyntaxKey(SyntaxRegistry.EFFECT);
        dataTracker.addManaged(effectHandlers);
        dataTracker.addManaged(parserHandlers);
        dataTracker.addManaged(usableSuppliers);
        dataTracker.addManaged(parseSectionLoaded);
    }
}

