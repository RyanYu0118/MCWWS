/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.properties.handlers;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.ExprSubnodeValue;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

@ApiStatus.Experimental
public interface TypedValueHandler<Type, ValueType>
extends ExpressionPropertyHandler<Type, ValueType> {
    @Override
    @Nullable
    public ValueType convert(Type var1);

    default public <Converted> Converted convert(Type propertyHolder, ClassInfo<Converted> expected) {
        ValueType value = this.convert(propertyHolder);
        if (value == null) {
            return null;
        }
        return ExprSubnodeValue.convertedValue(value, expected);
    }

    default public ValueType convertChangeValue(Object value) throws UnsupportedOperationException {
        Class typeClass = this.returnType();
        ClassInfo classInfo = Classes.getSuperClassInfo(typeClass);
        if (value == null) {
            return null;
        }
        if (typeClass == String.class) {
            return (ValueType)typeClass.cast(Classes.toString(value, StringMode.MESSAGE));
        }
        if (value instanceof String) {
            String string = (String)value;
            if (classInfo.getParser() != null && classInfo.getParser().canParse(ParseContext.CONFIG)) {
                return (ValueType)classInfo.getParser().parse(string, ParseContext.CONFIG);
            }
        }
        return (ValueType)Converters.convert(value, typeClass);
    }
}

