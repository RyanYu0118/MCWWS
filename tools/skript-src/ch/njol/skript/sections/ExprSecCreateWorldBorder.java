/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.WorldBorder
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name(value="Create WorldBorder")
@Description(value={"Creates a new, unused world border. World borders can be assigned to either worlds or specific players.", "Borders assigned to worlds apply to all players in that world.", "Borders assigned to players apply only to those players, and different players can have different borders."})
@Example.Examples(value={@Example(value="on join:\n\tset {_location} to location of player\n\tset worldborder of player to a virtual worldborder:\n\t\tset worldborder radius to 25\n\t\tset world border center of event-worldborder to {_location}\n"), @Example(value="on load:\n\tset worldborder of world \"world\" to a worldborder:\n\t\tset worldborder radius of event-worldborder to 200\n\t\tset worldborder center of event-worldborder to location(0, 64, 0)\n\t\tset worldborder warning distance of event-worldborder to 5\n")})
@Since(value={"2.11"})
public class ExprSecCreateWorldBorder
extends SectionExpression<WorldBorder> {
    private Trigger trigger = null;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        if (node != null) {
            this.trigger = SectionUtils.loadLinkedCode("create worldborder", (beforeLoading, afterLoading) -> this.loadCode(node, "create worldborder", (Runnable)beforeLoading, (Runnable)afterLoading, (Class<? extends Event>)CreateWorldborderEvent.class));
            return this.trigger != null;
        }
        return true;
    }

    protected WorldBorder @Nullable [] get(Event event) {
        WorldBorder worldBorder = Bukkit.createWorldBorder();
        if (this.trigger == null) {
            return new WorldBorder[]{worldBorder};
        }
        CreateWorldborderEvent worldborderEvent = new CreateWorldborderEvent(worldBorder);
        Variables.withLocalVariables(event, worldborderEvent, () -> TriggerItem.walk(this.trigger, worldborderEvent));
        return new WorldBorder[]{worldborderEvent.getWorldBorder()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<WorldBorder> getReturnType() {
        return WorldBorder.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a virtual worldborder";
    }

    static {
        Skript.registerExpression(ExprSecCreateWorldBorder.class, WorldBorder.class, ExpressionType.SIMPLE, "a [virtual] world[ ]border");
        EventValues.registerEventValue(CreateWorldborderEvent.class, WorldBorder.class, CreateWorldborderEvent::getWorldBorder);
    }

    public static class CreateWorldborderEvent
    extends Event {
        private final WorldBorder worldborder;

        public CreateWorldborderEvent(WorldBorder worldborder) {
            this.worldborder = worldborder;
        }

        public WorldBorder getWorldBorder() {
            return this.worldborder;
        }

        @NotNull
        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}

