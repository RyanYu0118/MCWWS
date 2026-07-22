/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  edu.umd.cs.findbugs.annotations.SuppressFBWarnings
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.doc;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.conditions.CondCompare;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Documentable;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.NoDoc;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.IteratorIterable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.Parameter;

@SuppressFBWarnings(value={"ES_COMPARING_STRINGS_WITH_EQ"})
public class Documentation {
    private static final Pattern CP_PARSE_MARKS_PATTERN = Pattern.compile("(?<=[(|])[-0-9]+?\u00a6");
    private static final Pattern CP_EMPTY_PARSE_MARKS_PATTERN = Pattern.compile("\\(\\)");
    private static final Pattern CP_PARSE_TAGS_PATTERN = Pattern.compile("(?<=[(|\\[ ])[-a-zA-Z0-9!$#%^&*_+~=\"'<>?,.]*?:");
    private static final Pattern CP_EXTRA_OPTIONAL_PATTERN = Pattern.compile("\\[\\(((\\w+? ?)+)\\)]");
    private static final File DOCS_TEMPLATE_DIRECTORY = new File(Skript.getInstance().getDataFolder(), "docs/templates");
    private static final File DOCS_OUTPUT_DIRECTORY = new File(Skript.getInstance().getDataFolder(), "docs");
    public static final boolean FORCE_HOOKS_SYSTEM_PROPERTY = "true".equals(System.getProperty("skript.forceregisterhooks"));
    public static final boolean generate = Skript.testing() && new File(Skript.getInstance().getDataFolder(), "generate-doc").exists();
    private static ArrayList<Pattern> validation = new ArrayList();
    private static final String[] urls;

    public static boolean isDocsTemplateFound() {
        return Documentation.getDocsTemplateDirectory().isDirectory();
    }

    public static boolean canGenerateUnsafeDocs() {
        return Documentation.isDocsTemplateFound() && FORCE_HOOKS_SYSTEM_PROPERTY;
    }

    public static File getDocsTemplateDirectory() {
        String environmentTemplateDir = System.getenv("SKRIPT_DOCS_TEMPLATE_DIR");
        return environmentTemplateDir == null ? DOCS_TEMPLATE_DIRECTORY : new File(environmentTemplateDir);
    }

    public static File getDocsOutputDirectory() {
        String environmentOutputDir = System.getenv("SKRIPT_DOCS_OUTPUT_DIR");
        return environmentOutputDir == null ? DOCS_OUTPUT_DIRECTORY : new File(environmentOutputDir);
    }

