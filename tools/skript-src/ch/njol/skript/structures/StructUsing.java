/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.structure.Structure;

@Name(value="Using Experimental Feature")
@Description(value={"Place at the top of a script file to enable an optional experimental feature.", "Experimental features may change behavior in Skript and may contain bugs. Use at your own discretion.", "A list of the available experimental features can be found in the changelog for your version of Skript."})
@Example.Examples(value={@Example(value="using 1.21"), @Example(value="using the experiment my-cool-addon-feature")})
@Since(value={"2.9.0"})
public class StructUsing
extends Structure {
    public static final Structure.Priority PRIORITY = new Structure.Priority(15);
    private Experiment experiment;

    @Override
    public boolean init(Literal<?> @NotNull [] arguments, int pattern, SkriptParser.ParseResult result, @Nullable EntryContainer container) {
        this.enableExperiment(result.regexes.get(0).group());
        return true;
    }

    private void enableExperiment(String name) {
        this.experiment = Skript.experiments().find(name.trim());
        switch (this.experiment.phase()) {
            case MAINSTREAM: {
                Skript.warning("The experimental feature '" + name + "' is now included by default and is no longer required.");
                break;
            }
            case DEPRECATED: {
                Skript.warning("The experimental feature '" + name + "' is deprecated and may be removed in future versions.");
                break;
            }
            case UNKNOWN: {
                Skript.warning("The experimental feature '" + name + "' was not found.");
            }
        }
        this.getParser().addExperiment(this.experiment);
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
    public String toString(@Nullable Event event, boolean debug) {
        return "using " + this.experiment.codeName();
    }

    static {
        Skript.registerSimpleStructure(StructUsing.class, "using [[the] experiment] <.+>");
    }
}

