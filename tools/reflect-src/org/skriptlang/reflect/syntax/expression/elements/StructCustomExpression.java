/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.classes.Changer$ChangeMode
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.ExpressionType
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptEvent
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.skript.lang.Trigger
 *  ch.njol.skript.lang.TriggerItem
 *  ch.njol.skript.lang.util.SimpleEvent
 *  ch.njol.skript.log.SkriptLogger
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.skript.util.Utils
 *  ch.njol.util.NonNullPair
 *  ch.njol.util.StringUtils
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.lang.entry.EntryContainer
 *  org.skriptlang.skript.lang.entry.EntryData
 *  org.skriptlang.skript.lang.entry.EntryValidator
 *  org.skriptlang.skript.lang.entry.EntryValidator$EntryValidatorBuilder
 *  org.skriptlang.skript.lang.entry.KeyValueEntryData
 *  org.skriptlang.skript.lang.script.Script
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 */
package org.skriptlang.reflect.syntax.expression.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.expression.ChangerEntryData;
import org.skriptlang.reflect.syntax.expression.ExpressionChangeEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionGetEvent;
import org.skriptlang.reflect.syntax.expression.ExpressionSyntaxInfo;
import org.skriptlang.reflect.syntax.expression.elements.CustomExpression;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.KeyValueEntryData;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class StructCustomExpression
extends CustomSyntaxStructure<ExpressionSyntaxInfo> {
    public static boolean customExpressionsUsed = false;
    public static final CustomSyntaxStructure.DataTracker<ExpressionSyntaxInfo> dataTracker;
    static final Map<ExpressionSyntaxInfo, Class<?>> returnTypes;
    static final Map<ExpressionSyntaxInfo, Trigger> expressionHandlers;
    static final Map<ExpressionSyntaxInfo, Trigger> parserHandlers;
    static final Map<ExpressionSyntaxInfo, List<Changer.ChangeMode>> hasChanger;
    static final Map<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Trigger>> changerHandlers;
    static final Map<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Class<?>[]>> changerTypes;
    static final Map<ExpressionSyntaxInfo, String> loopOfs;
    static final Map<ExpressionSyntaxInfo, List<Supplier<Boolean>>> usableSuppliers;
    static final Map<ExpressionSyntaxInfo, Boolean> parseSectionLoaded;
    private final Map<Changer.ChangeMode, SectionNode> changerNodes = new HashMap<Changer.ChangeMode, SectionNode>();
    private SectionNode parseNode;

    @Override
    protected CustomSyntaxStructure.DataTracker<ExpressionSyntaxInfo> getDataTracker() {
        return dataTracker;
    }

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        customExpressionsUsed = true;
        List patterns = (List)entryContainer.getOptional("patterns", List.class, false);
        Script script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;
        boolean alwaysPlural = parseResult.hasTag("plural");
        if (matchedPattern != 1 && patterns != null) {
            Skript.error((String)"A custom expression with an inline pattern cannot have a 'patterns' entry too");
            return false;
        }
        switch (matchedPattern) {
            case 0: {
                String pattern = ((MatchResult)parseResult.regexes.get(0)).group();
                this.register(ExpressionSyntaxInfo.create(script, pattern, 1, alwaysPlural, false, false));
                break;
            }
            case 1: {
                if (patterns == null) {
                    Skript.error((String)"A custom expression without an inline pattern must have a 'patterns' entry");
                    return false;
                }
                int i = 1;
                for (String p : patterns) {
                    this.register(ExpressionSyntaxInfo.create(script, p, i++, alwaysPlural, false, false));
                }
                break;
            }
            case 2: {
                String property = ((MatchResult)parseResult.regexes.get(0)).group();
                Object type = Arrays.stream((ClassInfo[])args[0].getArray()).map(ClassInfo::getCodeName).map(codeName -> {
                    boolean isPlural = (Boolean)Utils.getEnglishPlural((String)codeName).getSecond();
                    if (!isPlural) {
                        return Utils.toEnglishPlural((String)codeName);
                    }
                    return codeName;
                }).collect(Collectors.joining("/"));
                if (!alwaysPlural) {
                    type = "$" + (String)type;
                }
                this.register(ExpressionSyntaxInfo.create(script, "[the] " + property + " of %" + (String)type + "%", 1, alwaysPlural, true, true));
                this.register(ExpressionSyntaxInfo.create(script, "%" + (String)type + "%'[s] " + property, 1, alwaysPlural, false, true));
            }
        }
        return true;
    }

    @Override
    public boolean preLoad() {
        SectionNode usableInNode;
        String loopOf;
        super.preLoad();
        EntryContainer entryContainer = this.getEntryContainer();
        SectionNode[] parseNode = this.getParseNode();
        if (parseNode == null) {
            return false;
        }
        this.parseNode = parseNode[0];
        this.whichInfo.forEach(which -> parseSectionLoaded.put((ExpressionSyntaxInfo)which, this.parseNode == null));
        Class returnType = (Class)entryContainer.getOptional("return type", Class.class, false);
        if (returnType != null) {
            this.whichInfo.forEach(which -> returnTypes.put((ExpressionSyntaxInfo)which, returnType));
        }
        if ((loopOf = (String)entryContainer.getOptional("loop of", String.class, false)) != null) {
            this.whichInfo.forEach(which -> loopOfs.put((ExpressionSyntaxInfo)which, loopOf));
        }
        if ((usableInNode = (SectionNode)entryContainer.getOptional("usable in", SectionNode.class, false)) != null && !this.handleUsableEntry(usableInNode, usableSuppliers)) {
            return false;
        }
        for (Changer.ChangeMode mode : Changer.ChangeMode.values()) {
            String name = mode.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
            NonNullPair pair = (NonNullPair)entryContainer.getOptional(name, NonNullPair.class, false);
            if (pair == null) continue;
            this.changerNodes.put(mode, (SectionNode)pair.getFirst());
            this.whichInfo.forEach(which -> {
                List hasChangerList = hasChanger.computeIfAbsent((ExpressionSyntaxInfo)which, k -> new ArrayList());
                hasChangerList.add(mode);
            });
            if (((Class[])pair.getSecond()).length == 0) continue;
            this.whichInfo.forEach(which -> changerTypes.computeIfAbsent((ExpressionSyntaxInfo)which, k -> new HashMap()).put(mode, (Class[])pair.getSecond()));
        }
        return true;
    }

    @Override
    public boolean load() {
        if (this.parseNode != null) {
            SkriptLogger.setNode((Node)this.parseNode);
            SyntaxParseEvent.register(this.parseNode, this.whichInfo, parserHandlers);
            this.whichInfo.forEach(which -> parseSectionLoaded.put((ExpressionSyntaxInfo)which, true));
        }
        SectionNode getNode = (SectionNode)this.getEntryContainer().get("get", SectionNode.class, false);
        SkriptLogger.setNode((Node)getNode);
        this.getParser().setCurrentEvent("custom expression getter", new Class[]{ExpressionGetEvent.class});
        List<TriggerItem> items = SkriptUtil.getItemsFromNode(getNode);
        this.whichInfo.forEach(which -> expressionHandlers.put((ExpressionSyntaxInfo)which, new Trigger(this.getParser().getCurrentScript(), "get " + which.getPattern(), (SkriptEvent)new SimpleEvent(), items)));
        this.changerNodes.forEach((changeMode, node) -> {
            SkriptLogger.setNode((Node)node);
            this.getParser().setCurrentEvent("custom expression changer", new Class[]{ExpressionChangeEvent.class});
            List<TriggerItem> items = SkriptUtil.getItemsFromNode(node);
            this.whichInfo.forEach(which -> {
                Map changerMap = changerHandlers.computeIfAbsent((ExpressionSyntaxInfo)which, k -> new HashMap());
                String name = changeMode.toString().toLowerCase(Locale.ENGLISH).replace('_', ' ');
                changerMap.put(changeMode, new Trigger(this.getParser().getCurrentScript(), String.format("%s %s", name, which.getPattern()), (SkriptEvent)new SimpleEvent(), items));
            });
        });
        SkriptLogger.setNode(null);
        return true;
    }

    public static ExpressionSyntaxInfo lookup(Script script, int matchedPattern) {
        return dataTracker.lookup(script, matchedPattern);
    }

    static {
        String[] syntax = new String[]{"[:local] [plural:(plural|non[-| ]single)] expression <.+>", "[:local] [plural:(plural|non[-| ]single)] expression", "[:local] [plural:(plural|non[-| ]single)] %*classinfos% property <.+>"};
        EntryValidator.EntryValidatorBuilder builder = StructCustomExpression.customSyntaxValidator().addEntryData((EntryData)new KeyValueEntryData<Class<?>>("return type", null, true){

            @Nullable
            protected Class<?> getValue(String value) {
                Class returnType = Classes.getClassFromUserInput((String)value);
                if (returnType == null) {
                    Skript.error((String)"The given return type doesn't exist");
                }
                return returnType;
            }
        }).addEntry("loop of", null, true).addSection("get", false);
        Arrays.stream(Changer.ChangeMode.values()).sorted((mode1, mode2) -> {
            long words1 = StringUtils.count((String)mode1.toString(), (char)'_');
            long words2 = StringUtils.count((String)mode2.toString(), (char)'_');
            return Long.compare(words2, words1);
        }).map(mode -> mode.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH)).forEach(name -> builder.addEntryData((EntryData)new ChangerEntryData((String)name, true)));
        Skript.registerStructure(StructCustomExpression.class, (EntryValidator)builder.build(), (String[])syntax);
        dataTracker = new CustomSyntaxStructure.DataTracker();
        returnTypes = new HashMap();
        expressionHandlers = new HashMap<ExpressionSyntaxInfo, Trigger>();
        parserHandlers = new HashMap<ExpressionSyntaxInfo, Trigger>();
        hasChanger = new HashMap<ExpressionSyntaxInfo, List<Changer.ChangeMode>>();
        changerHandlers = new HashMap<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Trigger>>();
        changerTypes = new HashMap<ExpressionSyntaxInfo, Map<Changer.ChangeMode, Class<?>[]>>();
        loopOfs = new HashMap<ExpressionSyntaxInfo, String>();
        usableSuppliers = new HashMap<ExpressionSyntaxInfo, List<Supplier<Boolean>>>();
        parseSectionLoaded = new HashMap<ExpressionSyntaxInfo, Boolean>();
        Skript.registerExpression(CustomExpression.class, Object.class, (ExpressionType)ExpressionType.PATTERN_MATCHES_EVERYTHING, (String[])new String[]{"this is here because at least one pattern is required"});
        Optional<SyntaxInfo> info = SkriptMirror.getAddonInstance().syntaxRegistry().elements().stream().filter(i -> Expression.class.isAssignableFrom(i.type())).filter(i -> i.type() == CustomExpression.class).findFirst();
        info.ifPresent(dataTracker::setInfo);
        dataTracker.setSyntaxKey(SyntaxRegistry.EXPRESSION);
        dataTracker.addManaged(returnTypes);
        dataTracker.addManaged(expressionHandlers);
        dataTracker.addManaged(hasChanger);
        dataTracker.addManaged(changerHandlers);
        dataTracker.addManaged(changerTypes);
        dataTracker.addManaged(parserHandlers);
        dataTracker.addManaged(loopOfs);
        dataTracker.addManaged(usableSuppliers);
        dataTracker.addManaged(parseSectionLoaded);
    }
}

