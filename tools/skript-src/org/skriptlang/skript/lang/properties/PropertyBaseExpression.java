/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.properties;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyBaseSyntax;
import org.skriptlang.skript.lang.properties.PropertyMap;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Experimental
public abstract class PropertyBaseExpression<Handler extends ExpressionPropertyHandler<?, ?>>
extends SimpleExpression<Object>
implements PropertyBaseSyntax<Handler> {
    protected Expression<?> expr;
    protected PropertyMap<Handler> properties;
    protected Class<?>[] returnTypes;
    protected Class<?> returnType;
    protected final Property<Handler> property = this.getProperty();
    protected boolean useCIP;
    private final ChangeDetails changeDetails = new ChangeDetails();

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!LiteralUtils.canInitSafely(LiteralUtils.defendExpression(expressions[0]))) {
            return false;
        }
        this.expr = PropertyBaseSyntax.asProperty(this.property, expressions[0]);
        if (this.expr == null) {
            Skript.error(this.getBadTypesErrorMessage(expressions[0]));
            return false;
        }
        this.properties = PropertyBaseSyntax.getPossiblePropertyInfos(this.property, this.expr);
        if (this.properties.isEmpty()) {
            Skript.error(this.getBadTypesErrorMessage(this.expr));
            return false;
        }
        for (Property.PropertyInfo propertyInfo : this.properties.values()) {
            if (!((ExpressionPropertyHandler)propertyInfo.handler()).requiresSourceExprChange()) continue;
            this.useCIP = true;
            break;
        }
        this.returnTypes = this.getPropertyReturnTypes(this.properties, ExpressionPropertyHandler::possibleReturnTypes);
        this.returnType = Utils.getSuperType(this.returnTypes);
        return LiteralUtils.canInitSafely(this.expr);
    }

    protected Class<?> @NotNull [] getPropertyReturnTypes(@NotNull PropertyMap<Handler> properties, Function<Handler, Class<?>[]> getReturnType) {
        return (Class[])properties.values().stream().flatMap(propertyInfo -> Arrays.stream((Class[])getReturnType.apply((ExpressionPropertyHandler)propertyInfo.handler()))).filter(type -> type != Object.class).toArray(Class[]::new);
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        return this.expr.stream(event).flatMap(source -> {
            ExpressionPropertyHandler handler = (ExpressionPropertyHandler)this.properties.getHandler(source.getClass());
            if (handler == null) {
                return null;
            }
            Object value = this.convert(event, handler, source);
            if (value != null && value.getClass().isArray()) {
                return Arrays.stream((Object[])value);
            }
            return Stream.of(value);
        }).filter(Objects::nonNull).toArray(size -> (Object[])Array.newInstance(this.getReturnType(), size));
    }

    @Nullable
    protected <T> Object convert(Event event, Handler handler, T source) {
        return handler.convert(event, source);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Set<Object> changableTypes = Set.of();
        if (this.useCIP && (changableTypes = this.properties.keySet().stream().filter(type -> Changer.ChangerUtils.acceptsChange(this.expr, Changer.ChangeMode.SET, type)).collect(Collectors.toSet())).isEmpty()) {
            return null;
        }
        HashSet allowedChangeTypes = new HashSet();
        for (Map.Entry entry : this.properties.entrySet()) {
            Class propertyType = (Class)entry.getKey();
            Property.PropertyInfo propertyInfo = (Property.PropertyInfo)entry.getValue();
            if (this.useCIP && !changableTypes.contains(propertyType)) {
                this.changeDetails.storeTypes(mode, propertyInfo, null);
            }
            Class<?>[] types = ((ExpressionPropertyHandler)propertyInfo.handler()).acceptChange(mode);
            this.changeDetails.storeTypes(mode, propertyInfo, types);
            if (types == null) continue;
            if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
                return new Class[0];
            }
            allowedChangeTypes.addAll(Arrays.asList(types));
        }
        if (allowedChangeTypes.isEmpty()) {
            return null;
        }
        return allowedChangeTypes.toArray(new Class[0]);
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Function<Object, Object> updateTypeFunction = propertyHaver -> {
            Property.PropertyInfo<Handler> propertyInfo = this.properties.get(propertyHaver.getClass());
            if (propertyInfo == null) {
                return null;
            }
            Class<?>[] allowedTypes = this.changeDetails.getTypes(mode, propertyInfo);
            if (allowedTypes == null) {
                return null;
            }
            if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
                ExpressionPropertyHandler handler = (ExpressionPropertyHandler)propertyInfo.handler();
                handler.change(propertyHaver, null, mode);
                return propertyHaver;
            }
            assert (delta != null);
            Object[] verifiedDelta = delta;
            Class[] flatAllowedTypes = new Class[allowedTypes.length];
            for (int i = 0; i < allowedTypes.length; ++i) {
                flatAllowedTypes[i] = Utils.getComponentType(allowedTypes[i]);
            }
            boolean tryConverting = false;
            block1: for (Object object : verifiedDelta) {
                for (Class allowedType : flatAllowedTypes) {
                    if (allowedType.isInstance(object)) continue block1;
                }
                tryConverting = true;
            }
            if (tryConverting) {
                Object[] newDelta = new Object[verifiedDelta.length];
                Converters.convert(verifiedDelta, newDelta, flatAllowedTypes);
                for (Object object : newDelta) {
                    if (object != null) continue;
                    return null;
                }
                verifiedDelta = newDelta;
            }
            ExpressionPropertyHandler handler = (ExpressionPropertyHandler)propertyInfo.handler();
            handler.change(propertyHaver, verifiedDelta, mode);
            return propertyHaver;
        };
        if (this.useCIP) {
            this.expr.changeInPlace(event, updateTypeFunction);
        } else {
            this.expr.stream(event).forEach(updateTypeFunction::apply);
        }
    }

    @Override
    public boolean isSingle() {
        return this.expr.isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return this.returnType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return this.returnTypes;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return this.getPropertyName() + " of " + this.expr.toString(event, debug);
    }

    class ChangeDetails
    extends EnumMap<Changer.ChangeMode, Map<Property.PropertyInfo<Handler>, Class<?>[]>> {
        public ChangeDetails() {
            super(Changer.ChangeMode.class);
        }

        public void storeTypes(Changer.ChangeMode mode, Property.PropertyInfo<Handler> propertyInfo, Class<?>[] types) {
            Map map = this.computeIfAbsent(mode, k -> new HashMap());
            map.put(propertyInfo, types);
        }

        public Class<?>[] getTypes(Changer.ChangeMode mode, Property.PropertyInfo<Handler> propertyInfo) {
            Map map = (Map)this.get((Object)mode);
            if (map != null) {
                return (Class[])map.get(propertyInfo);
            }
            return null;
        }
    }
}

