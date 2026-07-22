/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.FunctionEvent;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.lang.function.Signature;
import ch.njol.skript.lang.parser.ParserInstance;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.common.function.FunctionParser;
import org.skriptlang.skript.common.function.FunctionReference;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Function")
@Description(value={"Functions are structures that can be executed with arguments/parameters to run code.", "They can also return a value to the trigger that is executing the function.", "Note that local functions come before global functions execution"})
@Example.Examples(value={@Example(value="function sayMessage(message: text):\n\tbroadcast {_message} # our message argument is available in '{_message}'\n"), @Example(value="local function giveApple(amount: number) :: item:\n\treturn {_amount} of apple\n"), @Example(value="function getPoints(p: player) returns number:\n\treturn {points::%{_p}%}\n")})
@Since(value={"2.2, 2.7 (local functions)"})
public class StructFunction
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(400);
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("^(?:local )?function (?<name>[\\p{IsAlphabetic}_][\\p{IsAlphabetic}\\d_]*)\\((?<args>.*?)\\)(?:\\s*(?:->|::| returns )\\s*(?<returns>.+))?$");
    private static final AtomicBoolean VALIDATE_FUNCTIONS = new AtomicBoolean();
    private SectionNode source;
    @Nullable
    private Signature<?> signature;
    private boolean local;

    @Override
    public boolean init(Literal<?>[] literals, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        assert (entryContainer != null);
        this.source = entryContainer.getSource();
        this.local = parseResult.hasTag("local");
        return true;
    }

    @Override
    public boolean preLoad() {
        String rawSignature = this.source.getKey();
        assert (rawSignature != null);
        Matcher matcher = SIGNATURE_PATTERN.matcher(rawSignature = ScriptLoader.replaceOptions(rawSignature));
        if (!matcher.matches()) {
            Skript.error("Invalid function signature: " + rawSignature);
            return false;
        }
        this.getParser().setCurrentEvent((this.local ? "local " : "") + "function", FunctionEvent.class);
        this.signature = FunctionParser.parse(this.getParser().getCurrentScript().getConfig().getFileName(), matcher.group("name"), matcher.group("args"), matcher.group("returns"), this.local);
        this.getParser().deleteCurrentEvent();
        return this.signature != null && Functions.registerSignature(this.signature) != null;
    }

    @Override
    public boolean load() {
        ParserInstance parser = this.getParser();
        parser.setCurrentEvent((this.local ? "local " : "") + "function", FunctionEvent.class);
        assert (this.signature != null);
        Functions.loadFunction(parser.getCurrentScript(), this.source, this.signature);
        parser.deleteCurrentEvent();
        VALIDATE_FUNCTIONS.set(true);
        return true;
    }

    @Override
    public boolean postLoad() {
        if (VALIDATE_FUNCTIONS.get()) {
            VALIDATE_FUNCTIONS.set(false);
            Functions.validateFunctions();
        }
        return true;
    }

    @Override
    public void unload() {
        assert (this.signature != null);
        Functions.unregisterFunction(this.signature);
        this.signature.calls().forEach(FunctionReference::invalidateCache);
        VALIDATE_FUNCTIONS.set(true);
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (this.local ? "local " : "") + "function";
    }

    static {
        Skript.registerStructure(StructFunction.class, "[:local] function <.+>");
    }
}

