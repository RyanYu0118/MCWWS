/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.util.SimpleEvent
 *  ch.njol.skript.log.SkriptLogger
 *  ch.njol.skript.util.Utils
 *  org.skriptlang.skript.lang.entry.EntryContainer
 *  org.skriptlang.skript.lang.entry.EntryValidator
 *  org.skriptlang.skript.lang.script.Script
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package org.skriptlang.reflect.syntax.condition.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.util.Utils;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.condition.ConditionCheckEvent;
import org.skriptlang.reflect.syntax.condition.ConditionSyntaxInfo;
import org.skriptlang.reflect.syntax.condition.elements.CustomCondition;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class StructCustomCondition
extends CustomSyntaxStructure<ConditionSyntaxInfo> {
    public static boolean customConditionsUsed = false;
    public static final CustomSyntaxStructure.DataTracker<ConditionSyntaxInfo> dataTracker;
    static final Map<ConditionSyntaxInfo, Trigger> conditionHandlers;
    static final Map<ConditionSyntaxInfo, Trigger> parserHandlers;
    static final Map<ConditionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers;
    static final Map<ConditionSyntaxInfo, Boolean> parseSectionLoaded;
    private SectionNode parseNode;

    @Override
    public CustomSyntaxStructure.DataTracker<ConditionSyntaxInfo> getDataTracker() {
        return dataTracker;
    }

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        Script script;
        customConditionsUsed = true;
        List patterns = (List)entryContainer.getOptional("patterns", List.class, false);
        Object object = script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;
        if (matchedPattern != 1 && patterns != null) {
            Skript.error((String)"A custom condition with an inline pattern cannot have a 'patterns' entry too");
            return false;
        }
        switch (matchedPattern) {
            case 0: {
                String pattern = ((MatchResult)parseResult.regexes.get(0)).group();
                this.register(ConditionSyntaxInfo.create(script, pattern, 1, false, false));
                break;
            }
            case 1: {
                if (patterns == null) {
                    Skript.error((String)"A custom condition without an inline pattern must have a 'patterns' entry");
                    return false;
                }
                int i = 1;
                for (String p : patterns) {
                    this.register(ConditionSyntaxInfo.create(script, p, i++, false, false));
                }
                break;
            }
            case 2: {
                String property = ((MatchResult)parseResult.regexes.get(0)).group();
                String type = Arrays.stream((ClassInfo[])args[0].getArray()).map(ClassInfo::getCodeName).map(codeName -> {
                    boolean isPlural = (Boolean)Utils.getEnglishPlural((String)codeName).getSecond();
                    if (!isPlural) {
                        return Utils.toEnglishPlural((String)codeName);
                    }
                    return codeName;
                }).collect(Collectors.joining("/"));
                this.register(ConditionSyntaxInfo.create(script, "%" + type + "% (is|are) " + property, 1, false, true));
                this.register(ConditionSyntaxInfo.create(script, "%" + type + "% (isn't|is not|aren't|are not) " + property, 1, true, true));
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
        this.whichInfo.forEach(which -> parseSectionLoaded.put((ConditionSyntaxInfo)which, this.parseNode == null));
        SectionNode usableInNode = (SectionNode)entryContainer.getOptional("usable in", SectionNode.class, false);
        return usableInNode == null || this.handleUsableEntry(usableInNode, usableSuppliers);
    }

    @Override
    public boolean load() {
        if (this.parseNode != null) {
            SkriptLogger.setNode((Node)this.parseNode);
            SyntaxParseEvent.register(this.parseNode, this.whichInfo, parserHandlers);
            this.whichInfo.forEach(which -> parseSectionLoaded.put((ConditionSyntaxInfo)which, true));
        }
        SectionNode checkNode = (SectionNode)this.getEntryContainer().get("check", SectionNode.class, false);
        SkriptLogger.setNode((Node)checkNode);
        this.getParser().setCurrentEvent("custom condition check", new Class[]{ConditionCheckEvent.class});
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(checkNode);
        this.whichInfo.forEach(which -> conditionHandlers.put((ConditionSyntaxInfo)which, new Trigger(this.getParser().getCurrentScript(), "condition " + String.valueOf(which), (SkriptEvent)new SimpleEvent(), items)));
        SkriptLogger.setNode(null);
        return true;
    }

    public static ConditionSyntaxInfo lookup(Script script, int matchedPattern) {
        return dataTracker.lookup(script, matchedPattern);
    }

    static {
        String[] syntax = new String[]{"[:local] condition <.+>", "[:local] condition", "[:local] %*classinfos% property condition <.+>"};
        Skript.registerStructure(StructCustomCondition.class, (EntryValidator)StructCustomCondition.customSyntaxValidator().addSection("check", false).build(), (String[])syntax);
        dataTracker = new CustomSyntaxStructure.DataTracker();
        conditionHandlers = new HashMap<ConditionSyntaxInfo, Trigger>();
        parserHandlers = new HashMap<ConditionSyntaxInfo, Trigger>();
        usableSuppliers = new HashMap<ConditionSyntaxInfo, List<Supplier<Boolean>>>();
        parseSectionLoaded = new HashMap<ConditionSyntaxInfo, Boolean>();
        Skript.registerCondition(CustomCondition.class, (String[])new String[]{"this is here because at least one pattern is required"});
        Optional<SyntaxInfo> info = SkriptMirror.getAddonInstance().syntaxRegistry().syntaxes(SyntaxRegistry.CONDITION).stream().filter(i -> i.type() == CustomCondition.class).findFirst();
        info.ifPresent(dataTracker::setInfo);
        dataTracker.setSyntaxKey(SyntaxRegistry.CONDITION);
        dataTracker.addManaged(conditionHandlers);
        dataTracker.addManaged(parserHandlers);
        dataTracker.addManaged(usableSuppliers);
        dataTracker.addManaged(parseSectionLoaded);
    }
}

