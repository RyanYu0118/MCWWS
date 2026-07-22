/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.properties;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import java.util.ArrayList;
import java.util.Set;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyMap;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Experimental
public interface PropertyBaseSyntax<Handler extends PropertyHandler<?>> {
    @Nullable
    default public String getBadTypesErrorMessage(@NotNull Expression<?> expr) {
        expr = LiteralUtils.defendExpression(expr);
        ArrayList<ClassInfo<Object>> invalidTypes = new ArrayList<ClassInfo<Object>>();
        for (Class<Object> type : expr.possibleReturnTypes()) {
            ClassInfo<Object> info = Classes.getSuperClassInfo(type);
            if (info.hasProperty(this.getProperty())) continue;
            invalidTypes.add(info);
        }
        return "The expression '" + String.valueOf(expr) + "' returns the following types that do not have the " + this.getPropertyName() + " property: " + Classes.toString(invalidTypes.toArray(), true);
    }

    @NotNull
    public Property<Handler> getProperty();

    default public String getPropertyName() {
        return this.getProperty().name();
    }

    @Nullable
    public static Expression<?> asProperty(Property<?> property, Expression<?> expr) {
        if (expr == null) {
            return null;
        }
        Set<ClassInfo<?>> classInfos = Classes.getClassInfosByProperty(property);
        Class[] classes = (Class[])classInfos.stream().map(ClassInfo::getC).toArray(Class[]::new);
        if (classes.length == 0) {
            return null;
        }
        return LiteralUtils.defendExpression(expr).getConvertedExpression(classes);
    }

    public static <Handler extends PropertyHandler<?>> PropertyMap<Handler> getPossiblePropertyInfos(Property<Handler> property, Expression<?> expr) {
        PropertyMap propertyInfos = new PropertyMap();
        Set<ClassInfo<?>> classInfos = Classes.getClassInfosByProperty(property);
        for (Class<?> returnType : expr.possibleReturnTypes()) {
            ClassInfo<?> closestInfo = null;
            for (ClassInfo<?> propertiedClassInfo : classInfos) {
                if (propertiedClassInfo.getC() == returnType) {
                    closestInfo = propertiedClassInfo;
                    break;
                }
                if (!propertiedClassInfo.getC().isAssignableFrom(returnType) || closestInfo != null && !closestInfo.getC().isAssignableFrom(propertiedClassInfo.getC())) continue;
                closestInfo = propertiedClassInfo;
            }
            if (closestInfo == null) continue;
            Property.PropertyInfo<Object> propertyInfo = closestInfo.getPropertyInfo(property);
            if (propertyInfo != null) {
                PropertyHandler<?> clonedHandler = propertyInfo.handler().newInstance();
                propertyInfo = clonedHandler.init(expr, expr.getParser()) ? new Property.PropertyInfo(propertyInfo.property(), clonedHandler) : null;
            }
            ClassInfo<?> classInfo = Classes.getSuperClassInfo(returnType);
            propertyInfos.put(classInfo.getC(), propertyInfo);
            propertyInfos.put(closestInfo.getC(), propertyInfo);
        }
        return propertyInfos;
    }
}

