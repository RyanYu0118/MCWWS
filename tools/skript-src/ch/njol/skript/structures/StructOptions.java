/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.StringUtils;
import java.lang.invoke.LambdaMetafactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.script.ScriptData;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Options")
@Description(value={"Options are used for replacing parts of a script with something else.", "For example, an option may represent a message that appears in multiple locations.", "Take a look at the example below that showcases this."})
@Example(value="options:\n\tno_permission: You're missing the required permission to execute this command!\n\ncommand /ping:\n\tpermission: command.ping\n\tpermission message: {@no_permission}\n\ttrigger:\n\t\tmessage \"Pong!\"\n\ncommand /pong:\n\tpermission: command.pong\n\tpermission message: {@no_permission}\n\ttrigger:\n\t\tmessage \"Ping!\"\n")
@Since(value={"1.0"})
public class StructOptions
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(100);

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        SectionNode node = entryContainer.getSource();
        node.convertToEntries(-1);
        this.loadOptions(node, "", this.getParser().getCurrentScript().getData(OptionsData.class, (Supplier<OptionsData>)LambdaMetafactory.metafactory(null, null, null, ()Ljava/lang/Object;, <init>(), ()Lch/njol/skript/structures/StructOptions$OptionsData;)()).options);
        return true;
    }

    private void loadOptions(SectionNode sectionNode, String prefix, Map<String, String> options) {
        for (Node node : sectionNode) {
            if (node instanceof EntryNode) {
                options.put(prefix + node.getKey(), ((EntryNode)node).getValue());
                continue;
            }
            if (node instanceof SectionNode) {
                this.loadOptions((SectionNode)node, prefix + node.getKey() + ".", options);
                continue;
            }
            Skript.error("Invalid line in options");
        }
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public void unload() {
        this.getParser().getCurrentScript().removeData(OptionsData.class);
    }

    @Override
    public Structure.Priority getPriority() {
        return PRIORITY;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "options";
    }

    static {
        Skript.registerStructure(StructOptions.class, "options");
    }

    public static final class OptionsData
    implements ScriptData {
        private final Map<String, String> options = new HashMap<String, String>();

        public String replaceOptions(String string) {
            return StringUtils.replaceAll((CharSequence)string, "\\{@(.+?)\\}", m -> {
                String option = this.options.get(m.group(1));
                if (option == null) {
                    Skript.error("undefined option " + m.group());
                    return m.group();
                }
                return Matcher.quoteReplacement(option);
            });
        }

        public Map<String, String> getOptions() {
            return Collections.unmodifiableMap(this.options);
        }
    }
}

