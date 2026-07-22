/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.common.types;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.Feature;
import java.io.File;
import java.nio.file.Path;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;
import org.skriptlang.skript.lang.script.Script;

@ApiStatus.Internal
public class ScriptClassInfo
extends ClassInfo<Script> {
    public ScriptClassInfo() {
        super(Script.class, "script");
        this.user("scripts?").name("Script").description("A script loaded by Skript.", "Disabled scripts will report as being empty since their content has not been loaded.").usage("").examples("the current script").since("2.10").parser(new ScriptParser()).property(Property.NAME, "A script's name, as text. If the experiment 'Script Reflection' is enabled, this will return the resolved name of the script, otherwise it returns the file name with path relative to the scripts folder. Cannot be changed.", Skript.instance(), new ScriptNameHandler());
    }

    private static class ScriptParser
    extends Parser<Script> {
        final Path path = Skript.getInstance().getScriptsFolder().getAbsoluteFile().toPath();

        private ScriptParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return switch (context) {
                case ParseContext.PARSE, ParseContext.COMMAND -> true;
                default -> false;
            };
        }

        @Override
        @Nullable
        public Script parse(String name, ParseContext context) {
            return switch (context) {
                case ParseContext.PARSE, ParseContext.COMMAND -> {
                    @Nullable File file = ScriptLoader.getScriptFromName(name);
                    if (file == null || !file.isFile()) {
                        yield null;
                    }
                    yield ScriptLoader.getScript(file);
                }
                default -> null;
            };
        }

        @Override
        public String toString(Script script, int flags) {
            @Nullable File file = script.getConfig().getFile();
            if (file == null) {
                return script.getConfig().getFileName();
            }
            return this.path.relativize(file.toPath().toAbsolutePath()).toString();
        }

        @Override
        public String toVariableNameString(Script script) {
            return this.toString(script, 0);
        }
    }

    private static class ScriptNameHandler
    implements ExpressionPropertyHandler<Script, String> {
        private boolean useResolvedName;

        private ScriptNameHandler() {
        }

        @Override
        public PropertyHandler<Script> newInstance() {
            return new ScriptNameHandler();
        }

        @Override
        public boolean init(Expression<?> parentExpression, ParserInstance parser) {
            this.useResolvedName = parser.hasExperiment(Feature.SCRIPT_REFLECTION);
            return true;
        }

        @Override
        public String convert(Script script) {
            if (this.useResolvedName) {
                return script.name();
            }
            return script.nameAndPath();
        }

        @Override
        @NotNull
        public Class<String> returnType() {
            return String.class;
        }
    }
}

