/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.Skript
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.skript.lang.VariableString
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.skriptlang.skript.lang.entry.EntryData
 *  org.skriptlang.skript.lang.entry.EntryValidator
 *  org.skriptlang.skript.lang.entry.EntryValidator$EntryValidatorBuilder
 *  org.skriptlang.skript.lang.script.Script
 *  org.skriptlang.skript.lang.structure.Structure
 *  org.skriptlang.skript.lang.structure.Structure$Priority
 *  org.skriptlang.skript.registration.SyntaxInfo
 *  org.skriptlang.skript.registration.SyntaxRegistry
 *  org.skriptlang.skript.registration.SyntaxRegistry$Key
 */
package org.skriptlang.reflect.syntax;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.VariableString;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.SkriptMirror;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.skriptlang.reflect.java.elements.structures.StructImport;
import org.skriptlang.reflect.syntax.PatternsEntryData;
import org.skriptlang.reflect.syntax.event.BukkitCustomEvent;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.elements.CustomEvent;
import org.skriptlang.reflect.syntax.event.elements.CustomEventUtils;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public abstract class CustomSyntaxStructure<T extends SyntaxData>
extends Structure {
    public static final String DEFAULT_PATTERN = "this is here because at least one pattern is required";
    public static final Structure.Priority PRIORITY = new Structure.Priority(350);
    protected List<T> whichInfo = new ArrayList<T>();
    private boolean hasPatterns = false;

    protected abstract DataTracker<T> getDataTracker();

    public boolean preLoad() {
        this.update();
        return true;
    }

    public boolean load() {
        return true;
    }

    public void unload() {
        this.whichInfo.forEach(which -> {
            Map<Script, Map<String, T>> primaryData = this.getDataTracker().getPrimaryData();
            Script script = which.getScript();
            Map<String, T> syntaxes = primaryData.get(script);
            syntaxes.remove(which.getPattern());
            if (syntaxes.isEmpty()) {
                primaryData.remove(script);
            }
            this.getDataTracker().getManagedData().forEach(data -> data.remove(which));
        });
        this.update();
    }

    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    public String toString(Event e, boolean debug) {
        return null;
    }

    private void update() {
        this.getDataTracker().recomputePatterns();
        SyntaxRegistry syntaxRegistry = SkriptMirror.getAddonInstance().syntaxRegistry();
        SyntaxInfo<?> oldSyntaxInfo = this.getDataTracker().getInfo();
        syntaxRegistry.unregister(this.getDataTracker().getSyntaxKey(), oldSyntaxInfo);
        SyntaxInfo newSyntaxInfo = oldSyntaxInfo.toBuilder().clearPatterns().addPatterns(this.getDataTracker().getPatterns()).build();
        syntaxRegistry.register(this.getDataTracker().getSyntaxKey(), newSyntaxInfo);
        this.getDataTracker().setInfo(newSyntaxInfo);
    }

    protected final void register(T data) {
        this.hasPatterns = true;
        String pattern = ((SyntaxData)data).getPattern();
        this.whichInfo.add(data);
        this.getDataTracker().getPrimaryData().computeIfAbsent(((SyntaxData)data).getScript(), f -> new HashMap()).put(pattern, data);
    }

    protected boolean checkHasPatterns() {
        if (this.hasPatterns) {
            return true;
        }
        Skript.error((String)"A custom syntax must have at least one pattern");
        return false;
    }

    protected SectionNode[] getParseNode() {
        SectionNode parseNode = (SectionNode)this.getEntryContainer().getOptional("parse", SectionNode.class, false);
        SectionNode safeParseNode = (SectionNode)this.getEntryContainer().getOptional("safe parse", SectionNode.class, false);
        if (safeParseNode != null) {
            if (parseNode != null) {
                Skript.error((String)"A custom syntax element cannot contain both 'parse' and 'safe parse' entries");
                return null;
            }
            Skript.warning((String)"The 'safe parse' entry is deprecated and will act as a regular 'parse' entry. Please use the 'parse' entry instead");
            return new SectionNode[]{safeParseNode};
        }
        return new SectionNode[]{parseNode};
    }

    protected boolean handleUsableEntry(SectionNode sectionNode, Map<T, List<Supplier<Boolean>>> usableSuppliers) {
        Script currentScript = SkriptUtil.getCurrentScript();
        for (Node usableNode : sectionNode) {
            Supplier<Boolean> supplier;
            String usableKey = usableNode.getKey();
            assert (usableKey != null);
            if (usableKey.startsWith("custom event ")) {
                String customEventString = usableKey.substring("custom event ".length());
                VariableString variableString = VariableString.newInstance((String)customEventString.substring(1, customEventString.length() - 1));
                if (variableString == null || !variableString.isSimple()) {
                    Skript.error((String)"Custom event identifiers may only be simple strings");
                    return false;
                }
                String identifier = variableString.toString(null);
                supplier = () -> {
                    if (!this.getParser().isCurrentEvent(BukkitCustomEvent.class)) {
                        return false;
                    }
                    EventSyntaxInfo eventWhich = CustomEvent.lastWhich;
                    return CustomEventUtils.getName(eventWhich).equalsIgnoreCase(identifier);
                };
            } else {
                Class<?> javaClass;
                JavaType javaType = StructImport.lookup(currentScript, usableKey);
                Class<?> clazz = javaClass = javaType == null ? null : javaType.getJavaClass();
                if (javaClass == null || !Event.class.isAssignableFrom(javaClass)) {
                    Skript.error((String)(String.valueOf(javaType) + " is not a Bukkit event"));
                    return false;
                }
                Class<?> eventClass = javaClass;
                supplier = () -> this.getParser().isCurrentEvent(eventClass);
            }
            this.whichInfo.forEach(which -> usableSuppliers.computeIfAbsent(which, whichIndex -> new ArrayList()).add(supplier));
        }
        return true;
    }

    public T getFirstWhich() {
        return (T)((SyntaxData)this.whichInfo.get(0));
    }

    public static EntryValidator.EntryValidatorBuilder customSyntaxValidator() {
        return EntryValidator.builder().addEntryData((EntryData)new PatternsEntryData("patterns", null, true)).addSection("parse", true).addSection("safe parse", true).addSection("usable in", true);
    }

    public static class DataTracker<T> {
        private List<String> patterns = new ArrayList<String>();
        private final Map<Script, Map<String, T>> primaryData = new HashMap<Script, Map<String, T>>();
        private final List<Map<T, ?>> managedData = new ArrayList();
        private SyntaxRegistry.Key<?> syntaxKey;
        private SyntaxInfo<?> info;

        public List<String> getPatterns() {
            return this.patterns;
        }

        public Map<Script, Map<String, T>> getPrimaryData() {
            return this.primaryData;
        }

        public List<Map<T, ?>> getManagedData() {
            return this.managedData;
        }

        public SyntaxInfo<?> getInfo() {
            return this.info;
        }

        public void recomputePatterns() {
            this.patterns = this.primaryData.values().stream().map(Map::keySet).flatMap(Collection::stream).distinct().collect(Collectors.toList());
            this.patterns.add(0, CustomSyntaxStructure.DEFAULT_PATTERN);
        }

        public void addManaged(Map<T, ?> data) {
            this.managedData.add(data);
        }

        public void setInfo(SyntaxInfo<?> info) {
            this.info = info;
        }

        public final T lookup(Script script, int matchedPattern) {
            T privateResult;
            String originalSyntax = this.patterns.get(matchedPattern);
            Map<String, T> localSyntax = this.primaryData.get(script);
            if (localSyntax != null && (privateResult = localSyntax.get(originalSyntax)) != null) {
                return privateResult;
            }
            Map<String, T> globalSyntax = this.primaryData.get(null);
            if (globalSyntax == null || !globalSyntax.containsKey(originalSyntax)) {
                return null;
            }
            return globalSyntax.get(originalSyntax);
        }

        public SyntaxRegistry.Key<?> getSyntaxKey() {
            return this.syntaxKey;
        }

        public void setSyntaxKey(SyntaxRegistry.Key<?> syntaxKey) {
            this.syntaxKey = syntaxKey;
        }
    }

    public static abstract class SyntaxData {
        private final Script script;
        private final String pattern;
        private final int matchedPattern;

        protected SyntaxData(Script script, String pattern, int matchedPattern) {
            this.script = script;
            this.pattern = pattern;
            this.matchedPattern = matchedPattern;
        }

        public Script getScript() {
            return this.script;
        }

        public String getPattern() {
            return this.pattern;
        }

        public int getMatchedPattern() {
            return this.matchedPattern;
        }

        public String toString() {
            return this.pattern;
        }
    }

    public static class CustomSyntaxEvent
    extends Event {
        private CustomSyntaxEvent() {
        }

        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}

