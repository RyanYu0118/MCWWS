/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Keywords;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityType;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.particles.particleeffects.ParticleEffect;

@Name(value="X of Item/Entity Type")
@Description(value={"An expression for using an item or entity type with a different amount."})
@Example(value="give level of player of iron pickaxes to the player")
@Since(value={"1.2"})
@Keywords(value={"amount"})
public class ExprXOf
extends PropertyExpression<Object, Object> {
    private Class<?>[] possibleReturnTypes;
    private Expression<Number> amount;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.amount = exprs[0];
        Expression<Object> type = exprs[1];
        this.setExpr(type);
        if (this.amount instanceof Literal && this.amount.getSource() instanceof Literal && type instanceof Literal && type.getSource() instanceof Literal && type.canReturnAnyOf(ItemStack.class, ItemType.class)) {
            return false;
        }
        ArrayList<Class> possibleReturnTypes = new ArrayList<Class>();
        if (type.canReturn(ItemStack.class)) {
            possibleReturnTypes.add(ItemStack.class);
        }
        if (type.canReturn(ItemType.class)) {
            possibleReturnTypes.add(ItemType.class);
        }
        if (type.canReturn(EntityType.class)) {
            possibleReturnTypes.add(EntityType.class);
        }
        if (type.canReturn(ParticleEffect.class)) {
            possibleReturnTypes.add(ParticleEffect.class);
        }
        this.possibleReturnTypes = possibleReturnTypes.toArray(new Class[0]);
        return true;
    }

    @Override
    protected Object[] get(Event event, Object[] source) {
        Number amount = this.amount.getSingle(event);
        if (amount == null) {
            return (Object[])Array.newInstance(this.getReturnType(), 0);
        }
        long absAmount = Math.max(amount.longValue(), 0L);
        return this.get(source, object -> {
            if (object instanceof ItemStack) {
                ItemStack itemStack = (ItemStack)object;
                itemStack = itemStack.clone();
                itemStack.setAmount((int)absAmount);
                return itemStack;
            }
            if (object instanceof ItemType) {
                ItemType itemType = (ItemType)object;
                ItemType type = itemType.clone();
                type.setAmount(absAmount);
                return type;
            }
            if (object instanceof EntityType) {
                EntityType ogType = (EntityType)object;
                EntityType entityType = ogType.clone();
                entityType.amount = (int)absAmount;
                return entityType;
            }
            if (object instanceof ParticleEffect) {
                ParticleEffect particleEffect = (ParticleEffect)object;
                ParticleEffect effect = particleEffect.copy();
                effect.count((int)absAmount);
                return effect;
            }
            return null;
        });
    }

    @Override
    public Class<?> getReturnType() {
        return this.possibleReturnTypes.length == 1 ? this.possibleReturnTypes[0] : Object.class;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return Arrays.copyOf(this.possibleReturnTypes, this.possibleReturnTypes.length);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return this.amount.toString(event, debug) + " of " + this.getExpr().toString(event, debug);
    }

    @Override
    public Expression<?> simplify() {
        if (this.amount instanceof Literal && this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return super.simplify();
    }

    static {
        Skript.registerExpression(ExprXOf.class, Object.class, ExpressionType.PATTERN_MATCHES_EVERYTHING, "%number% of %itemstacks/itemtypes/entitytypes/particles%");
    }
}

