/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.aliases.ScriptAliases;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.Script;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Aliases")
@Description(value={"Used for registering custom aliases for a script."})
@Example(value="# Example aliases for a script\naliases:\n\tblacklisted items = TNT, bedrock, obsidian, mob spawner, lava, lava bucket\n\tshiny swords = golden sword, iron sword, diamond sword\n")
@Since(value={"1.0"})
public class StructAliases
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(200);

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        SectionNode node = entryContainer.getSource();
        node.convertToEntries(0, "=");
        Script script = this.getParser().getCurrentScript();
        ScriptAliases scriptAliases = Aliases.getScriptAliases(script);
        if (scriptAliases == null) {
            scriptAliases = Aliases.createScriptAliases(script);
        }
        scriptAliases.parser.load(node);
        return true;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "aliases";
    }

    static {
        Skript.registerStructure(StructAliases.class, "aliases");
    }
}

