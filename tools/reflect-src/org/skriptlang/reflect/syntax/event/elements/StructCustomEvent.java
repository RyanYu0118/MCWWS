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
 *  org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos$Event
 *  org.skriptlang.skript.lang.entry.EntryContainer
 *  org.skriptlang.skript.lang.entry.EntryData
 *  org.skriptlang.skript.lang.entry.EntryValidator
 *  org.skriptlang.skript.lang.script.Script
 */
package org.skriptlang.reflect.syntax.event.elements;

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
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.skript.custom.SyntaxParseEvent;
import com.btk5h.skriptmirror.util.SkriptReflection;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.skriptlang.reflect.syntax.CustomSyntaxStructure;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.EventTriggerEvent;
import org.skriptlang.reflect.syntax.event.EventValuesEntryData;
import org.skriptlang.reflect.syntax.event.elements.CustomEvent;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;

public class StructCustomEvent
extends CustomSyntaxStructure<EventSyntaxInfo> {
    public static boolean customEventsUsed = false;
    private static final CustomSyntaxStructure.DataTracker<EventSyntaxInfo> dataTracker;
    static final Map<EventSyntaxInfo, String> nameValues;
    static final Map<EventSyntaxInfo, List<ClassInfo<?>>> eventValueTypes;
    static final Map<EventSyntaxInfo, Trigger> parserHandlers;
    static final Map<EventSyntaxInfo, Trigger> eventHandlers;
    static final Map<EventSyntaxInfo, Boolean> parseSectionLoaded;
    private SectionNode parseNode;

    @Override
    protected CustomSyntaxStructure.DataTracker<EventSyntaxInfo> getDataTracker() {
        return dataTracker;
    }

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        customEventsUsed = true;
        Script script = parseResult.hasTag("local") ? SkriptUtil.getCurrentScript() : null;
        List<String> patterns = (List<String>)entryContainer.getOptional("patterns", List.class, false);
        String patternNode = (String)entryContainer.getOptional("pattern", String.class, false);
        if (patterns != null && patternNode != null) {
            Skript.error((String)"A custom event may not have both a 'patterns' and 'pattern' entries");
            return false;
        }
        if (patternNode != null) {
            patterns = Collections.singletonList(patternNode);
        }
        if (patterns == null || patterns.isEmpty()) {
            return this.checkHasPatterns();
        }
        int i = 1;
        for (String pattern : patterns) {
            this.register(EventSyntaxInfo.create(script, pattern, i++));
        }
        String name = (String)args[0].getSingle();
        if (nameValues.values().stream().anyMatch(name::equalsIgnoreCase)) {
            Skript.error((String)"There is already a custom event with that name");
            return false;
        }
        this.whichInfo.forEach(which -> nameValues.put((EventSyntaxInfo)which, name));
        super.preLoad();
        return true;
    }

    @Override
    public boolean preLoad() {
        SectionNode[] parseNode = this.getParseNode();
        if (parseNode == null) {
            return false;
        }
        this.parseNode = parseNode[0];
        this.whichInfo.forEach(which -> parseSectionLoaded.put((EventSyntaxInfo)which, this.parseNode == null));
        return true;
    }

    @Override
    public boolean load() {
        SectionNode checkNode;
        EntryContainer entryContainer = this.getEntryContainer();
        List classInfoList = (List)entryContainer.getOptional("event values", List.class, false);
        if (classInfoList != null) {
            SkriptReflection.replaceEventValues(classInfoList);
            this.whichInfo.forEach(which -> eventValueTypes.put((EventSyntaxInfo)which, classInfoList));
        }
        if (this.parseNode != null) {
            SkriptLogger.setNode((Node)this.parseNode);
            SyntaxParseEvent.register(this.parseNode, this.whichInfo, parserHandlers);
            this.whichInfo.forEach(which -> parseSectionLoaded.put((EventSyntaxInfo)which, true));
        }
        if ((checkNode = (SectionNode)entryContainer.getOptional("check", SectionNode.class, false)) != null) {
            SkriptLogger.setNode((Node)checkNode);
            this.getParser().setCurrentEvent("custom event trigger", new Class[]{EventTriggerEvent.class});
            CustomEvent.setLastWhich((EventSyntaxInfo)this.whichInfo.get(0));
            List<TriggerItem> items = SkriptUtil.getItemsFromNode(checkNode);
            CustomEvent.setLastWhich(null);
            this.whichInfo.forEach(which -> eventHandlers.put((EventSyntaxInfo)which, new Trigger(this.getParser().getCurrentScript(), "event " + String.valueOf(which), (SkriptEvent)new SimpleEvent(), items)));
        }
        SkriptLogger.setNode(null);
        return true;
    }

    public static EventSyntaxInfo lookup(Script script, int matchedPattern) {
        return dataTracker.lookup(script, matchedPattern);
    }

    static {
        Skript.registerStructure(StructCustomEvent.class, (EntryValidator)StructCustomEvent.customSyntaxValidator().addEntry("pattern", null, true).addEntryData((EntryData)new EventValuesEntryData("event values", null, true){

            @Override
            public boolean canCreateWith(String node) {
                return super.canCreateWith(node) || node.startsWith(this.getKey().replace(' ', '-') + this.getSeparator());
            }
        }).addSection("check", true).build(), (String[])new String[]{"[:local] [custom] event %string%"});
        dataTracker = new CustomSyntaxStructure.DataTracker();
        nameValues = new HashMap<EventSyntaxInfo, String>();
        eventValueTypes = new HashMap();
        parserHandlers = new HashMap<EventSyntaxInfo, Trigger>();
        eventHandlers = new HashMap<EventSyntaxInfo, Trigger>();
        parseSectionLoaded = new HashMap<EventSyntaxInfo, Boolean>();
        Skript.registerEvent((String)"custom event", CustomEvent.class, BukkitCustomEvent.class, (String[])new String[]{"this is here because at least one pattern is required"});
        Optional<BukkitSyntaxInfos.Event> info = SkriptMirror.getAddonInstance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY).stream().filter(i -> i.type() == CustomEvent.class).findFirst();
        info.ifPresent(dataTracker::setInfo);
        dataTracker.setSyntaxKey(BukkitSyntaxInfos.Event.KEY);
        dataTracker.addManaged(nameValues);
        dataTracker.addManaged(eventValueTypes);
        dataTracker.addManaged(parserHandlers);
        dataTracker.addManaged(eventHandlers);
        dataTracker.addManaged(parseSectionLoaded);
    }
}

