/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.registrations.Classes
 */
package org.skriptlang.reflect.syntax.event.elements;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import java.util.List;
import org.skriptlang.reflect.syntax.event.EventSyntaxInfo;
import org.skriptlang.reflect.syntax.event.elements.StructCustomEvent;

public class CustomEventUtils {
    public static boolean hasEventValue(EventSyntaxInfo which, ClassInfo<?> classInfo) {
        List<ClassInfo<?>> eventValueClassInfoList = StructCustomEvent.eventValueTypes.get(which);
        if (eventValueClassInfoList == null) {
            return false;
        }
        Class classInfoClass = classInfo.getC();
        for (ClassInfo<?> loopedClassInfo : eventValueClassInfoList) {
            if (!classInfoClass.isAssignableFrom(loopedClassInfo.getC())) continue;
            return true;
        }
        return false;
    }

    public static String getName(ClassInfo<?> classInfo) {
        return Classes.getSuperClassInfo((Class)classInfo.getC()).getName().toString();
    }

    public static String getName(EventSyntaxInfo which) {
        if (which == null) {
            return null;
        }
        return StructCustomEvent.nameValues.get(which);
    }
}

