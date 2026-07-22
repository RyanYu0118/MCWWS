/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.attribute.Attributable
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.entity.Entity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Entity Attribute")
@Description(value={"The numerical value of an entity's particular attribute.", "Note that the movement speed attribute cannot be reliably used for players. For that purpose, use the speed expression instead.", "Resetting an entity's attribute is only available in Minecraft 1.11 and above."})
@Example(value="on damage of player:\n\tsend \"You are wounded!\" to victim\n\tset victim's attack speed attribute to 2\n")
@Since(value={"2.5, 2.6.1 (final attribute value)"})
public class ExprEntityAttribute
extends PropertyExpression<Entity, Number> {
    @Nullable
    private Expression<Attribute> attributes;
    private boolean withModifiers;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.attributes = exprs[matchedPattern];
        this.setExpr(exprs[matchedPattern ^ 1]);
        this.withModifiers = parseResult.mark == 1;
        return true;
    }

    protected Number[] get(Event event, Entity[] entities) {
        Attribute attribute = this.attributes.getSingle(event);
        return (Number[])Stream.of(entities).map(ent -> ExprEntityAttribute.getAttribute(ent, attribute)).filter(Objects::nonNull).map(att -> this.withModifiers ? att.getValue() : att.getBaseValue()).toArray(Number[]::new);
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL || this.withModifiers) {
            return null;
        }
        return CollectionUtils.array(Number.class);
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Attribute attribute = this.attributes.getSingle(event);
        double deltaValue = delta == null ? 0.0 : ((Number)delta[0]).doubleValue();
        block8: for (Entity entity : (Entity[])this.getExpr().getArray(event)) {
            AttributeInstance instance = ExprEntityAttribute.getAttribute(entity, attribute);
            if (instance == null) continue;
            switch (mode) {
                case ADD: {
                    instance.setBaseValue(instance.getBaseValue() + deltaValue);
                    continue block8;
                }
                case SET: {
                    instance.setBaseValue(deltaValue);
                    continue block8;
                }
                case DELETE: {
                    instance.setBaseValue(0.0);
                    continue block8;
                }
                case RESET: {
                    AttributeInstance defaultValue = entity.getType().getDefaultAttributes().getAttribute(attribute);
                    if (defaultValue == null) continue block8;
                    instance.setBaseValue(defaultValue.getBaseValue());
                    continue block8;
                }
                case REMOVE: {
                    instance.setBaseValue(instance.getBaseValue() - deltaValue);
                    continue block8;
                }
                case REMOVE_ALL: {
                    assert (false);
                    continue block8;
                }
            }
        }
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "entity " + this.getExpr().toString(event, debug) + "'s " + (this.attributes == null ? "" : this.attributes.toString(event, debug)) + "attribute";
    }

    @Nullable
    private static AttributeInstance getAttribute(Entity entity, @Nullable Attribute attribute) {
        if (attribute != null && entity instanceof Attributable) {
            return ((Attributable)entity).getAttribute(attribute);
        }
        return null;
    }

    static {
        Skript.registerExpression(ExprEntityAttribute.class, Number.class, ExpressionType.COMBINED, "[the] %attributetype% [(1:(total|final|modified))] attribute [value] of %entities%", "%entities%'[s] %attributetype% [(1:(total|final|modified))] attribute [value]");
    }
}

