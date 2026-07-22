/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.lang.parser;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.config.Config;
import ch.njol.skript.config.Node;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.lang.parser.ExpressionParseCache;
import ch.njol.skript.lang.parser.LiteralParseCache;
import ch.njol.skript.lang.parser.ParsingStack;
import ch.njol.skript.log.HandlerList;
import ch.njol.skript.structures.StructOptions;
import ch.njol.skript.variables.HintManager;
import ch.njol.util.Kleenean;
import com.google.common.base.Preconditions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.experiment.ExperimentSet;
import org.skriptlang.skript.lang.experiment.Experimented;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

public final class ParserInstance
implements Experimented {
    private static final ThreadLocal<ParserInstance> PARSER_INSTANCES = ThreadLocal.withInitial(ParserInstance::new);
    private boolean isActive = false;
    private final ExpressionParseCache expressionParseCache = new ExpressionParseCache();
    private final LiteralParseCache literalParseCache = new LiteralParseCache();
    @Nullable
    private Script currentScript = null;
    @Nullable
    private Structure currentStructure = null;
    @Nullable
    private String currentEventName;
    private Class<? extends Event> @Nullable [] currentEvents = null;
    private List<TriggerSection> currentSections = new ArrayList<TriggerSection>();
    private Kleenean hasDelayBefore = Kleenean.FALSE;
    private final HandlerList handlers = new HandlerList();
    @Nullable
    private Node node;
    private String indentation = "";
    private final ParsingStack parsingStack = new ParsingStack();
    private HintManager hintManager = new HintManager(true);
    private static final Map<Class<? extends Data>, Function<ParserInstance, ? extends Data>> dataRegister = new HashMap<Class<? extends Data>, Function<ParserInstance, ? extends Data>>();
    private final Map<Class<? extends Data>, Data> dataMap = new HashMap<Class<? extends Data>, Data>();

    public static ParserInstance get() {
        return PARSER_INSTANCES.get();
    }

    @ApiStatus.Internal
    public void setInactive() {
        this.isActive = false;
        this.reset();
        this.literalParseCache.clear();
        this.setCurrentScript((Script)null);
    }

    @ApiStatus.Internal
    public void setActive(Script script) {
        this.reset();
        this.hintManager.setActive(true);
        this.isActive = true;
        this.setCurrentScript(script);
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void reset() {
        this.currentStructure = null;
        this.currentEventName = null;
        this.currentEvents = null;
        this.currentSections = new ArrayList<TriggerSection>();
        this.hasDelayBefore = Kleenean.FALSE;
        this.node = null;
        this.hintManager = new HintManager(this.hintManager.isActive());
        this.dataMap.clear();
        this.expressionParseCache.clear();
    }

    public ExpressionParseCache getExpressionParseCache() {
        return this.expressionParseCache;
    }

    public LiteralParseCache getLiteralParseCache() {
        return this.literalParseCache;
    }

    private void setCurrentScript(@Nullable Script currentScript) {
        if (currentScript == this.currentScript) {
            return;
        }
        Script previous = this.currentScript;
        this.currentScript = currentScript;
        this.getDataInstances().forEach(data -> data.onCurrentScriptChange(currentScript != null ? currentScript.getConfig() : null));
        if (previous != null) {
            ScriptLoader.eventRegistry().events(ScriptActivityChangeEvent.class).forEach(event -> event.onActivityChange(this, previous, false, currentScript));
            previous.eventRegistry().events(ScriptActivityChangeEvent.class).forEach(event -> event.onActivityChange(this, previous, false, currentScript));
        }
        if (currentScript != null) {
            ScriptLoader.eventRegistry().events(ScriptActivityChangeEvent.class).forEach(event -> event.onActivityChange(this, currentScript, true, previous));
            currentScript.eventRegistry().events(ScriptActivityChangeEvent.class).forEach(event -> event.onActivityChange(this, currentScript, true, previous));
        }
    }

    public Script getCurrentScript() {
        if (this.currentScript == null) {
            throw new SkriptAPIException("This ParserInstance is not currently parsing/loading something!");
        }
        return this.currentScript;
    }

    public void setCurrentStructure(@Nullable Structure structure) {
        this.currentStructure = structure;
    }

    @Nullable
    public Structure getCurrentStructure() {
        return this.currentStructure;
    }

    public boolean isCurrentStructure(Class<? extends Structure> structureClass) {
        return structureClass.isInstance(this.currentStructure);
    }

    @SafeVarargs
    public final boolean isCurrentStructure(Class<? extends Structure> ... structureClasses) {
        for (Class<? extends Structure> structureClass : structureClasses) {
            if (!this.isCurrentStructure(structureClass)) continue;
            return true;
        }
        return false;
    }

    public void setCurrentEventName(@Nullable String currentEventName) {
        this.currentEventName = currentEventName;
    }

    @Nullable
    public String getCurrentEventName() {
        return this.currentEventName;
    }

    public void setCurrentEvents(Class<? extends Event> @Nullable [] currentEvents) {
        this.currentEvents = currentEvents;
        this.getDataInstances().forEach(data -> data.onCurrentEventsChange(currentEvents));
    }

    @SafeVarargs
    public final void setCurrentEvent(String name, Class<? extends Event> ... events) {
        this.currentEventName = name;
        this.setCurrentEvents(events);
        this.setHasDelayBefore(Kleenean.FALSE);
    }

    public void deleteCurrentEvent() {
        this.currentEventName = null;
        this.setCurrentEvents(null);
        this.setHasDelayBefore(Kleenean.FALSE);
    }

    public Class<? extends Event> @Nullable [] getCurrentEvents() {
        return this.currentEvents;
    }

    public boolean isCurrentEvent(Class<? extends Event> event) {
        if (this.currentEvents == null) {
            return false;
        }
        for (Class<? extends Event> currentEvent : this.currentEvents) {
            if (!event.isAssignableFrom(currentEvent)) continue;
            return true;
        }
        return false;
    }

    @SafeVarargs
    public final boolean isCurrentEvent(Class<? extends Event> ... events) {
        for (Class<? extends Event> event : events) {
            if (!this.isCurrentEvent(event)) continue;
            return true;
        }
        return false;
    }

    public void setCurrentSections(List<TriggerSection> currentSections) {
        this.currentSections = currentSections;
    }

    public List<TriggerSection> getCurrentSections() {
        return this.currentSections;
    }

    @Nullable
    public <T extends TriggerSection> T getCurrentSection(Class<T> sectionClass) {
        int i = this.currentSections.size();
        while (i-- > 0) {
            TriggerSection triggerSection = this.currentSections.get(i);
            if (!sectionClass.isInstance(triggerSection)) continue;
            return (T)triggerSection;
        }
        return null;
    }

    @NotNull
    public <T extends TriggerSection> List<T> getCurrentSections(Class<T> sectionClass) {
        ArrayList<TriggerSection> list = new ArrayList<TriggerSection>();
        for (TriggerSection triggerSection : this.currentSections) {
            if (!sectionClass.isInstance(triggerSection)) continue;
            list.add(triggerSection);
        }
        return list;
    }

    public List<TriggerSection> getSectionsUntil(TriggerSection section) {
        return new ArrayList<TriggerSection>(this.currentSections.subList(this.currentSections.indexOf(section) + 1, this.currentSections.size()));
    }

    public List<TriggerSection> getSections(int levels) {
        Preconditions.checkArgument((levels > 0 ? 1 : 0) != 0, (Object)"Depth must be at least 1");
        return new ArrayList<TriggerSection>(this.currentSections.subList(Math.max(this.currentSections.size() - levels, 0), this.currentSections.size()));
    }

    public List<TriggerSection> getSections(int levels, Class<? extends TriggerSection> type) {
        Preconditions.checkArgument((levels > 0 ? 1 : 0) != 0, (Object)"Depth must be at least 1");
        List<? extends TriggerSection> sections = this.getCurrentSections(type);
        if (sections.isEmpty()) {
            return new ArrayList<TriggerSection>();
        }
        TriggerSection section = sections.get(Math.max(sections.size() - levels, 0));
        return new ArrayList<TriggerSection>(this.currentSections.subList(this.currentSections.indexOf(section), this.currentSections.size()));
    }

    public boolean isCurrentSection(Class<? extends TriggerSection> sectionClass) {
        for (TriggerSection triggerSection : this.currentSections) {
            if (!sectionClass.isInstance(triggerSection)) continue;
            return true;
        }
        return false;
    }

    @SafeVarargs
    public final boolean isCurrentSection(Class<? extends TriggerSection> ... sectionClasses) {
        for (Class<? extends TriggerSection> sectionClass : sectionClasses) {
            if (!this.isCurrentSection(sectionClass)) continue;
            return true;
        }
        return false;
    }

    public void setHasDelayBefore(Kleenean hasDelayBefore) {
        this.hasDelayBefore = hasDelayBefore;
    }

    public Kleenean getHasDelayBefore() {
        return this.hasDelayBefore;
    }

    public HandlerList getHandlers() {
        return this.handlers;
    }

    public void setNode(@Nullable Node node) {
        this.node = node == null || node.getParent() == null ? null : node;
    }

    @Nullable
    public Node getNode() {
        return this.node;
    }

    public void setIndentation(String indentation) {
        this.indentation = indentation;
    }

    public String getIndentation() {
        return this.indentation;
    }

    public ParsingStack getParsingStack() {
        return this.parsingStack;
    }

    @Override
    public boolean hasExperiment(String featureName) {
        return this.isActive() && Skript.experiments().isUsing(this.getCurrentScript(), featureName);
    }

    @Override
    public boolean hasExperiment(Experiment experiment) {
        return this.isActive() && Skript.experiments().isUsing(this.getCurrentScript(), experiment);
    }

    @ApiStatus.Internal
    public void addExperiment(Experiment experiment) {
        Script script = this.getCurrentScript();
        ExperimentSet set = script.getData(ExperimentSet.class, () -> new ExperimentSet());
        set.add(experiment);
    }

    @ApiStatus.Internal
    public void removeExperiment(Experiment experiment) {
        Script script = this.getCurrentScript();
        @Nullable ExperimentSet set = script.getData(ExperimentSet.class);
        if (set == null) {
            return;
        }
        set.remove(experiment);
    }

    public Experimented experimentSnapshot() {
        if (!this.isActive()) {
            return new ExperimentSet();
        }
        Script script = this.getCurrentScript();
        @Nullable ExperimentSet set = script.getData(ExperimentSet.class);
        if (set == null) {
            return new ExperimentSet();
        }
        return new ExperimentSet(set);
    }

    public ExperimentSet getExperimentSet() {
        if (!this.isActive()) {
            return new ExperimentSet();
        }
        Script script = this.getCurrentScript();
        ExperimentSet set = script.getData(ExperimentSet.class);
        if (set == null) {
            return new ExperimentSet();
        }
        return set;
    }

    @ApiStatus.Experimental
    public HintManager getHintManager() {
        return this.hintManager;
    }

    public static <T extends Data> void registerData(Class<T> dataClass, Function<ParserInstance, T> dataFunction) {
        dataRegister.put(dataClass, dataFunction);
    }

    public static boolean isRegistered(Class<? extends Data> dataClass) {
        return dataRegister.containsKey(dataClass);
    }

    @NotNull
    public <T extends Data> T getData(Class<T> dataClass) {
        if (this.dataMap.containsKey(dataClass)) {
            return (T)this.dataMap.get(dataClass);
        }
        if (dataRegister.containsKey(dataClass)) {
            Data data = dataRegister.get(dataClass).apply(this);
            this.dataMap.put(dataClass, data);
            return (T)data;
        }
        assert (false);
        return null;
    }

    @NotNull
    private List<? extends Data> getDataInstances() {
        ArrayList<Data> dataList = new ArrayList<Data>();
        for (Class<? extends Data> dataClass : dataRegister.keySet()) {
            Data data = this.getData(dataClass);
            dataList.add(data);
        }
        return dataList;
    }

    public Backup backup() {
        if (!this.isActive()) {
            throw new SkriptAPIException("Backups may only be created from active ParserInstances");
        }
        return new Backup(this);
    }

    public void restoreBackup(Backup backup) {
        backup.apply(this);
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public HashMap<String, String> getCurrentOptions() {
        if (!this.isActive()) {
            return new HashMap<String, String>(0);
        }
        StructOptions.OptionsData data = this.getCurrentScript().getData(StructOptions.OptionsData.class);
        if (data == null) {
            return new HashMap<String, String>(0);
        }
        return new HashMap<String, String>(data.getOptions());
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    @Nullable
    public SkriptEvent getCurrentSkriptEvent() {
        Structure structure = this.getCurrentStructure();
        if (structure instanceof SkriptEvent) {
            SkriptEvent event = (SkriptEvent)structure;
            return event;
        }
        return null;
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public void setCurrentSkriptEvent(@Nullable SkriptEvent currentSkriptEvent) {
        this.setCurrentStructure(currentSkriptEvent);
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public void deleteCurrentSkriptEvent() {
        this.setCurrentStructure(null);
    }

    @Deprecated(since="2.7.0", forRemoval=true)
    public void setCurrentScript(@Nullable Config currentScript) {
        if (currentScript == null) {
            return;
        }
        File file = currentScript.getFile();
        if (file == null) {
            return;
        }
        Script script = ScriptLoader.getScript(file);
        if (script != null) {
            this.setActive(script);
        }
    }

    @FunctionalInterface
    public static interface ScriptActivityChangeEvent
    extends ScriptLoader.LoaderEvent,
    Script.Event {
        public void onActivityChange(ParserInstance var1, Script var2, boolean var3, @Nullable Script var4);
    }

    public static abstract class Data {
        private final ParserInstance parserInstance;

        public Data(ParserInstance parserInstance) {
            this.parserInstance = parserInstance;
        }

        protected final ParserInstance getParser() {
            return this.parserInstance;
        }

        @Deprecated(since="2.11.0", forRemoval=true)
        public void onCurrentScriptChange(@Nullable Config currentScript) {
        }

        public void onCurrentEventsChange(Class<? extends Event> @Nullable [] currentEvents) {
        }
    }

    public static class Backup {
        private final Script currentScript;
        @Nullable
        private final Structure currentStructure;
        @Nullable
        private final String currentEventName;
        private final Class<? extends Event> @Nullable [] currentEvents;
        private final List<TriggerSection> currentSections;
        private final Kleenean hasDelayBefore;
        private final HintManager hintManager;
        private final Map<Class<? extends Data>, Data> dataMap;

        private Backup(ParserInstance parser) {
            this.currentScript = parser.currentScript;
            this.currentStructure = parser.currentStructure;
            this.currentEventName = parser.currentEventName != null ? parser.currentEventName : null;
            this.currentEvents = parser.currentEvents != null ? Arrays.copyOf(parser.currentEvents, parser.currentEvents.length) : null;
            this.currentSections = new ArrayList<TriggerSection>(parser.currentSections);
            this.hasDelayBefore = parser.hasDelayBefore;
            this.hintManager = parser.hintManager;
            this.dataMap = new HashMap<Class<? extends Data>, Data>(parser.dataMap);
        }

        private void apply(ParserInstance parser) {
            parser.setCurrentScript(this.currentScript);
            parser.currentStructure = this.currentStructure;
            parser.currentEventName = this.currentEventName;
            parser.currentEvents = this.currentEvents;
            parser.currentSections = this.currentSections;
            parser.hasDelayBefore = this.hasDelayBefore;
            parser.hintManager = this.hintManager;
            parser.dataMap.clear();
            parser.dataMap.putAll(this.dataMap);
        }
    }
}

