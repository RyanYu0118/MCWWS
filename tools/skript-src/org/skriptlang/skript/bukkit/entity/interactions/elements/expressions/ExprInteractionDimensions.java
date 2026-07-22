/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Interaction
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.interactions.elements.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Interaction Height/Width")
@Description(value={"Returns the height or width of an interaction entity's hitbox. Both default to 1.\nThe width of the hitbox determines the x/z widths\n"})
@Example.Examples(value={@Example(value="set interaction height of last spawned interaction to 5.3"), @Example(value="set interaction width of last spawned interaction to 2")})
@Since(value={"2.14"})
public class ExprInteractionDimensions
extends SimplePropertyExpression<Entity, Number> {
    private boolean width;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprInteractionDimensions.infoBuilder(ExprInteractionDimensions.class, Number.class, "interaction (height|:width)[s]", "entities", true).supplier(ExprInteractionDimensions::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.width = parseResult.hasTag("width");
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Number convert(Entity entity) {
        if (entity instanceof Interaction) {
            Interaction interaction = (Interaction)entity;
            return Float.valueOf(this.width ? interaction.getInteractionWidth() : interaction.getInteractionHeight());
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Class[] classArray;
        switch (mode) {
            case ADD: 
            case REMOVE: 
            case SET: 
            case RESET: {
                Class[] classArray2 = new Class[1];
                classArray = classArray2;
                classArray2[0] = Number.class;
                break;
            }
            default: {
                classArray = null;
            }
        }
        return classArray;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Entity[] entities = (Entity[])this.getExpr().getArray(event);
        if (entities.length == 0) {
            return;
        }
        float deltaValue = delta == null ? 1.0f : ((Number)delta[0]).floatValue();
        block5: for (Entity entity : entities) {
            if (!(entity instanceof Interaction)) continue;
            Interaction interaction = (Interaction)entity;
            switch (mode) {
                case REMOVE: {
                    deltaValue = -deltaValue;
                }
                case ADD: {
                    if (this.width) {
                        interaction.setInteractionWidth(Math.max(interaction.getInteractionWidth() + deltaValue, 0.0f));
                        continue block5;
                    }
                    interaction.setInteractionHeight(Math.max(interaction.getInteractionHeight() + deltaValue, 0.0f));
                    continue block5;
                }
                case SET: 
                case RESET: {
                    deltaValue = Math.max(deltaValue, 0.0f);
                    if (this.width) {
                        interaction.setInteractionWidth(deltaValue);
                        continue block5;
                    }
                    interaction.setInteractionHeight(deltaValue);
                }
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    protected String getPropertyName() {
        return "interaction " + (this.width ? "width" : "height");
    }
}

