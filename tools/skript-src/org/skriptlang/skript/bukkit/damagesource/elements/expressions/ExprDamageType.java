/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.damage.DamageSource
 *  org.bukkit.damage.DamageType
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.damagesource.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.damagesource.elements.expressions.ExprSecDamageSource;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Damage Source - Damage Type")
@Description(value={"The type of damage of a damage source.", "Attributes of a damage source cannot be changed once created, only while within the 'custom damage source' section."})
@Example.Examples(value={@Example(value="set {_source} to a custom damage source:\n\tset the damage type to magic\n\tset the causing entity to {_player}\n\tset the direct entity to {_arrow}\n\tset the damage location to location(0, 0, 10)\ndamage all players by 5 using {_source}\n"), @Example(value="on death:\n\tset {_type} to the damage type of event-damage source\n")})
@Since(value={"2.12"})
public class ExprDamageType
extends SimplePropertyExpression<DamageSource, DamageType> {
    private boolean isEvent;

    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprDamageType.infoBuilder(ExprDamageType.class, DamageType.class, "damage type", "damagesources", true).supplier(ExprDamageType::new)).build());
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.isEvent = this.getParser().isCurrentEvent((Class<? extends Event>)ExprSecDamageSource.DamageSourceSectionEvent.class);
        return super.init(expressions, matchedPattern, isDelayed, parseResult);
    }

    @Override
    @Nullable
    public DamageType convert(DamageSource damageSource) {
        return damageSource.getDamageType();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (!this.isEvent) {
            Skript.error("You cannot change the attributes of a damage source outside a 'custom damage source' section.");
        } else if (!this.getExpr().isSingle() || !this.getExpr().isDefault()) {
            Skript.error("You can only change the attributes of the damage source being created in this section.");
        } else if (mode == Changer.ChangeMode.SET) {
            return CollectionUtils.array(DamageType.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        assert (delta != null);
        if (!(event instanceof ExprSecDamageSource.DamageSourceSectionEvent)) {
            return;
        }
        ExprSecDamageSource.DamageSourceSectionEvent sectionEvent = (ExprSecDamageSource.DamageSourceSectionEvent)event;
        sectionEvent.damageType = (DamageType)delta[0];
    }

    @Override
    public Class<DamageType> getReturnType() {
        return DamageType.class;
    }

    @Override
    protected String getPropertyName() {
        return "damage type";
    }
}

