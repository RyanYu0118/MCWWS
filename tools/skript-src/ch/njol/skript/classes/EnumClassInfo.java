/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.classes;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumParser;
import ch.njol.skript.classes.EnumSerializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.DefaultExpression;
import ch.njol.util.coll.iterator.ArrayIterator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

public class EnumClassInfo<T extends Enum<T>>
extends ClassInfo<T> {
    public EnumClassInfo(Class<T> enumClass, String codeName, String languageNode) {
        this(enumClass, codeName, languageNode, new EventValueExpression<T>(enumClass), true);
    }

    public EnumClassInfo(Class<T> enumClass, String codeName, String languageNode, boolean registerComparator) {
        this(enumClass, codeName, languageNode, new EventValueExpression<T>(enumClass), registerComparator);
    }

    public EnumClassInfo(Class<T> enumClass, String codeName, String languageNode, DefaultExpression<T> defaultExpression) {
        this(enumClass, codeName, languageNode, defaultExpression, true);
    }

    public EnumClassInfo(Class<T> enumClass, String codeName, String languageNode, DefaultExpression<T> defaultExpression, boolean registerComparator) {
        super(enumClass, codeName);
        EnumParser<T> enumParser = new EnumParser<T>(enumClass, languageNode);
        this.usage(enumParser.getCombinedPatterns()).serializer(new EnumSerializer<T>(enumClass)).defaultExpression(defaultExpression).supplier(() -> new ArrayIterator<Enum>((Enum[])enumClass.getEnumConstants())).parser(enumParser);
        if (registerComparator) {
            Comparators.registerComparator(enumClass, enumClass, (o1, o2) -> Relation.get(o1.ordinal() - o2.ordinal()));
        }
    }
}

