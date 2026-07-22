/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.google.common.collect.Lists
 *  com.google.common.io.Files
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.block.BlockCanBuildEvent
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.doc;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
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
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.structure.StructureInfo;

public class HTMLGenerator
extends DocumentationGenerator {
    private static final String SKRIPT_VERSION = Skript.getVersion().toString().replaceAll("-(dev|alpha|beta|pre)\\d*", "").replace(".0", "");
    private static final Pattern NEW_TAG_PATTERN = Pattern.compile(SKRIPT_VERSION + "(?!\\.[1-9])");
    private static final Pattern RETURN_TYPE_LINK_PATTERN = Pattern.compile("( ?href=\"(classes\\.html|)#|)\\$\\{element\\.return-type-linkcheck}");
    private final String skeleton;
    private static final Comparator<? super SyntaxElementInfo<?>> annotatedComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            assert (false);
            throw new NullPointerException();
        }
        if (o1.getElementClass().getAnnotation(NoDoc.class) != null) {
            if (o2.getElementClass().getAnnotation(NoDoc.class) != null) {
                return 0;
            }
            return 1;
        }
        if (o2.getElementClass().getAnnotation(NoDoc.class) != null) {
            return -1;
        }
        Name name1 = o1.getElementClass().getAnnotation(Name.class);
        Name name2 = o2.getElementClass().getAnnotation(Name.class);
        if (name1 == null) {
            throw new SkriptAPIException("Name annotation expected: " + String.valueOf(o1.getElementClass()));
        }
        if (name2 == null) {
            throw new SkriptAPIException("Name annotation expected: " + String.valueOf(o2.getElementClass()));
        }
        return name1.value().compareTo(name2.value());
    };
    private static final EventComparator eventComparator = new EventComparator();
    private static final ClassInfoComparator classInfoComparator = new ClassInfoComparator();
    private static final FunctionComparator functionComparator = new FunctionComparator();

    public HTMLGenerator(File templateDir, File outputDir) {
        super(templateDir, outputDir);
        this.skeleton = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/template.html"));
    }

    private static <T> Iterator<SyntaxElementInfo<? extends T>> sortedAnnotatedIterator(Iterator<SyntaxElementInfo<? extends T>> it) {
        ArrayList<SyntaxElementInfo<Object>> list = new ArrayList<SyntaxElementInfo<Object>>();
        while (it.hasNext()) {
            SyntaxElementInfo<T> item = it.next();
            if (item.getElementClass().getAnnotation(Name.class) == null && item.getElementClass().getAnnotation(NoDoc.class) == null) {
                Skript.warning("Skipped generating '" + String.valueOf(item.getElementClass()) + "' class due to missing Name annotation");
                continue;
            }
            list.add(item);
        }
        list.sort(annotatedComparator);
        return list.iterator();
    }

    @Override
    public void generate() {
        for (File f : this.templateDir.listFiles()) {
            String name;
            if (f.getName().matches("css|js|assets")) {
                String slashName = "/" + f.getName();
                File fileTo = new File(String.valueOf(this.outputDir) + slashName);
                fileTo.mkdirs();
                for (File filesInside : new File(String.valueOf(this.templateDir) + slashName).listFiles()) {
                    if (filesInside.isDirectory()) continue;
                    if (!filesInside.getName().toLowerCase(Locale.ENGLISH).endsWith(".png")) {
                        HTMLGenerator.writeFile(new File(String.valueOf(fileTo) + "/" + filesInside.getName()), HTMLGenerator.readFile(filesInside));
                        continue;
                    }
                    if (filesInside.getName().matches("(?i)(.*)\\.(html?|js|css|json)")) continue;
                    try {
                        Files.copy((File)filesInside, (File)new File(String.valueOf(fileTo) + "/" + filesInside.getName()));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                continue;
            }
            if (f.isDirectory() || f.getName().endsWith("template.html") || f.getName().endsWith(".md")) continue;
            Skript.info("Creating documentation for " + f.getName());
            String content = HTMLGenerator.readFile(f);
            String page = f.getName().endsWith(".html") ? this.skeleton.replace("${content}", content) : content;
            page = page.replace("${skript.version}", Skript.getVersion().toString());
            page = page.replace("${skript.build.date}", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            page = page.replace("${pagename}", f.getName().replace(".html", ""));
            ArrayList replace = Lists.newArrayList();
            int include = page.indexOf("${include");
            while (include != -1) {
                int endIncl = page.indexOf("}", include);
                name = page.substring(include + 10, endIncl);
                replace.add(name);
                include = page.indexOf("${include", endIncl);
            }
            for (String name2 : replace) {
                String temp = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/templates/" + name2));
                temp = temp.replace("${skript.version}", Skript.getVersion().toString());
                page = page.replace("${include " + name2 + "}", temp);
            }
            int generate = page.indexOf("${generate");
            while (generate != -1) {
                SyntaxElementInfo info;
                Iterator it;
                int nextBracket = page.indexOf("}", generate);
                String[] genParams = page.substring(generate + 11, nextBracket).split(" ");
                StringBuilder generated = new StringBuilder();
                String descTemp = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/templates/" + genParams[1]));
                String genType = genParams[0];
                boolean isDocsPage = genType.equals("docs");
                if (genType.equals("structures") || isDocsPage) {
                    it = HTMLGenerator.sortedAnnotatedIterator(Skript.getStructures().stream().filter(structure -> structure.getClass() == StructureInfo.class).iterator());
                    while (it.hasNext()) {
                        info = (StructureInfo)it.next();
                        assert (info != null);
                        if (info.getElementClass().getAnnotation(NoDoc.class) != null) continue;
                        String string = this.generateAnnotated(descTemp, info, generated.toString(), "Structure");
                        generated.append(string);
                    }
                }
                if (genType.equals("expressions") || isDocsPage) {
                    it = HTMLGenerator.sortedAnnotatedIterator(Skript.getExpressions());
                    while (it.hasNext()) {
                        info = (ExpressionInfo)it.next();
                        assert (info != null);
                        if (info.getElementClass().getAnnotation(NoDoc.class) != null) continue;
                        String string = this.generateAnnotated(descTemp, info, generated.toString(), "Expression");
                        generated.append(string);
                    }
                }
                if (genType.equals("effects") || isDocsPage) {
                    it = HTMLGenerator.sortedAnnotatedIterator(Skript.getEffects().iterator());
                    while (it.hasNext()) {
                        info = it.next();
                        assert (info != null);
                        if (info.getElementClass().getAnnotation(NoDoc.class) != null) continue;
                        generated.append(this.generateAnnotated(descTemp, info, generated.toString(), "Effect"));
                    }
                    it = HTMLGenerator.sortedAnnotatedIterator(Skript.getSections().iterator());
                    while (it.hasNext()) {
                        info = it.next();
                        assert (info != null);
                        if (!EffectSection.class.isAssignableFrom(info.getElementClass()) || info.getElementClass().getAnnotation(NoDoc.class) != null) continue;
                        generated.append(this.generateAnnotated(descTemp, info, generated.toString(), "EffectSection"));
                    }
                }
                if (genType.equals("conditions") || isDocsPage) {
                    it = HTMLGenerator.sortedAnnotatedIterator(Skript.getConditions().iterator());
                    while (it.hasNext()) {
                        info = it.next();
                        assert (info != null);
                        if (info.getElementClass().getAnnotation(NoDoc.class) != null) continue;
                        generated.append(this.generateAnnotated(descTemp, info, generated.toString(), "Condition"));
                    }
                }
                if (genType.equals("sections") || isDocsPage) {
                    it = HTMLGenerator.sortedAnnotatedIterator(Skript.getSections().iterator());
                    while (it.hasNext()) {
                        info = it.next();
                        assert (info != null);
                        boolean bl = EffectSection.class.isAssignableFrom(info.getElementClass());
                        if (bl && isDocsPage || info.getElementClass().getAnnotation(NoDoc.class) != null) continue;
                        generated.append(this.generateAnnotated(descTemp, info, generated.toString(), (bl ? "Effect" : "") + "Section"));
                    }
                }
                if (genType.equals("events") || isDocsPage) {
                    ArrayList events = new ArrayList(Skript.instance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY));
                    events.sort(eventComparator);
                    for (BukkitSyntaxInfos.Event event : events) {
                        assert (event != null);
                        if (event.type().getAnnotation(NoDoc.class) != null) continue;
                        generated.append(this.generateEvent(descTemp, event, generated.toString()));
                    }
                }
                if (genType.equals("classes") || isDocsPage) {
                    ArrayList classes = new ArrayList(Classes.getClassInfos());
                    classes.sort(classInfoComparator);
                    for (ClassInfo classInfo : classes) {
                        if (!classInfo.hasDocs()) continue;
                        assert (classInfo != null);
                        generated.append(this.generateClass(descTemp, classInfo, generated.toString()));
                    }
                }
                if (genType.equals("functions") || isDocsPage) {
                    ArrayList functions = new ArrayList(Functions.getFunctions());
                    functions.sort(functionComparator);
                    for (Function function : functions) {
                        assert (function != null);
                        generated.append(this.generateFunction(descTemp, function));
                    }
                }
                page = page.replace(page.substring(generate, nextBracket + 1), generated.toString());
                generate = page.indexOf("${generate", nextBracket);
            }
            name = f.getName();
            if (name.endsWith(".html")) {
                page = page.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
                assert (page != null);
                page = HTMLGenerator.minifyHtml(page);
            }
            assert (page != null);
            HTMLGenerator.writeFile(new File(String.valueOf(this.outputDir) + File.separator + name), page);
        }
    }

    private static String minifyHtml(String page) {
        int c;
        StringBuilder sb = new StringBuilder(page.length());
        boolean space = false;
        for (int i = 0; i < page.length(); i += Character.charCount(c)) {
            c = page.codePointAt(i);
            if (c == 10 || c == 32) {
                if (space) continue;
                sb.append(' ');
                space = true;
                continue;
            }
            space = false;
            sb.appendCodePoint(c);
        }
        return sb.toString();
    }

    private static String handleIf(String desc, String condition, boolean value) {
        assert (desc != null);
        int ifStart = ((String)desc).indexOf(condition);
        while (ifStart != -1) {
            int ifEnd = ((String)desc).indexOf("${end}", ifStart);
            String data = ((String)desc).substring(ifStart + condition.length() + 1, ifEnd);
            String before = ((String)desc).substring(0, ifStart);
            String after = ((String)desc).substring(ifEnd + 6);
            desc = value ? before + data + after : before + after;
            ifStart = ((String)desc).indexOf(condition, ifEnd);
        }
        return desc;
    }

    private String generateAnnotated(String descTemp, SyntaxElementInfo<?> info, @Nullable String page, String type) {
        Class<?> c = info.getElementClass();
        Name name = c.getAnnotation(Name.class);
        String desc = descTemp.replace("${element.name}", this.getDefaultIfNullOrEmpty(name != null ? name.value() : null, "Unknown Name"));
        Since since = c.getAnnotation(Since.class);
        desc = desc.replace("${element.since}", Joiner.on((String)"<br/>").join((Object[])this.getDefaultIfNullOrEmpty(since != null ? since.value() : null, "Unknown")));
        Keywords keywords = c.getAnnotation(Keywords.class);
        desc = desc.replace("${element.keywords}", keywords == null ? "" : Joiner.on((String)", ").join((Object[])keywords.value()));
        Description description = c.getAnnotation(Description.class);
        desc = desc.replace("${element.desc}", Joiner.on((String)"<br>").join((Object[])this.getDefaultIfNullOrEmpty(description != null ? description.value() : null, "Unknown description.")).replace("\n\n", "<p>"));
        desc = desc.replace("${element.desc-safe}", Joiner.on((String)"<br>").join((Object[])this.getDefaultIfNullOrEmpty(description != null ? description.value() : null, "Unknown description.")).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        desc = this.extractExamples(desc, c);
        desc = desc.replace("${element.id}", DocumentationIdProvider.getId(info));
        desc = HTMLGenerator.handleIf(desc, "${if cancellable}", false);
        Events events = c.getAnnotation(Events.class);
        desc = HTMLGenerator.handleIf(desc, "${if events}", events != null);
        if (events != null) {
            String[] eventNames = events != null ? events.value() : null;
            Object[] eventLinks = new String[eventNames.length];
            for (int i = 0; i < eventNames.length; ++i) {
                String eventName = eventNames[i];
                eventLinks[i] = "<a href=\"events.html#" + eventName.replaceAll("( ?/ ?| +)", "_") + "\">" + eventName + "</a>";
            }
            desc = desc.replace("${element.events}", Joiner.on((String)", ").join(eventLinks));
        }
        desc = desc.replace("${element.events-safe}", events == null ? "" : Joiner.on((String)", ").join(events != null ? events.value() : null));
        RequiredPlugins plugins = c.getAnnotation(RequiredPlugins.class);
        desc = HTMLGenerator.handleIf(desc, "${if required-plugins}", plugins != null);
        desc = desc.replace("${element.required-plugins}", plugins == null ? "" : Joiner.on((String)", ").join(plugins != null ? plugins.value() : null));
        ClassInfo returnType = info instanceof ExpressionInfo ? Classes.getSuperClassInfo(((ExpressionInfo)info).getReturnType()) : null;
        desc = this.replaceReturnType(desc, returnType);
        desc = HTMLGenerator.handleIf(desc, "${if by-addon}", false);
        if (since != null) {
            String[] value = since.value();
            String s = value[value.length - 1];
            desc = HTMLGenerator.handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher(s).find());
        } else {
            desc = HTMLGenerator.handleIf(desc, "${if new-element}", false);
        }
        if (info instanceof StructureInfo) {
            EntryValidator entryValidator = ((StructureInfo)info).entryValidator;
            ArrayList entryDataList = new ArrayList();
            if (entryValidator != null) {
                entryDataList.addAll(entryValidator.getEntryData());
            }
            desc = HTMLGenerator.handleIf(desc, "${if structure-optional-entrydata}", entryValidator != null);
            desc = desc.replace("${element.structure-optional-entrydata}", entryValidator == null ? "" : Joiner.on((String)", ").join((Iterable)entryDataList.stream().filter(EntryData::isOptional).map(EntryData::getKey).collect(Collectors.toList())));
            desc = HTMLGenerator.handleIf(desc, "${if structure-required-entrydata}", entryValidator != null);
            desc = desc.replace("${element.structure-required-entrydata}", entryValidator == null ? "" : Joiner.on((String)", ").join((Iterable)entryDataList.stream().filter(entryData -> !entryData.isOptional()).map(EntryData::getKey).collect(Collectors.toList())));
        } else {
            desc = HTMLGenerator.handleIf(desc, "${if structure-optional-entrydata}", false);
            desc = HTMLGenerator.handleIf(desc, "${if structure-required-entrydata}", false);
        }
        desc = desc.replace("${element.type}", type);
        ArrayList toGen = Lists.newArrayList();
        int generate = desc.indexOf("${generate");
        while (generate != -1) {
            int nextBracket = desc.indexOf("}", generate);
            String data = desc.substring(generate + 11, nextBracket);
            toGen.add(data);
            generate = desc.indexOf("${generate", nextBracket);
        }
        for (String data : toGen) {
            String[] split = data.split(" ");
            String pattern = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/templates/" + split[1]));
            StringBuilder patterns = new StringBuilder();
            for (String line : this.getDefaultIfNullOrEmpty(info.getPatterns(), "Missing patterns.")) {
                assert (line != null);
                line = HTMLGenerator.cleanPatterns(line);
                String parsed = pattern.replace("${element.pattern}", line);
                patterns.append(parsed);
            }
            String toReplace = "${generate element.patterns " + split[1] + "}";
            desc = desc.replace(toReplace, patterns.toString());
            desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", patterns.toString().replace("\\", "\\\\"));
        }
        assert (desc != null);
        return desc;
    }

    @NotNull
    private String extractExamples(String desc, Class<?> syntax) {
        if (syntax.isAnnotationPresent(Example.class)) {
            Example examples = syntax.getAnnotation(Example.class);
            return this.addExamples(desc, examples.value());
        }
        if (syntax.isAnnotationPresent(Example.Examples.class)) {
            Example.Examples examples = syntax.getAnnotation(Example.Examples.class);
            Example[] examplesArray = examples.value();
            String[] mappedExamples = new String[examplesArray.length * 2 - 1];
            for (int i = 0; i < mappedExamples.length; ++i) {
                if (i % 2 == 0) {
                    String example = examplesArray[i / 2].value();
                    if (example.endsWith("\n")) {
                        example = example.substring(0, example.length() - "\n".length());
                    }
                    mappedExamples[i] = example;
                    continue;
                }
                mappedExamples[i] = "";
            }
            return this.addExamples(desc, mappedExamples);
        }
        if (syntax.isAnnotationPresent(Examples.class)) {
            Examples examples = syntax.getAnnotation(Examples.class);
            return this.addExamples(desc, examples.value());
        }
        return this.addExamples(desc, null);
    }

    @NotNull
    private String addExamples(String desc, String ... examples) {
        if (examples != null) {
            Documentation.escapeHTML(examples);
            for (int i = 0; i < examples.length; ++i) {
                examples[i] = examples[i].replace("\n", "<br>");
            }
        }
        String mergedExamples = Joiner.on((String)"<br>").join((Object[])this.getDefaultIfNullOrEmpty(examples, "Missing examples."));
        desc = desc.replace("${element.examples}", mergedExamples).replace("${element.examples-safe}", mergedExamples);
        return desc;
    }

    private String generateEvent(String descTemp, BukkitSyntaxInfos.Event<?> modernInfo, @Nullable String page) {
        SkriptEventInfo info = (SkriptEventInfo)SyntaxElementInfo.fromModern(modernInfo);
        Class c = info.getElementClass();
        String docName = this.getDefaultIfNullOrEmpty(info.getName(), "Unknown Name");
        String desc = descTemp.replace("${element.name}", docName);
        Object[] since = this.getDefaultIfNullOrEmpty(info.getSince(), "Unknown");
        desc = desc.replace("${element.since}", Joiner.on((String)"<br/>").join(since));
        Object[] description = this.getDefaultIfNullOrEmpty(info.getDescription(), "Missing description.");
        desc = desc.replace("${element.desc}", Joiner.on((String)"<br>").join(description).replace("\n\n", "<p>"));
        desc = desc.replace("${element.desc-safe}", Joiner.on((String)"<br>").join(description).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        desc = HTMLGenerator.handleIf(desc, "${if by-addon}", false);
        Object[] examples = this.getDefaultIfNullOrEmpty(info.getExamples(), "Missing examples.");
        desc = desc.replace("${element.examples}", Joiner.on((String)"\n<br>").join((Object[])Documentation.escapeHTML((String[])examples)));
        desc = desc.replace("${element.examples-safe}", Joiner.on((String)"<br>").join(examples).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        Object[] keywords = info.getKeywords();
        desc = desc.replace("${element.keywords}", keywords == null ? "" : Joiner.on((String)", ").join(keywords));
        boolean cancellable = false;
        for (Class<? extends Event> event : info.events) {
            if (!Cancellable.class.isAssignableFrom(event) && !BlockCanBuildEvent.class.isAssignableFrom(event)) continue;
            cancellable = true;
            break;
        }
        desc = HTMLGenerator.handleIf(desc, "${if cancellable}", cancellable);
        desc = desc.replace("${element.cancellable}", cancellable ? "Yes" : "");
        desc = desc.replace("${element.id}", DocumentationIdProvider.getId(modernInfo));
        Events events = c.getAnnotation(Events.class);
        desc = HTMLGenerator.handleIf(desc, "${if events}", events != null);
        if (events != null) {
            String[] eventNames = events != null ? events.value() : null;
            Object[] eventLinks = new String[eventNames.length];
            for (int i = 0; i < eventNames.length; ++i) {
                String eventName = eventNames[i];
                eventLinks[i] = "<a href=\"events.html#" + eventName.replaceAll(" ?/ ?", "_").replaceAll(" +", "_") + "\">" + eventName + "</a>";
            }
            desc = desc.replace("${element.events}", Joiner.on((String)", ").join(eventLinks));
        }
        desc = desc.replace("${element.events-safe}", events == null ? "" : Joiner.on((String)", ").join(events != null ? events.value() : null));
        Object[] plugins = info.getRequiredPlugins();
        desc = HTMLGenerator.handleIf(desc, "${if required-plugins}", plugins != null && plugins.length > 0);
        desc = plugins == null ? desc.replace("${element.required-plugins}", "") : desc.replace("${element.required-plugins}", Joiner.on((String)", ").join(plugins));
        Object lastValue = since[since.length - 1];
        desc = HTMLGenerator.handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher((CharSequence)lastValue).find());
        desc = desc.replace("${element.type}", "Event");
        desc = HTMLGenerator.handleIf(desc, "${if return-type}", false);
        desc = HTMLGenerator.handleIf(desc, "${if structure-optional-entrydata}", false);
        desc = HTMLGenerator.handleIf(desc, "${if structure-required-entrydata}", false);
        ArrayList toGen = Lists.newArrayList();
        int generate = desc.indexOf("${generate");
        while (generate != -1) {
            int nextBracket = desc.indexOf("}", generate);
            String data = desc.substring(generate + 11, nextBracket);
            toGen.add(data);
            generate = desc.indexOf("${generate", nextBracket);
        }
        for (String data : toGen) {
            String[] split = data.split(" ");
            String pattern = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/templates/" + split[1]));
            StringBuilder patterns = new StringBuilder();
            for (String string : this.getDefaultIfNullOrEmpty(info.getPatterns(), "Missing patterns.")) {
                assert (string != null);
                String string2 = "[on] " + HTMLGenerator.cleanPatterns(string);
                String parsed = pattern.replace("${element.pattern}", string2);
                patterns.append(parsed);
            }
            desc = desc.replace("${generate element.patterns " + split[1] + "}", patterns.toString());
            desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", patterns.toString().replace("\\", "\\\\"));
        }
        assert (desc != null);
        return desc;
    }

    private String generateClass(String descTemp, ClassInfo<?> info, @Nullable String page) {
        Class<?> c = info.getC();
        String docName = this.getDefaultIfNullOrEmpty(info.getDocName(), "Unknown Name");
        String desc = descTemp.replace("${element.name}", docName);
        String since = this.getDefaultIfNullOrEmpty(info.getSince(), "Unknown");
        desc = desc.replace("${element.since}", since);
        Object[] description = this.getDefaultIfNullOrEmpty(info.getDescription(), "Missing description.");
        desc = desc.replace("${element.desc}", Joiner.on((String)"<br>").join(description).replace("\n\n", "<p>"));
        desc = desc.replace("${element.desc-safe}", Joiner.on((String)"<br>").join(description).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        desc = HTMLGenerator.handleIf(desc, "${if by-addon}", false);
        String[] examples = this.getDefaultIfNullOrEmpty(info.getExamples(), "Missing examples.");
        desc = desc.replace("${element.examples}", Joiner.on((String)"\n<br>").join((Object[])Documentation.escapeHTML(examples)));
        desc = desc.replace("${element.examples-safe}", Joiner.on((String)"<br>").join((Object[])Documentation.escapeHTML(examples)).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        Keywords keywords = c.getAnnotation(Keywords.class);
        desc = desc.replace("${element.keywords}", keywords == null ? "" : Joiner.on((String)", ").join((Object[])keywords.value()));
        desc = desc.replace("${element.id}", DocumentationIdProvider.getId(info));
        desc = HTMLGenerator.handleIf(desc, "${if cancellable}", false);
        Events events = c.getAnnotation(Events.class);
        desc = HTMLGenerator.handleIf(desc, "${if events}", events != null);
        if (events != null) {
            String[] eventNames = events != null ? events.value() : null;
            Object[] eventLinks = new String[eventNames.length];
            for (int i = 0; i < eventNames.length; ++i) {
                String eventName = eventNames[i];
                eventLinks[i] = "<a href=\"events.html#" + eventName.replaceAll(" ?/ ?", "_").replaceAll(" +", "_") + "\">" + eventName + "</a>";
            }
            desc = desc.replace("${element.events}", Joiner.on((String)", ").join(eventLinks));
        }
        desc = desc.replace("${element.events-safe}", events == null ? "" : Joiner.on((String)", ").join(events != null ? events.value() : null));
        Object[] requiredPlugins = info.getRequiredPlugins();
        desc = HTMLGenerator.handleIf(desc, "${if required-plugins}", requiredPlugins != null);
        desc = desc.replace("${element.required-plugins}", Joiner.on((String)", ").join(requiredPlugins == null ? new String[]{} : requiredPlugins));
        desc = HTMLGenerator.handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher(since).find());
        desc = desc.replace("${element.type}", "Type");
        desc = HTMLGenerator.handleIf(desc, "${if return-type}", false);
        desc = HTMLGenerator.handleIf(desc, "${if structure-optional-entrydata}", false);
        desc = HTMLGenerator.handleIf(desc, "${if structure-required-entrydata}", false);
        ArrayList toGen = Lists.newArrayList();
        int generate = desc.indexOf("${generate");
        while (generate != -1) {
            int nextBracket = desc.indexOf("}", generate);
            String data = desc.substring(generate + 11, nextBracket);
            toGen.add(data);
            generate = desc.indexOf("${generate", nextBracket);
        }
        for (String data : toGen) {
            String[] split = data.split(" ");
            String pattern = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/templates/" + split[1]));
            StringBuilder patterns = new StringBuilder();
            String[] lines = this.getDefaultIfNullOrEmpty(info.getUsage(), "Missing patterns.");
            if (lines == null) continue;
            for (String line : lines) {
                assert (line != null);
                String parsed = pattern.replace("${element.pattern}", line);
                patterns.append(parsed);
            }
            desc = desc.replace("${generate element.patterns " + split[1] + "}", patterns.toString());
            desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", patterns.toString().replace("\\", "\\\\"));
        }
        assert (desc != null);
        return desc;
    }

    private String generateFunction(String descTemp, Function<?> info) {
        if (!(info instanceof Documentable)) {
            assert (false);
            return "";
        }
        Documentable documentable = (Documentable)((Object)info);
        String[] typeSince = documentable.since().toArray(new String[0]);
        String[] typeDescription = documentable.description().toArray(new String[0]);
        String[] typeExamples = documentable.examples().toArray(new String[0]);
        Object[] typeKeywords = documentable.keywords().toArray(new String[0]);
        String docName = this.getDefaultIfNullOrEmpty(info.getName(), "Unknown Name");
        String desc = descTemp.replace("${element.name}", docName);
        Object[] since = this.getDefaultIfNullOrEmpty(typeSince, "Unknown");
        desc = desc.replace("${element.since}", Joiner.on((String)"<br>").join(since));
        Object[] description = this.getDefaultIfNullOrEmpty(typeDescription, "Missing description.");
        desc = desc.replace("${element.desc}", Joiner.on((String)"<br>").join(description));
        desc = desc.replace("${element.desc-safe}", Joiner.on((String)"<br>").join(description).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        desc = HTMLGenerator.handleIf(desc, "${if by-addon}", false);
        Object[] examples = this.getDefaultIfNullOrEmpty(typeExamples, "Missing examples.");
        desc = desc.replace("${element.examples}", Joiner.on((String)"\n<br>").join((Object[])Documentation.escapeHTML((String[])examples)));
        desc = desc.replace("${element.examples-safe}", Joiner.on((String)"<br>").join(examples).replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "    "));
        Object[] keywords = typeKeywords;
        desc = desc.replace("${element.keywords}", keywords == null ? "" : Joiner.on((String)", ").join(keywords));
        desc = desc.replace("${element.id}", DocumentationIdProvider.getId(info));
        desc = HTMLGenerator.handleIf(desc, "${if cancellable}", false);
        desc = HTMLGenerator.handleIf(desc, "${if events}", false);
        desc = HTMLGenerator.handleIf(desc, "${if required-plugins}", false);
        ClassInfo<?> returnType = info.getReturnType();
        desc = this.replaceReturnType(desc, returnType);
        desc = HTMLGenerator.handleIf(desc, "${if new-element}", NEW_TAG_PATTERN.matcher(Joiner.on((String)" ").join(since)).find());
        desc = desc.replace("${element.type}", "Function");
        desc = HTMLGenerator.handleIf(desc, "${if structure-optional-entrydata}", false);
        desc = HTMLGenerator.handleIf(desc, "${if structure-required-entrydata}", false);
        ArrayList toGen = Lists.newArrayList();
        int generate = desc.indexOf("${generate");
        while (generate != -1) {
            int nextBracket = desc.indexOf("}", generate);
            String data = desc.substring(generate + 11, nextBracket);
            toGen.add(data);
            generate = desc.indexOf("${generate", nextBracket);
        }
        for (String data : toGen) {
            String[] split = data.split(" ");
            String pattern = HTMLGenerator.readFile(new File(String.valueOf(this.templateDir) + "/templates/" + split[1]));
            Object patterns = "";
            String line = info.getSignature().toString(false, false);
            patterns = (String)patterns + pattern.replace("${element.pattern}", line);
            desc = desc.replace("${generate element.patterns " + split[1] + "}", (CharSequence)patterns);
            desc = desc.replace("${generate element.patterns-safe " + split[1] + "}", ((String)patterns).replace("\\", "\\\\"));
        }
        assert (desc != null);
        return desc;
    }

    private static String readFile(File f) {
        try {
            return Files.toString((File)f, (Charset)StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static void writeFile(File f, String data) {
        try {
            Files.write((CharSequence)data, (File)f, (Charset)StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String cleanPatterns(String patterns) {
        return Documentation.cleanPatterns(patterns);
    }

    private static String cleanPatterns(String patterns, boolean escapeHTML) {
        if (escapeHTML) {
            return Documentation.cleanPatterns(patterns);
        }
        return Documentation.cleanPatterns(patterns, false);
    }

    public String getDefaultIfNullOrEmpty(@Nullable String string, String message) {
        return string == null || string.isEmpty() ? message : string;
    }

    public String[] getDefaultIfNullOrEmpty(@Nullable String[] string, String message) {
        String[] stringArray;
        if (string == null || string.length == 0 || string[0].equals("")) {
            String[] stringArray2 = new String[1];
            stringArray = stringArray2;
            stringArray2[0] = message;
        } else {
            stringArray = string;
        }
        return stringArray;
    }

    private String replaceReturnType(String desc, @Nullable ClassInfo<?> returnType) {
        if (returnType == null) {
            return HTMLGenerator.handleIf(desc, "${if return-type}", false);
        }
        boolean noDoc = !returnType.hasDocs();
        String returnTypeName = noDoc ? returnType.getCodeName() : returnType.getDocName();
        String returnTypeLink = noDoc ? "" : "$1" + this.getDefaultIfNullOrEmpty(returnType.getDocumentationID(), returnType.getCodeName());
        desc = HTMLGenerator.handleIf(desc, "${if return-type}", true);
        desc = RETURN_TYPE_LINK_PATTERN.matcher(desc).replaceAll(returnTypeLink);
        desc = desc.replace("${element.return-type}", returnTypeName);
        return desc;
    }

    private static class EventComparator
    implements Comparator<BukkitSyntaxInfos.Event<?>> {
        @Override
        public int compare(@Nullable BukkitSyntaxInfos.Event<?> o1, @Nullable BukkitSyntaxInfos.Event<?> o2) {
            if (o1 == null || o2 == null) {
                assert (false);
                throw new NullPointerException();
            }
            if (o1.type().getAnnotation(NoDoc.class) != null) {
                return 1;
            }
            if (o2.type().getAnnotation(NoDoc.class) != null) {
                return -1;
            }
            return o1.name().compareTo(o2.name());
        }
    }

    private static class ClassInfoComparator
    implements Comparator<ClassInfo<?>> {
        @Override
        public int compare(@Nullable ClassInfo<?> o1, @Nullable ClassInfo<?> o2) {
            String name2;
            if (o1 == null || o2 == null) {
                assert (false);
                throw new NullPointerException();
            }
            String name1 = o1.getDocName();
            if (name1 == null) {
                name1 = o1.getCodeName();
            }
            if ((name2 = o2.getDocName()) == null) {
                name2 = o2.getCodeName();
            }
            return name1.compareTo(name2);
        }
    }

    private static class FunctionComparator
    implements Comparator<Function<?>> {
        @Override
        public int compare(@Nullable Function<?> o1, @Nullable Function<?> o2) {
            if (o1 == null || o2 == null) {
                assert (false);
                throw new NullPointerException();
            }
            return o1.getName().compareTo(o2.getName());
        }
    }
}

