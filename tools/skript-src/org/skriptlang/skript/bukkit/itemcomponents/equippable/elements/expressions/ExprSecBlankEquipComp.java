/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions;

import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Blank Equippable Component")
@Description(value={"Gets a blank equippable component.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example(value="set {_component} to a blank equippable component:\n\tset the camera overlay to \"custom_overlay\"\n\tset the allowed entities to a zombie and a skeleton\n\tset the equip sound to \"block.note_block.pling\"\n\tset the equipped model id to \"custom_model\"\n\tset the shear sound to \"ui.toast.in\"\n\tset the equipment slot to chest slot\n\tallow event-equippable component to be damage when hurt\n\tallow event-equippable component to be dispensed\n\tallow event-equippable component to be equipped onto entities\n\tallow event-equippable component to be sheared off\n\tallow event-equippable component to swap equipment\nset the equippable component of {_item} to {_component}\n")
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class ExprSecBlankEquipComp
extends SectionExpression<EquippableWrapper>
implements EquippableExperimentSyntax {
    private Trigger trigger;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprSecBlankEquipComp.class, EquippableWrapper.class).addPatterns("a (blank|empty) equippable component")).supplier(ExprSecBlankEquipComp::new)).build());
        EventValues.registerEventValue(BlankEquippableSectionEvent.class, EquippableWrapper.class, BlankEquippableSectionEvent::getWrapper);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean delayed, SkriptParser.ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        if (node != null) {
            this.trigger = SectionUtils.loadLinkedCode("blank equippable component", (beforeLoading, afterLoading) -> this.loadCode(node, "blank equippable component", (Runnable)beforeLoading, (Runnable)afterLoading, (Class<? extends Event>)BlankEquippableSectionEvent.class));
            return this.trigger != null;
        }
        return true;
    }

    protected EquippableWrapper @Nullable [] get(Event event) {
        EquippableWrapper wrapper = EquippableWrapper.newInstance();
        if (this.trigger != null) {
            BlankEquippableSectionEvent sectionEvent = new BlankEquippableSectionEvent(wrapper);
            Variables.withLocalVariables(event, sectionEvent, () -> TriggerItem.walk(this.trigger, sectionEvent));
        }
        return new EquippableWrapper[]{wrapper};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<EquippableWrapper> getReturnType() {
        return EquippableWrapper.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a blank equippable component";
    }

    private static class BlankEquippableSectionEvent
    extends Event {
        private final EquippableWrapper wrapper;

        public BlankEquippableSectionEvent(EquippableWrapper wrapper) {
            this.wrapper = wrapper;
        }

        public EquippableWrapper getWrapper() {
            return this.wrapper;
        }

        @NotNull
        public HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}

