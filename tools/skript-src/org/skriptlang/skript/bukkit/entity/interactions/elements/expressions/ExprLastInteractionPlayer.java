/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Interaction
 *  org.bukkit.entity.Interaction$PreviousInteraction
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.entity.interactions.elements.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.entity.interactions.InteractionType;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Last Interaction Player")
@Description(value={"Returns the last player to attack (left click), or interact (right click) with an interaction entity.\nIf 'click on' or 'clicked on' are used, this will return the last player to either attack or interact with the entity whichever was most recent.\n"})
@Example.Examples(value={@Example(value="kill the last player that attacked the last spawned interaction"), @Example(value="feed the last player who interacted with {_i}")})
@Since(value={"2.14"})
public class ExprLastInteractionPlayer
extends SimplePropertyExpression<Entity, OfflinePlayer> {
    private InteractionType interactionType;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)((DefaultSyntaxInfos.Expression.Builder)DefaultSyntaxInfos.Expression.builder(ExprLastInteractionPlayer.class, OfflinePlayer.class).addPatterns("[the] last player[s] to (attack|1:interact with|2:click [on]) %entities%", "[the] last player[s] (who|that) (attacked|1:interacted with|2:clicked [on]) %entities%")).supplier(ExprLastInteractionPlayer::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.interactionType = InteractionType.values()[parseResult.mark];
        return super.init(exprs, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public OfflinePlayer convert(Entity entity) {
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
            return lastInteraction.getPlayer();
        }
        return null;
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    protected String getPropertyName() {
        return "UNUSED";
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder syntaxStringBuilder = new SyntaxStringBuilder(event, debug).append((Object)"the last player to");
        return syntaxStringBuilder.append((Object)(switch (this.interactionType) {
            default -> throw new MatchException(null, null);
            case InteractionType.ATTACK -> "attack";
            case InteractionType.INTERACT -> "interact with";
            case InteractionType.BOTH -> "click on";
        })).append((Object)this.getExpr()).toString();
    }
}

