/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.ScriptLoader
 *  ch.njol.skript.Skript
 *  ch.njol.skript.command.EffectCommandEvent
 *  ch.njol.skript.lang.Effect
 *  ch.njol.skript.lang.Expression
 *  ch.njol.skript.lang.Literal
 *  ch.njol.skript.lang.SkriptParser$ParseResult
 *  ch.njol.util.Kleenean
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.lang.entry.EntryContainer
 *  org.skriptlang.skript.lang.script.Script
 *  org.skriptlang.skript.lang.structure.Structure
 *  org.skriptlang.skript.lang.structure.Structure$Priority
 */
package org.skriptlang.reflect.java.elements.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.command.EffectCommandEvent;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

public class StructImport
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(150);
    private static final Pattern IMPORT_STATEMENT = Pattern.compile("((?:[_a-zA-Z$][\\w$]*\\.)*(?:[_a-zA-Z$][\\w$]*))(?:\\s+as ([_a-zA-Z$][\\w$]*))?");
    private static final Map<Script, Map<String, JavaType>> imports = new HashMap<Script, Map<String, JavaType>>();
    private Script script;

    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        this.script = this.getParser().getCurrentScript();
        assert (entryContainer != null);
        entryContainer.getSource().forEach(node -> StructImport.registerImport(Optional.ofNullable(node.getKey()).map(ScriptLoader::replaceOptions).orElse(null), this.script));
        return true;
    }

    public boolean load() {
        return true;
    }

    public void unload() {
        imports.remove(this.script);
    }

    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "import";
    }

    private static boolean registerImport(String rawStatement, @Nullable Script script) {
        Class<?> javaClass;
        Matcher statement = IMPORT_STATEMENT.matcher(ScriptLoader.replaceOptions((String)rawStatement));
        if (!statement.matches()) {
            Skript.error((String)(rawStatement + " is an invalid import statement."));
            return false;
        }
        String cls = statement.group(1);
        try {
            javaClass = LibraryLoader.getClassLoader().loadClass(cls);
        }
        catch (ClassNotFoundException ex) {
            Skript.error((String)(cls + " refers to a non-existent class."));
            return false;
        }
        String importName = statement.group(2);
        if (javaClass.getSimpleName().equals(importName)) {
            Skript.warning((String)(cls + " doesn't need the alias " + importName + ", as it will already be imported under that name"));
        }
        if (importName == null) {
            importName = javaClass.getSimpleName();
        }
        imports.computeIfAbsent(script, s -> new HashMap()).compute(importName, (name, oldClass) -> {
            if (oldClass != null) {
                Skript.error((String)(name + " is already mapped to " + String.valueOf(oldClass.getJavaClass()) + ". It will not be remapped to " + String.valueOf(javaClass) + "."));
                return oldClass;
            }
            return new JavaType(javaClass);
        });
        return true;
    }

    public static JavaType lookup(Script script, String identifier) {
        Map<String, JavaType> localImports = imports.get(script);
        if (localImports == null) {
            return null;
        }
        return localImports.get(identifier);
    }

    static {
        Skript.registerStructure(StructImport.class, (String[])new String[]{"import"});
        Skript.registerEffect(EffImport.class, (String[])new String[]{"import <" + IMPORT_STATEMENT.pattern() + ">"});
    }

    public static class EffImport
    extends Effect {
        private String className;

        public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
            if (!this.getParser().isCurrentEvent(EffectCommandEvent.class)) {
                Skript.error((String)"The import effect can only be used in effect commands. To use imports in scripts, use the section.");
                return false;
            }
            this.className = ((MatchResult)parseResult.regexes.get(0)).group();
            return StructImport.registerImport(this.className, null);
        }

        protected void execute(Event event) {
        }

        public String toString(@Nullable Event e, boolean debug) {
            return "import " + this.className;
        }
    }
}

