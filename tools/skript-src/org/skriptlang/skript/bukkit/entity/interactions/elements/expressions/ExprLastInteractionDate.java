/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Interaction
 *  org.bukkit.entity.Interaction$PreviousInteraction
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.interactions.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.Date;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.entity.interactions.InteractionType;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Last Interaction Date")
@Description(value={"Returns the date of the last attack (left click), or interaction (right click) on an interaction entity\nUsing 'clicked on' will return the latest attack or interaction, whichever was more recent.\n"})
@Examples(value={"if the last time {_interaction} was clicked < 5 seconds ago"})
@Since(value={"2.14"})
public class ExprLastInteractionDate
extends SimplePropertyExpression<Entity, Date> {
    private InteractionType interactionType;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprLastInteractionDate.class, Date.class).addPatterns("[the] last (date|time)[s] [that|when] %entities% (were|was) (attacked|1:interacted with|2:clicked [on])")).supplier(ExprLastInteractionDate::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.interactionType = InteractionType.values()[parseResult.mark];
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public Date convert(Entity entity) {
        if (entity instanceof Interaction) {
            Interaction.PreviousInteraction lastInteraction;
            Interaction interaction = (Interaction)entity;
            switch (this.interactionType) {
                default: {
                    throw new MatchException(null, null);
                }
                case ATTACK: {
                    Interaction.PreviousInteraction previousInteraction = interaction.getLastAttack();
                    break;
                }
                case INTERACT: {
                    Interaction.PreviousInteraction previousInteraction = interaction.getLastInteraction();
                    break;
                }
                case BOTH: {
                    Interaction.PreviousInteraction previousInteraction = lastInteraction = InteractionType.getLatest(interaction);
                }
            }
            if (lastInteraction == null) {
                return null;
            }
            return new Date(lastInteraction.getTimestamp());
        }
        return null;
    }

    @Override
    public Class<? extends Date> getReturnType() {
        return Date.class;
    }

    @Override
    protected String getPropertyName() {
        return "UNUSED";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder syntaxStringBuilder = new SyntaxStringBuilder(event, debug).append((Object)"the last date that").append((Object)this.getExpr()).append((Object)(this.getExpr().isSingle() ? "was" : "were"));
        return syntaxStringBuilder.append((Object)(switch (this.interactionType) {
            default -> throw new MatchException(null, null);
            case InteractionType.ATTACK -> "attacked";
            case InteractionType.INTERACT -> "interacted with";
            case InteractionType.BOTH -> "clicked on";
        })).toString();
    }
}

