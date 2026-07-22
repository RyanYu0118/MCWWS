/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.ExecutionIntent;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.LoopSection;
import ch.njol.skript.lang.SectionExitHandler;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.TriggerSection;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.util.Kleenean;
import ch.njol.util.StringUtils;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@Name(value="Continue")
@Description(value={"Moves the loop to the next iteration. You may also continue an outer loop from an inner one. The loops are labelled from 1 until the current loop, starting with the outermost one."})
@Example.Examples(value={@Example(value="# Broadcast online moderators\nloop all players:\n\tif loop-value does not have permission \"moderator\":\n\t\tcontinue # filter out non moderators\n\tbroadcast \"%loop-player% is a moderator!\" # Only moderators get broadcast\n"), @Example(value="# Game starting counter\nset {_counter} to 11\nwhile {_counter} > 0:\n\tremove 1 from {_counter}\n\twait a second\n\tif {_counter} != 1, 2, 3, 5 or 10:\n\t\tcontinue # only print when counter is 1, 2, 3, 5 or 10\n\tbroadcast \"Game starting in %{_counter}% second(s)\"\n")})
@Since(value={"2.2-dev37, 2.7 (while loops), 2.8.0 (outer loops)"})
public class EffContinue
extends Effect {
    private int level;
    private @UnknownNullability LoopSection loop;
    private @UnknownNullability List<SectionExitHandler> sectionsToExit;
    private int breakLevels;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ParserInstance parser = this.getParser();
        int loops = parser.getCurrentSections(LoopSection.class).size();
        if (loops == 0) {
            Skript.error("The 'continue' effect may only be used in loops");
            return false;
        }
        int n = this.level = matchedPattern == 0 ? loops : Integer.parseInt(parseResult.regexes.get(0).group());
        if (this.level < 1) {
            return false;
        }
        int levels = loops - this.level + 1;
        if (levels <= 0) {
            Skript.error("Can't continue the " + StringUtils.fancyOrderNumber(this.level) + " loop as there " + (String)(loops == 1 ? "is only 1 loop" : "are only " + loops + " loops") + " present");
            return false;
        }
        List<TriggerSection> innerSections = parser.getSections(levels, LoopSection.class);
        this.breakLevels = innerSections.size();
        this.loop = (LoopSection)innerSections.remove(0);
        this.sectionsToExit = innerSections.stream().filter(SectionExitHandler.class::isInstance).map(SectionExitHandler.class::cast).toList();
        return true;
    }

    @Override
    protected void execute(Event event) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    protected TriggerItem walk(Event event) {
        this.debug(event, false);
        for (SectionExitHandler section : this.sectionsToExit) {
            section.exit(event);
        }
        return this.loop;
    }

    @Override
    public ExecutionIntent executionIntent() {
        return ExecutionIntent.stopSections(this.breakLevels);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "continue" + (String)(this.level == -1 ? "" : " the " + StringUtils.fancyOrderNumber(this.level) + " loop");
    }

    static {
        Skript.registerEffect(EffContinue.class, "continue [this loop|[the] [current] loop]", "continue [the] <-?\\d+(_\\d+)*>(st|nd|rd|th) loop");
    }
}

