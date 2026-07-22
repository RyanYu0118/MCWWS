/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.registrations;

import ch.njol.skript.SkriptAddon;
import ch.njol.skript.doc.Documentable;
import ch.njol.skript.patterns.PatternCompiler;
import ch.njol.skript.patterns.SkriptPattern;
import com.google.common.base.Preconditions;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.lang.experiment.Experiment;
import org.skriptlang.skript.lang.experiment.ExperimentRegistry;
import org.skriptlang.skript.lang.experiment.LifeCycle;

public enum Feature implements Experiment,
Documentable
{
    EXAMPLES("examples", "Examples", "A section used to provide examples inside code.\n\n```\nexample:\n\tkick the player due to \"you are not allowed here!\"\n```\n", LifeCycle.STABLE, new String[0]),
    QUEUES("queues", "Queues", "A collection that removes elements whenever they are requested.\n\nThis is useful for processing tasks or keeping track of things that need to happen only once.\n\n```\nset {queue} to a new queue of \"hello\" and \"world\"\n\nbroadcast the first element of {queue}\n# \"hello\" is now removed\n\nbroadcast the first element of {queue}\n# \"world\" is now removed\n\n# queue is empty\n```\n\n```\nset {queue} to a new queue of all players\n\nset {player 1} to a random element out of {queue} \nset {player 2} to a random element out of {queue}\n# players 1 and 2 are guaranteed to be distinct\n```\n\nQueues can be looped over like a regular list.\n", LifeCycle.EXPERIMENTAL, new String[0]),
    FOR_EACH_LOOPS("for loop", "For Loops", "A new kind of loop syntax that stores the loop index and value in variables for convenience.\n\nThis can be used to avoid confusion when nesting multiple loops inside each other.\n\n```\nfor {_index}, {_value} in {my list::*}:\n\tbroadcast \"%{_index}%: %{_value}%\"\n```\n\n```\nfor each {_player} in all players:\n\tsend \"Hello %{_player}%!\" to {_player}\n```\n\nAll existing loop features are also available in this section.\n", LifeCycle.MAINSTREAM, "for [each] loop[s]"),
    SCRIPT_REFLECTION("reflection", "Script Reflection", "This feature includes:\n\n- The ability to reference a script in code.\n- Finding and running functions by name.\n- Reading configuration files and values.\n", LifeCycle.STABLE, "[script] reflection"),
    CATCH_ERRORS("catch runtime errors", "Runtime Error Catching", "A new catch runtime errors section allows you to catch and suppress runtime errors within it and access them later with the last caught runtime errors.\n\n```\ncatch runtime errors:\n\t...\n\tset worldborder center of {_border} to {_my unsafe location}\n\t...\nif last caught runtime errors contains \"Your location can't have a NaN value as one of its components\":\n\tset worldborder center of {_border} to location(0, 0, 0)\n```\n", LifeCycle.EXPERIMENTAL, "error catching [section]"),
    TYPE_HINTS("type hints", "Type Hints", "Local variable type hints enable Skript to understand what kind of values your local variables will hold at parse time. Consider the following example:\n\n```\nset {_a} to 5\nset {_b} to \"some string\"\n... do stuff ...\nset {_c} to {_a} in lowercase # oops i used the wrong variable\n```\n\nPreviously, the code above would parse without issue. However, Skript now understands that when it is used, {_a} could only be a number (and not a text). Thus, the code above would now error with a message about mismatched types.\n\nPlease note that this feature is currently only supported by simple local variables. A simple local variable is one whose name does not contain any expressions:\n\n```\n{_var} # can use type hints\n{_var::%player's name%} # can't use type hints\n```\n", LifeCycle.EXPERIMENTAL, "[local variable] type hints"),
    DAMAGE_SOURCE("damage source", "Damage Sources", "Damage sources are a more advanced and detailed version of damage causes. Damage sources include information such as the type of damage, the location where the damage originated from, the entity that directly caused the damage, and more.\n\nBelow is an example of what damaging using custom damage sources looks like:\n\n```\ndamage all players by 5 using a custom damage source:\n\tset the damage type to magic\n\tset the causing entity to {_player}\n\tset the direct entity to {_arrow}\n\tset the damage location to location(0, 0, 10)\n```\n\nFor more details about the syntax, visit damage source on our documentation website.\n", LifeCycle.MAINSTREAM, "damage source[s]"),
    EQUIPPABLE_COMPONENTS("equippable components", "Equippable Components", "Equippable components allow retrieving and changing the data of an item in the usage as equipment/armor.\n\nBelow is an example of creating a blank equippable component, modifying it, and applying it to an item:\n\n```\nset {_component} to a blank equippable component:\n\tset the camera overlay to \"custom_overlay\"\n\tset the allowed entities to a zombie and a skeleton\n\tset the equip sound to \"block.note_block.pling\"\n\tset the equipped model id to \"custom_model\"\n\tset the shear sound to \"ui.toast.in\"\n\tset the equipment slot to chest slot\n\tallow event-equippable component to be damage when hurt\n\tallow event-equippable component to be dispensed\n\tallow event-equippable component to be equipped onto entities\n\tallow event-equippable component to be sheared off\n\tallow event-equippable component to swap equipment\nset the equippable component of {_item} to {_component}\n```\n", LifeCycle.EXPERIMENTAL, "equippable components");

    private final String displayName;
    private final String codeName;
    private final String description;
    private final LifeCycle phase;
    private final SkriptPattern compiledPattern;

    private Feature(@NotNull String codeName, @NotNull String displayName, String description, LifeCycle phase, String ... patterns) {
        Preconditions.checkNotNull((Object)codeName, (Object)"codeName cannot be null");
        Preconditions.checkNotNull((Object)displayName, (Object)"displayName cannot be null");
        Preconditions.checkNotNull((Object)description, (Object)"description cannot be null");
        this.displayName = displayName;
        this.description = description.strip();
        this.codeName = codeName;
        this.phase = phase;
        this.compiledPattern = switch (patterns.length) {
            case 0 -> PatternCompiler.compile(codeName);
            case 1 -> PatternCompiler.compile(patterns[0]);
            default -> PatternCompiler.compile("(" + String.join((CharSequence)"|", patterns) + ")");
        };
    }

    public static void registerAll(SkriptAddon addon, ExperimentRegistry manager) {
        for (Feature value : Feature.values()) {
            manager.register(addon, value);
        }
    }

    @NotNull
    public String displayName() {
        return this.displayName;
    }

    @Override
    public String codeName() {
        return this.codeName;
    }

    @Override
    public LifeCycle phase() {
        return this.phase;
    }

    @Override
    public SkriptPattern pattern() {
        return this.compiledPattern;
    }

    @Override
    @NotNull
    public List<String> description() {
        return List.of(this.description);
    }

    @Override
    public @Unmodifiable @NotNull List<String> since() {
        return List.of();
    }

    @Override
    public @Unmodifiable @NotNull List<String> examples() {
        return List.of();
    }

    @Override
    public @Unmodifiable @NotNull List<String> keywords() {
        return List.of();
    }

    @Override
    public @Unmodifiable @NotNull List<String> requires() {
        return List.of();
    }
}