    public static void generate() {
        if (!generate) {
            return;
        }
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(new File(Skript.getInstance().getDataFolder(), "doc.sql")), "UTF-8"));
            Documentation.asSql(pw);
            pw.flush();
            pw.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
    }

    private static void asSql(PrintWriter pw) {
        pw.println("-- syntax elements");
        pw.println("CREATE TABLE IF NOT EXISTS syntax_elements (id VARCHAR(20) NOT NULL PRIMARY KEY,name VARCHAR(100) NOT NULL,type ENUM('condition','effect','expression','event') NOT NULL,patterns VARCHAR(2000) NOT NULL,description VARCHAR(2000) NOT NULL,examples VARCHAR(2000) NOT NULL,since VARCHAR(100) NOT NULL);");
        pw.println("UPDATE syntax_elements SET patterns='';");
        pw.println();
        pw.println("-- expressions");
        for (ExpressionInfo<?, ?> expressionInfo : new IteratorIterable(Skript.getExpressions())) {
            assert (expressionInfo != null);
            Documentation.insertSyntaxElement(pw, expressionInfo, "expression");
        }
        pw.println();
        pw.println("-- effects");
        for (SyntaxElementInfo syntaxElementInfo : Skript.getEffects()) {
            assert (syntaxElementInfo != null);
            Documentation.insertSyntaxElement(pw, syntaxElementInfo, "effect");
        }
        pw.println();
        pw.println("-- conditions");
        for (SyntaxElementInfo syntaxElementInfo : Skript.getConditions()) {
            assert (syntaxElementInfo != null);
            Documentation.insertSyntaxElement(pw, syntaxElementInfo, "condition");
        }
        pw.println();
        pw.println("-- events");
        for (SkriptEventInfo skriptEventInfo : Skript.getEvents()) {
            assert (skriptEventInfo != null);
            Documentation.insertEvent(pw, skriptEventInfo);
        }
        pw.println();
        pw.println();
        pw.println("-- classes");
        pw.println("CREATE TABLE IF NOT EXISTS classes (id VARCHAR(20) NOT NULL PRIMARY KEY,name VARCHAR(100) NOT NULL,description VARCHAR(2000) NOT NULL,patterns VARCHAR(2000) NOT NULL,`usage` VARCHAR(2000) NOT NULL,examples VARCHAR(2000) NOT NULL,since VARCHAR(100) NOT NULL);");
        pw.println("UPDATE classes SET patterns='';");
        pw.println();
        for (ClassInfo classInfo : Classes.getClassInfos()) {
            assert (classInfo != null);
            Documentation.insertClass(pw, classInfo);
        }
        pw.println();
        pw.println();
        pw.println("-- functions");
        pw.println("CREATE TABLE IF NOT EXISTS functions (name VARCHAR(100) NOT NULL,parameters VARCHAR(2000) NOT NULL,description VARCHAR(2000) NOT NULL,examples VARCHAR(2000) NOT NULL,since VARCHAR(100) NOT NULL);");
        for (Function function : Functions.getFunctions()) {
            assert (function != null);
            Documentation.insertFunction(pw, function);
        }
    }

    private static String convertRegex(String regex, boolean escapeHTML) {
        if (StringUtils.containsAny(regex, ".[]\\*+")) {
            Skript.error("Regex '" + regex + "' contains unconverted Regex syntax");
        }
        regex = escapeHTML ? Documentation.escapeHTML(regex) : regex;
        return regex.replaceAll("\\((.+?)\\)\\?", "[$1]").replaceAll("(.)\\?", "[$1]");
    }

    private static String convertRegex(String regex) {
        return Documentation.convertRegex(regex, true);
    }

    protected static String cleanPatterns(String patterns) {
        return Documentation.cleanPatterns(patterns, true);
    }

    protected static String cleanPatterns(String patterns, boolean escapeHTML) {
        return Documentation.cleanPatterns(patterns, escapeHTML, true);
    }

    protected static String cleanPatterns(String patterns, boolean escapeHTML, boolean useLinks) {
        String cleanedPatterns = escapeHTML ? Documentation.escapeHTML(patterns) : patterns;
        cleanedPatterns = CP_PARSE_MARKS_PATTERN.matcher(cleanedPatterns).replaceAll("");
        cleanedPatterns = CP_EMPTY_PARSE_MARKS_PATTERN.matcher(cleanedPatterns).replaceAll("");
        cleanedPatterns = CP_PARSE_TAGS_PATTERN.matcher(cleanedPatterns).replaceAll("");
        cleanedPatterns = CP_EXTRA_OPTIONAL_PATTERN.matcher(cleanedPatterns).replaceAll("[$1]");
        java.util.function.Function<Matcher, String> callback = m -> {
            int i;
            String group = m.group();
            boolean startToEnd = group.contains("(|");
            int depth = 0;
            int charIndex = 0;
            char[] chars = group.toCharArray();
            int n = i = startToEnd ? 0 : chars.length - 1;
            while (startToEnd ? i < chars.length : i >= 0) {
                char c = chars[i];
                if (c == ')') {
                    if (startToEnd && ++depth == 0) {
                        charIndex = i;
                        break;
                    }
                } else if (c == '(') {
                    if (!startToEnd && --depth == 0) {
                        charIndex = i;
                        break;
                    }
                } else if (c == '\\') {
                    --i;
                }
                i = startToEnd ? i + 1 : i - 1;
            }
            if (depth == 0 && charIndex != 0) {
                if (startToEnd) {
                    return "[" + group.substring(0, charIndex).replace("(|", "") + "]" + group.substring(charIndex + 1, chars.length);
                }
                return group.substring(0, charIndex) + "[" + group.substring(charIndex + 1, chars.length).replace("|)", "") + "]";
            }
            return group;
        };
        cleanedPatterns = cleanedPatterns.replaceAll("\\(([^()]+?)\\|\\)", "[($1)]");
        cleanedPatterns = cleanedPatterns.replaceAll("\\(\\|([^()]+?)\\)", "[($1)]");
        cleanedPatterns = StringUtils.replaceAll((CharSequence)cleanedPatterns, "\\((.+)\\|\\)", callback);
        assert (cleanedPatterns != null);
        cleanedPatterns = StringUtils.replaceAll((CharSequence)cleanedPatterns, "\\((.+?)\\|\\)", callback);
        assert (cleanedPatterns != null);
        cleanedPatterns = StringUtils.replaceAll((CharSequence)cleanedPatterns, "\\(\\|(.+)\\)", callback);
        assert (cleanedPatterns != null);
        cleanedPatterns = StringUtils.replaceAll((CharSequence)cleanedPatterns, "\\(\\|(.+?)\\)", callback);
        assert (cleanedPatterns != null);
        String s = StringUtils.replaceAll((CharSequence)cleanedPatterns, "(?<!\\\\)%(.+?)(?<!\\\\)%", m -> {
            int a;
            String s1 = m.group(1);
            if (s1.startsWith("-")) {
                s1 = s1.substring(1);
            }
            String flag = "";
            if (s1.startsWith("*") || s1.startsWith("~")) {
                flag = s1.substring(0, 1);
                s1 = s1.substring(1);
            }
            if ((a = s1.indexOf("@")) != -1) {
                s1 = s1.substring(0, a);
            }
            StringBuilder b = new StringBuilder("%");
            b.append(flag);
            boolean first = true;
            for (String c : s1.split("/")) {
                assert (c != null);
                if (!first) {
                    b.append("/");
                }
                first = false;
                NonNullPair<String, Boolean> p = Utils.getEnglishPlural(c);
                ClassInfo<?> ci = Classes.getClassInfoNoError(p.getFirst());
                if (ci != null && ci.hasDocs()) {
                    if (useLinks) {
                        b.append("<a href='./classes.html#").append(p.getFirst()).append("'>").append(ci.getName().toString(p.getSecond())).append("</a>");
                        continue;
                    }
                    b.append(ci.getName().toString(p.getSecond()));
                    continue;
                }
                b.append(c);
                if (ci == null || !ci.hasDocs()) continue;
                Skript.warning("Used class " + p.getFirst() + " has no docName/name defined");
            }
            return b.append("%").toString();
        });
        assert (s != null) : patterns;
        return s;
    }

    private static void insertSyntaxElement(PrintWriter pw, SyntaxElementInfo<?> info, String type) {
        Class<?> elementClass = info.getElementClass();
        if (elementClass.getAnnotation(NoDoc.class) != null) {
            return;
        }
        if (elementClass.getAnnotation(Name.class) == null || elementClass.getAnnotation(Description.class) == null || !elementClass.isAnnotationPresent(Examples.class) && !elementClass.isAnnotationPresent(Example.class) && !elementClass.isAnnotationPresent(Example.Examples.class) || elementClass.getAnnotation(Since.class) == null) {
            Skript.warning(elementClass.getSimpleName() + " is missing information");
            return;
        }
        String desc = Documentation.validateHTML(StringUtils.join(elementClass.getAnnotation(Description.class).value(), "<br/>"), type + "s");
        String since = Documentation.validateHTML(StringUtils.join(elementClass.getAnnotation(Since.class).value(), "<br/>"), type + "s");
        if (desc == null || since == null) {
            Skript.warning(elementClass.getSimpleName() + "'s description or 'since' is invalid");
            return;
        }
        String patterns = Documentation.cleanPatterns(StringUtils.join(info.patterns, "\n", 0, elementClass == CondCompare.class ? 8 : info.getPatterns().length));
        Documentation.insertOnDuplicateKeyUpdate(pw, "syntax_elements", "id, name, type, patterns, description, examples, since", "patterns = TRIM(LEADING '\n' FROM CONCAT(patterns, '\n', '" + Documentation.escapeSQL(patterns) + "'))", Documentation.escapeHTML(elementClass.getSimpleName()), Documentation.escapeHTML(elementClass.getAnnotation(Name.class).value()), type, patterns, desc, Documentation.escapeHTML(StringUtils.join(elementClass.getAnnotation(Examples.class).value(), "\n")), since);
    }

    private static void insertEvent(PrintWriter pw, SkriptEventInfo<?> info) {
        if (info.getDescription() == SkriptEventInfo.NO_DOC) {
            return;
        }
        if (info.getDescription() == null || info.getExamples() == null || info.getSince() == null) {
            Skript.warning(info.getName() + " (" + info.getElementClass().getSimpleName() + ") is missing information");
            return;
        }
        for (SkriptEventInfo<?> i : Skript.getEvents()) {
            if (!info.getId().equals(i.getId()) || info == i || i.getDescription() == null || i.getDescription() == SkriptEventInfo.NO_DOC) continue;
            Skript.warning("Duplicate event id '" + info.getId() + "'");
            return;
        }
        String desc = Documentation.validateHTML(StringUtils.join(info.getDescription(), "<br/>"), "events");
        String since = Documentation.validateHTML(StringUtils.join(info.getSince(), "<br/>"), "events");
        if (desc == null || since == null) {
            Skript.warning("description or 'since' of " + info.getName() + " (" + info.getElementClass().getSimpleName() + ") is invalid");
            return;
        }
        String patterns = Documentation.cleanPatterns((String)(info.getName().startsWith("On ") ? "[on] " + StringUtils.join(info.getPatterns(), "\n[on] ") : StringUtils.join(info.patterns, "\n")));
        Documentation.insertOnDuplicateKeyUpdate(pw, "syntax_elements", "id, name, type, patterns, description, examples, since", "patterns = '" + Documentation.escapeSQL(patterns) + "'", Documentation.escapeHTML(info.getId()), Documentation.escapeHTML(info.getName()), "event", patterns, desc, Documentation.escapeHTML(StringUtils.join(info.getExamples(), "\n")), since);
    }

    private static void insertClass(PrintWriter pw, ClassInfo<?> info) {
        String since;
        if (info.getDocName() == ClassInfo.NO_DOC) {
            return;
        }
        if (info.getDocName() == null || info.getDescription() == null || info.getUsage() == null || info.getExamples() == null || info.getSince() == null) {
            Skript.warning("Class " + info.getCodeName() + " is missing information");
            return;
        }
        String desc = Documentation.validateHTML(StringUtils.join(info.getDescription(), "<br/>"), "classes");
        String usage = Documentation.validateHTML(StringUtils.join(info.getUsage(), "<br/>"), "classes");
        String string = since = info.getSince() == null ? "" : Documentation.validateHTML(info.getSince(), "classes");
        if (desc == null || usage == null || since == null) {
            Skript.warning("Class " + info.getCodeName() + "'s description, usage or 'since' is invalid");
            return;
        }
        String patterns = info.getUserInputPatterns() == null ? "" : Documentation.convertRegex(StringUtils.join(info.getUserInputPatterns(), "\n"));
        Documentation.insertOnDuplicateKeyUpdate(pw, "classes", "id, name, description, patterns, `usage`, examples, since", "patterns = TRIM(LEADING '\n' FROM CONCAT(patterns, '\n', '" + Documentation.escapeSQL(patterns) + "'))", Documentation.escapeHTML(info.getCodeName()), Documentation.escapeHTML(info.getDocName()), desc, patterns, usage, Documentation.escapeHTML(StringUtils.join(info.getExamples(), "\n")), since);
    }

    private static void insertFunction(PrintWriter pw, Function<?> func) {
        if (!(func instanceof Documentable)) {
            assert (false);
            return;
        }
        Documentable documentable = (Documentable)((Object)func);
        Object[] typeSince = documentable.since().toArray(new String[0]);
        Object[] typeDescription = documentable.description().toArray(new String[0]);
        Object[] typeExamples = documentable.examples().toArray(new String[0]);
        StringBuilder params = new StringBuilder();
        for (Parameter<?> p : func.getSignature().parameters().all()) {
            if (!params.isEmpty()) {
                params.append(", ");
            }
            params.append(p.toString());
        }
        String desc = Documentation.validateHTML(StringUtils.join(typeDescription, "<br/>"), "functions");
        String since = Documentation.validateHTML(StringUtils.join(typeSince, "\n"), "functions");
        if (desc == null || since == null) {
            Skript.warning("Function " + func.getName() + "'s description or 'since' is invalid");
            return;
        }
        Documentation.replaceInto(pw, "functions", "name, parameters, description, examples, since", Documentation.escapeHTML(func.getName()), Documentation.escapeHTML(params.toString()), desc, Documentation.escapeHTML(StringUtils.join(typeExamples, "\n")), since);
    }

    private static void insertOnDuplicateKeyUpdate(PrintWriter pw, String table, String fields, String update, String ... values) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = Documentation.escapeSQL(values[i]);
        }
        pw.println("INSERT INTO " + table + " (" + fields + ") VALUES ('" + StringUtils.join(values, "','") + "') ON DUPLICATE KEY UPDATE " + update + ";");
    }

    private static void replaceInto(PrintWriter pw, String table, String fields, String ... values) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = Documentation.escapeSQL(values[i]);
        }
        pw.println("REPLACE INTO " + table + " (" + fields + ") VALUES ('" + StringUtils.join(values, "','") + "');");
    }

    @Nullable
    private static String validateHTML(@Nullable String html, String baseURL) {
        if (html == null) {
            assert (false);
            return null;
        }
        for (Pattern p : validation) {
            if (!p.matcher((CharSequence)html).find()) continue;
            return null;
        }
        html = ((String)html).replaceAll("&(?!(amp|lt|gt|quot);)", "&amp;");
        Matcher m = Pattern.compile("<a href='(.*?)'>").matcher((CharSequence)html);
        block3: while (m.find()) {
            String url = m.group(1);
            String[] s = url.split("#");
            if (s.length == 1) continue;
            if (s[0].isEmpty()) {
                s[0] = "../" + baseURL + "/";
            }
            if (s[0].startsWith("../") && s[0].endsWith("/")) {
                if (s[0].equals("../classes/")) {
                    if (Classes.getClassInfoNoError(s[1]) != null) {
                        continue;
                    }
                } else if (s[0].equals("../events/")) {
                    for (SkriptEventInfo<?> i : Skript.getEvents()) {
                        if (!s[1].equals(i.getId())) continue;
                        continue block3;
                    }
                } else if (s[0].equals("../functions/")) {
                    if (Functions.getGlobalFunction(s[1]) != null) {
                        continue;
                    }
                } else {
                    int i = CollectionUtils.indexOf(urls, s[0].substring("../".length(), s[0].length() - 1));
                    if (i != -1) {
                        try {
                            Class.forName("ch.njol.skript." + urls[i] + "." + s[1]);
                            continue;
                        }
                        catch (ClassNotFoundException classNotFoundException) {
                            // empty catch block
                        }
                    }
                }
            }
            Skript.warning("invalid link '" + url + "' found in '" + (String)html + "'");
        }
        return html;
    }

    private static String escapeSQL(String value) {
        return value.replace("'", "\\'").replace("\"", "\\\"");
    }

    public static String escapeHTML(@Nullable String value) {
        if (value == null) {
            assert (false);
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static String[] escapeHTML(@Nullable String[] values) {
        for (int i = 0; i < values.length; ++i) {
            values[i] = Documentation.escapeHTML(values[i]);
        }
        return values;
    }

    static {
        validation.add(Pattern.compile("<(?!a href='|/a>|br ?/|/?(i|b|u|code|pre|ul|li|em)>)"));
        validation.add(Pattern.compile("(?<!</a|'|br ?/|/?(i|b|u|code|pre|ul|li|em))>"));
        urls = new String[]{"expressions", "effects", "conditions"};
    }
}

