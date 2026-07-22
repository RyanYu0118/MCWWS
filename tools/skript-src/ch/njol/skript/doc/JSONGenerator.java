/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  com.google.common.collect.Multimap
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonPrimitive
 *  org.bukkit.Bukkit
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockCanBuildEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.doc;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Documentable;
import ch.njol.skript.doc.Documentation;
import ch.njol.skript.doc.DocumentationGenerator;
import ch.njol.skript.doc.DocumentationIdProvider;
import ch.njol.skript.doc.Events;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.doc.RelatedProperty;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.registrations.Feature;
import ch.njol.skript.util.Version;
import ch.njol.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyRegistry;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.registration.SyntaxRegistry;

public class JSONGenerator
extends DocumentationGenerator {
    public static final Version JSON_VERSION = new Version(2, 0);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().serializeNulls().create();
    private static final Map<Property<?>, Set<SyntaxInfo<?>>> PROPERTY_RELATED_SYNTAXES = new HashMap();
    private static final PropertyRegistry PROPERTY_REGISTRY = Skript.instance().registry(PropertyRegistry.class);
    @NotNull
    private final SkriptAddon source;

    @Contract(value="_ -> new")
    public static JSONGenerator of(@NotNull SkriptAddon source) {
        return new JSONGenerator(source);
    }

    private JSONGenerator(@NotNull SkriptAddon source) {
        super(Documentation.getDocsTemplateDirectory(), Documentation.getDocsOutputDirectory());
        Preconditions.checkNotNull((Object)source, (Object)"addon cannot be null");
        this.source = source;
    }

    @Deprecated(forRemoval=true, since="2.13")
    public JSONGenerator(File templateDir, File outputDir) {
        super(templateDir, outputDir);
        this.source = Skript.instance();
    }

    private static JsonObject getVersion() {
        JsonObject version = new JsonObject();
        version.addProperty("major", (Number)JSON_VERSION.getMajor());
        version.addProperty("minor", (Number)JSON_VERSION.getMinor());
        return version;
    }

    private static JsonArray convertToJsonArray(String ... strings) {
        if (strings == null || strings.length == 0) {
            return null;
        }
        JsonArray jsonArray = new JsonArray();
        for (String string : strings) {
            jsonArray.add((JsonElement)new JsonPrimitive(string));
        }
        return jsonArray;
    }

    private JsonObject generatedAnnotatedElement(SyntaxInfo<?> syntaxInfo) {
        Class<?> syntaxClass = syntaxInfo.type();
        Name name = syntaxClass.getAnnotation(Name.class);
        if (name == null || syntaxClass.getAnnotation(NoDoc.class) != null) {
            return null;
        }
        JsonObject syntaxJsonObject = new JsonObject();
        syntaxJsonObject.addProperty("id", DocumentationIdProvider.getId(syntaxInfo));
        syntaxJsonObject.addProperty("name", name.value());
        Since since = syntaxClass.getAnnotation(Since.class);
        syntaxJsonObject.add("since", since == null ? null : JSONGenerator.convertToJsonArray(since.value()));
        Deprecated deprecated = syntaxClass.getAnnotation(Deprecated.class);
        syntaxJsonObject.addProperty("deprecated", Boolean.valueOf(deprecated != null));
        Description description = syntaxClass.getAnnotation(Description.class);
        syntaxJsonObject.add("description", description == null ? null : JSONGenerator.convertToJsonArray(description.value()));
        syntaxJsonObject.add("patterns", (JsonElement)JSONGenerator.cleanPatterns(syntaxInfo.patterns().toArray(new String[0])));
        RelatedProperty relatedProperty = syntaxClass.getAnnotation(RelatedProperty.class);
        Property<?> property = null;
        if (relatedProperty != null && (property = PROPERTY_REGISTRY.get(relatedProperty.value())) != null) {
            PROPERTY_RELATED_SYNTAXES.computeIfAbsent(property, key -> new HashSet()).add(syntaxInfo);
        }
        syntaxJsonObject.add("property", (JsonElement)(property == null ? null : JSONGenerator.getPropertyDetails(property)));
        syntaxJsonObject.add("propertyTypes", (JsonElement)(property == null ? null : JSONGenerator.getPropertyRelatedClassInfos(property)));
        if (syntaxClass.isAnnotationPresent(Examples.class)) {
            examplesAnnotation = syntaxClass.getAnnotation(Examples.class);
            syntaxJsonObject.add("examples", (JsonElement)JSONGenerator.convertToJsonArray(examplesAnnotation.value()));
        } else if (syntaxClass.isAnnotationPresent(Example.Examples.class)) {
            examplesAnnotation = syntaxClass.getAnnotation(Example.Examples.class);
            syntaxJsonObject.add("examples", (JsonElement)JSONGenerator.convertToJsonArray((String[])Arrays.stream(examplesAnnotation.value()).map(Example::value).toArray(String[]::new)));
        } else if (syntaxClass.isAnnotationPresent(Example.class)) {
            @NotNull Example example = syntaxClass.getAnnotation(Example.class);
            syntaxJsonObject.add("examples", (JsonElement)JSONGenerator.convertToJsonArray(example.value()));
        } else {
            syntaxJsonObject.add("examples", null);
        }
        syntaxJsonObject.add("events", (JsonElement)this.getAnnotatedEvents(syntaxClass.getAnnotation(Events.class)));
        RequiredPlugins requirements = syntaxClass.getAnnotation(RequiredPlugins.class);
        syntaxJsonObject.add("requirements", requirements == null ? null : JSONGenerator.convertToJsonArray(requirements.value()));
        Keywords keywords = syntaxClass.getAnnotation(Keywords.class);
        syntaxJsonObject.add("keywords", keywords == null ? null : JSONGenerator.convertToJsonArray(keywords.value()));
        if (syntaxInfo instanceof DefaultSyntaxInfos.Expression) {
            DefaultSyntaxInfos.Expression expression = (DefaultSyntaxInfos.Expression)syntaxInfo;
            syntaxJsonObject.add("returns", (JsonElement)JSONGenerator.getExpressionReturnTypes(expression));
        }
        return syntaxJsonObject;
    }

    @Nullable
    private JsonArray getAnnotatedEvents(Events events) {
        if (events == null || events.value() == null) {
            return null;
        }
        JsonArray array = new JsonArray();
        for (String event : events.value()) {
            String id;
            BukkitSyntaxInfos.Event info;
            JsonObject object = new JsonObject();
            ArrayList candidates = new ArrayList();
            for (BukkitSyntaxInfos.Event<?> info2 : this.source.syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY)) {
                String infoName = info2.name().toLowerCase(Locale.ENGLISH);
                if (infoName.startsWith("on ")) {
                    infoName = infoName.substring(3);
                }
                if (infoName.equals(event.toLowerCase(Locale.ENGLISH)) || info2.id().equals(event)) {
                    candidates.add(info2);
                    continue;
                }
                if (!event.equals(info2.documentationId())) continue;
                candidates.clear();
                candidates.add(info2);
                break;
            }
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("No matching info found for event annotation: " + event);
            }
            if (candidates.size() == 1) {
                info = (BukkitSyntaxInfos.Event)candidates.getFirst();
                id = info.documentationId();
                if (id == null) {
                    id = info.id();
                }
            } else {
                throw new IllegalArgumentException("Multiple matching info found for event annotation: " + event + "\nDifferentiate by specifying a documentationId on the relevant event infos.");
            }
            String name = info.name();
            object.addProperty("id", id);
            object.addProperty("name", name);
            array.add((JsonElement)object);
        }
        return array;
    }

    @NotNull
    private static JsonObject getExpressionReturnTypes(DefaultSyntaxInfos.Expression<?, ?> expression) {
        ClassInfo<?> exact = Classes.getSuperClassInfo(expression.returnType());
        String name = Objects.requireNonNullElse(exact.getDocName(), exact.getName().getSingular());
        if (name.equals(ClassInfo.NO_DOC)) {
            exact = Classes.getSuperClassInfo(expression.returnType().getSuperclass());
            name = Objects.requireNonNullElse(exact.getDocName(), exact.getName().getSingular());
        }
        JsonObject object = new JsonObject();
        object.addProperty("id", exact.getCodeName());
        object.addProperty("name", name);
        return object;
    }

    private static JsonObject generateEventElement(BukkitSyntaxInfos.Event<?> info) {
        JsonObject syntaxJsonObject = new JsonObject();
        syntaxJsonObject.addProperty("id", DocumentationIdProvider.getId(info));
        syntaxJsonObject.addProperty("name", info.name());
        syntaxJsonObject.addProperty("cancellable", Boolean.valueOf(JSONGenerator.isCancellable(info)));
        syntaxJsonObject.add("since", (JsonElement)JSONGenerator.convertToJsonArray(info.since().toArray(new String[0])));
        syntaxJsonObject.add("patterns", (JsonElement)JSONGenerator.cleanPatterns(info.patterns().toArray(new String[0])));
        syntaxJsonObject.add("description", (JsonElement)JSONGenerator.convertToJsonArray(info.description().toArray(new String[0])));
        syntaxJsonObject.add("requirements", (JsonElement)JSONGenerator.convertToJsonArray(info.requiredPlugins().toArray(new String[0])));
        syntaxJsonObject.add("examples", (JsonElement)JSONGenerator.convertToJsonArray(info.examples().toArray(new String[0])));
        syntaxJsonObject.add("eventValues", (JsonElement)JSONGenerator.getEventValues(info));
        syntaxJsonObject.add("keywords", (JsonElement)JSONGenerator.convertToJsonArray(info.keywords().toArray(new String[0])));
        return syntaxJsonObject;
    }

    private static JsonArray getEventValues(BukkitSyntaxInfos.Event<?> info) {
        HashSet<JsonObject> eventValues = new HashSet<JsonObject>();
        Multimap<Class<? extends Event>, EventValues.EventValueInfo<?, ?>> allEventValues = EventValues.getPerEventEventValues();
        for (Class<Event> supportedEvent : info.events()) {
            for (Map.Entry entry : allEventValues.entries()) {
                Class<E>[] excludes;
                Class event = (Class)entry.getKey();
                EventValues.EventValueInfo eventValueInfo = (EventValues.EventValueInfo)entry.getValue();
                if (event == null || !event.isAssignableFrom(supportedEvent) || (excludes = eventValueInfo.excludes()) != null && Set.of(excludes).contains(event)) continue;
                Class valueClass = eventValueInfo.valueClass();
                ClassInfo classInfo = valueClass.isArray() ? Classes.getSuperClassInfo(valueClass.componentType()) : Classes.getSuperClassInfo(valueClass);
                Object name = classInfo.getName().getSingular();
                if (valueClass.isArray()) {
                    name = classInfo.getName().getPlural();
                }
                if (((String)name).isBlank()) continue;
                if (eventValueInfo.time() == EventValues.TIME_PAST) {
                    name = "past " + (String)name;
                } else if (eventValueInfo.time() == EventValues.TIME_FUTURE) {
                    name = "future " + (String)name;
                }
                JsonObject object = new JsonObject();
                object.addProperty("id", DocumentationIdProvider.getId(classInfo));
                object.addProperty("name", ((String)name).toLowerCase(Locale.ENGLISH));
                eventValues.add(object);
            }
        }
        if (eventValues.isEmpty()) {
            return null;
        }
        JsonArray array = new JsonArray();
        for (JsonObject eventValue : eventValues) {
            array.add((JsonElement)eventValue);
        }
        return array;
    }

    private static boolean isCancellable(BukkitSyntaxInfos.Event<?> info) {
        boolean cancellable = false;
        for (Class<Event> event : info.events()) {
            if (!Cancellable.class.isAssignableFrom(event) && !BlockCanBuildEvent.class.isAssignableFrom(event)) continue;
            cancellable = true;
            break;
        }
        return cancellable;
    }

    private <T extends SyntaxInfo<? extends Structure>> JsonArray generateStructureElementArray(Collection<T> infos) {
        JsonArray syntaxArray = new JsonArray();
        infos.forEach(info -> {
            if (info instanceof BukkitSyntaxInfos.Event) {
                BukkitSyntaxInfos.Event eventInfo = (BukkitSyntaxInfos.Event)info;
                syntaxArray.add((JsonElement)JSONGenerator.generateEventElement(eventInfo));
            } else {
                JsonObject structureElementJsonObject = this.generatedAnnotatedElement((SyntaxInfo<?>)info);
                if (structureElementJsonObject != null) {
                    syntaxArray.add((JsonElement)structureElementJsonObject);
                }
            }
        });
        return syntaxArray;
    }

    private <T extends SyntaxInfo<? extends SyntaxElement>> JsonArray generateSyntaxElementArray(Collection<T> infos) {
        JsonArray syntaxArray = new JsonArray();
        infos.forEach(info -> {
            JsonObject syntaxJsonObject = this.generatedAnnotatedElement((SyntaxInfo<?>)info);
            if (syntaxJsonObject != null) {
                syntaxArray.add((JsonElement)syntaxJsonObject);
            }
        });
        return syntaxArray;
    }

    private static JsonObject generateClassInfoElement(ClassInfo<?> classInfo) {
        if (!classInfo.hasDocs()) {
            return null;
        }
        JsonObject syntaxJsonObject = new JsonObject();
        syntaxJsonObject.addProperty("id", DocumentationIdProvider.getId(classInfo));
        syntaxJsonObject.addProperty("name", Objects.requireNonNullElse(classInfo.getDocName(), classInfo.getCodeName()));
        syntaxJsonObject.addProperty("since", classInfo.getSince());
        syntaxJsonObject.add("patterns", (JsonElement)JSONGenerator.cleanPatterns(classInfo.getUsage()));
        syntaxJsonObject.add("description", (JsonElement)JSONGenerator.convertToJsonArray(classInfo.getDescription()));
        syntaxJsonObject.add("requirements", (JsonElement)JSONGenerator.convertToJsonArray(classInfo.getRequiredPlugins()));
        syntaxJsonObject.add("examples", (JsonElement)JSONGenerator.convertToJsonArray(classInfo.getExamples()));
        syntaxJsonObject.add("properties", (JsonElement)JSONGenerator.getClassInfoProperties(classInfo));
        return syntaxJsonObject;
    }

    private static JsonArray getClassInfoProperties(ClassInfo<?> classInfo) {
        JsonArray array = new JsonArray();
        for (Property<?> property : classInfo.getAllProperties()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", DocumentationIdProvider.getId(property));
            object.addProperty("name", property.name());
            ClassInfo.PropertyDocs docs = classInfo.getPropertyDocumentation(property);
            object.addProperty("description", docs.description());
            object.addProperty("provider", docs.provider().name());
            object.add("relatedSyntax", JSONGenerator.getPropertyRelatedSyntaxes(property));
            array.add((JsonElement)object);
        }
        return array;
    }

    private static JsonArray generateClassInfoArray(Iterator<ClassInfo<?>> classInfos) {
        JsonArray syntaxArray = new JsonArray();
        classInfos.forEachRemaining(classInfo -> {
            JsonObject classInfoElement = JSONGenerator.generateClassInfoElement(classInfo);
            if (classInfoElement != null) {
                syntaxArray.add((JsonElement)classInfoElement);
            }
        });
        return syntaxArray;
    }

    private static JsonArray getPropertyRelatedClassInfos(Property<?> property) {
        JsonArray array = new JsonArray();
        for (ClassInfo<?> classInfo : Classes.getClassInfosByProperty(property)) {
            JsonObject object = new JsonObject();
            object.addProperty("id", DocumentationIdProvider.getId(classInfo));
            object.addProperty("name", Objects.requireNonNullElse(classInfo.getDocName(), classInfo.getCodeName()));
            array.add((JsonElement)object);
        }
        return array;
    }

    private static JsonElement getPropertyRelatedSyntaxes(Property<?> property) {
        JsonArray array = new JsonArray();
        Set<SyntaxInfo<?>> relatedSyntaxes = PROPERTY_RELATED_SYNTAXES.get(property);
        if (relatedSyntaxes == null || relatedSyntaxes.isEmpty()) {
            System.out.println("Property " + property.name() + " has no related syntaxes");
            return null;
        }
        for (SyntaxInfo<?> element : relatedSyntaxes) {
            Name name = element.type().getAnnotation(Name.class);
            if (name == null) continue;
            JsonObject object = new JsonObject();
            object.addProperty("id", DocumentationIdProvider.getId(element));
            object.addProperty("name", element.type().getAnnotation(Name.class).value());
            array.add((JsonElement)object);
        }
        return array;
    }

    private static JsonObject getPropertyDetails(Property<?> property) {
        JsonObject object = new JsonObject();
        object.addProperty("id", DocumentationIdProvider.getId(property));
        object.addProperty("name", property.name());
        object.addProperty("description", property.description());
        object.addProperty("provider", property.provider().name());
        object.add("since", (JsonElement)JSONGenerator.convertToJsonArray(property.since()));
        return object;
    }

    private static JsonElement generatePropertiesArray(Iterator<Property<?>> iterator) {
        JsonArray array = new JsonArray();
        iterator.forEachRemaining(property -> {
            JsonObject object = JSONGenerator.getPropertyDetails(property);
            object.add("relatedTypes", (JsonElement)JSONGenerator.getPropertyRelatedClassInfos(property));
            object.add("relatedSyntaxes", JSONGenerator.getPropertyRelatedSyntaxes(property));
            array.add((JsonElement)object);
        });
        return array;
    }

    private static JsonObject generateFunctionElement(Function<?> function) {
        JsonObject functionJsonObject = new JsonObject();
        functionJsonObject.addProperty("id", DocumentationIdProvider.getId(function));
        functionJsonObject.addProperty("name", function.getName());
        if (function instanceof Documentable) {
            Documentable documentable = (Documentable)((Object)function);
            functionJsonObject.addProperty("since", StringUtils.join(documentable.since(), "\n"));
            functionJsonObject.add("description", (JsonElement)JSONGenerator.convertToJsonArray(documentable.description().toArray(new String[0])));
            functionJsonObject.add("examples", (JsonElement)JSONGenerator.convertToJsonArray(documentable.examples().toArray(new String[0])));
        }
        functionJsonObject.add("returns", (JsonElement)JSONGenerator.getReturnType(function));
        String functionSignature = function.getSignature().toString(false, false);
        functionJsonObject.add("patterns", (JsonElement)JSONGenerator.convertToJsonArray(functionSignature));
        return functionJsonObject;
    }

    private static JsonObject getReturnType(Function<?> function) {
        JsonObject object = new JsonObject();
        ClassInfo<?> returnType = function.getReturnType();
        if (returnType == null) {
            return null;
        }
        object.addProperty("id", DocumentationIdProvider.getId(returnType));
        object.addProperty("name", Objects.requireNonNullElse(returnType.getDocName(), returnType.getCodeName()));
        return object;
    }

    private static JsonArray generateFunctionArray(Iterator<Function<?>> functions) {
        JsonArray syntaxArray = new JsonArray();
        functions.forEachRemaining(function -> syntaxArray.add((JsonElement)JSONGenerator.generateFunctionElement(function)));
        return syntaxArray;
    }

    private static JsonArray generateExperiments() {
        JsonArray array = new JsonArray();
        for (Experiment experiment : Skript.experiments().registered()) {
            JsonObject object = new JsonObject();
            if (!(experiment instanceof Feature)) continue;
            Feature feature = (Feature)experiment;
            object.addProperty("id", experiment.codeName());
            if (feature.displayName().isEmpty()) {
                object.addProperty("name", (String)null);
            } else {
                object.addProperty("name", feature.displayName());
            }
            if (feature.description().isEmpty()) {
                object.add("description", null);
            } else {
                JsonArray description = new JsonArray();
                for (String part : feature.description()) {
                    description.add(part);
                }
                object.add("description", (JsonElement)description);
            }
            object.addProperty("pattern", experiment.pattern().toString());
            object.addProperty("phase", experiment.phase().name().toLowerCase(Locale.ENGLISH));
            array.add((JsonElement)object);
        }
        return array;
    }

    private static JsonArray cleanPatterns(String ... strings) {
        if (strings == null || strings.length == 0 || strings.length == 1 && strings[0].isBlank()) {
            return null;
        }
        for (int i = 0; i < strings.length; ++i) {
            strings[i] = Documentation.cleanPatterns(strings[i], false, false);
        }
        return JSONGenerator.convertToJsonArray(strings);
    }

    private JsonObject getSource() {
        JsonObject object = new JsonObject();
        object.addProperty("name", this.source.name());
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(this.source.name());
        if (plugin == null) {
            try {
                plugin = JavaPlugin.getProvidingPlugin(this.source.source());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        object.addProperty("version", plugin == null ? null : plugin.getDescription().getVersion());
        return object;
    }

    public void generate(@NotNull Path path) throws IOException {
        Preconditions.checkNotNull((Object)path, (Object)"path cannot be null");
        JsonObject jsonDocs = new JsonObject();
        jsonDocs.add("version", (JsonElement)JSONGenerator.getVersion());
        jsonDocs.add("source", (JsonElement)this.getSource());
        jsonDocs.add("conditions", (JsonElement)this.generateSyntaxElementArray(this.source.syntaxRegistry().syntaxes(SyntaxRegistry.CONDITION)));
        jsonDocs.add("effects", (JsonElement)this.generateSyntaxElementArray(this.source.syntaxRegistry().syntaxes(SyntaxRegistry.EFFECT)));
        jsonDocs.add("expressions", (JsonElement)this.generateSyntaxElementArray(this.source.syntaxRegistry().syntaxes(SyntaxRegistry.EXPRESSION)));
        jsonDocs.add("events", (JsonElement)this.generateStructureElementArray(this.source.syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY)));
        jsonDocs.add("structures", (JsonElement)this.generateStructureElementArray(this.source.syntaxRegistry().syntaxes(SyntaxRegistry.STRUCTURE)));
        jsonDocs.add("sections", (JsonElement)this.generateSyntaxElementArray(this.source.syntaxRegistry().syntaxes(SyntaxRegistry.SECTION)));
        jsonDocs.add("types", (JsonElement)JSONGenerator.generateClassInfoArray(Classes.getClassInfos().iterator()));
        jsonDocs.add("functions", (JsonElement)JSONGenerator.generateFunctionArray(Functions.getFunctions().iterator()));
        jsonDocs.add("experiments", (JsonElement)JSONGenerator.generateExperiments());
        jsonDocs.add("properties", JSONGenerator.generatePropertiesArray(PROPERTY_REGISTRY.iterator()));
        try {
            Files.writeString(path, (CharSequence)GSON.toJson((JsonElement)jsonDocs), new OpenOption[0]);
        }
        catch (IOException ex) {
            Skript.exception((Throwable)ex, "An error occurred while trying to generate JSON documentation");
            throw new IOException(ex);
        }
    }

    @Override
    @Deprecated(forRemoval=true, since="2.13")
    public void generate() {
        try {
            this.generate(this.outputDir.toPath());
        }
        catch (IOException ex) {
            Skript.exception((Throwable)ex, "An error occurred while trying to generate JSON documentation");
        }
    }
}

