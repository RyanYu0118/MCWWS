/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.UnknownNullability
 */
package ch.njol.skript.lang.util.common;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.ExprSubnodeValue;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.common.AnyProvider;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.StringMode;
import org.jetbrains.annotations.UnknownNullability;
import org.skriptlang.skript.lang.converter.Converters;

@Deprecated(since="2.13", forRemoval=true)
public interface AnyValued<Type>
extends AnyProvider {
    public @UnknownNullability Type value();

    default public <Converted> Converted convertedValue(ClassInfo<Converted> expected) {
        Type value = this.value();
        if (value == null) {
            return null;
        }
        return ExprSubnodeValue.convertedValue(value, expected);
    }

    default public boolean supportsValueChange() {
        return false;
    }

    default public void changeValue(Type value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Class<Type> valueType();

    default public void resetValue() throws UnsupportedOperationException {
        this.changeValueSafely(null);
    }

    /*
     * Enabled aggressive block sorting
     */
    default public void changeValueSafely(Object value) throws UnsupportedOperationException {
        Class<Type> typeClass = this.valueType();
        ClassInfo<Type> classInfo = Classes.getSuperClassInfo(typeClass);
        if (value == null) {
            this.changeValue(null);
            return;
        }
        if (typeClass == String.class) {
            this.changeValue(typeClass.cast(Classes.toString(value, StringMode.MESSAGE)));
            return;
        }
        if (value instanceof String) {
            String string = (String)value;
            if (classInfo.getParser() != null && classInfo.getParser().canParse(ParseContext.CONFIG)) {
                Type convert = classInfo.getParser().parse(string, ParseContext.CONFIG);
                this.changeValue(convert);
                return;
            }
        }
        Type convert = Converters.convert(value, typeClass);
        this.changeValue(convert);
    }
}

