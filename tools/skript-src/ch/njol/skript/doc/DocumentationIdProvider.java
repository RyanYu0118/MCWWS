/*
 * Decompiled with CFR 0.152.
 */
package ch.njol.skript.doc;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.DocumentationId;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.function.Function;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.PropertyRegistry;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.SyntaxInfo;

public class DocumentationIdProvider {
    private static String addCollisionSuffix(String id, int collisionCount) {
        if (collisionCount == 0) {
            return id;
        }
        return id + "-" + (collisionCount + 1);
    }

    private static <T> int calculateCollisionCount(Iterator<? extends T> potentialCollisions, Predicate<T> collisionCriteria, Predicate<T> equalsCriteria) {
        int collisionCount = 0;
        while (potentialCollisions.hasNext()) {
            T potentialCollision = potentialCollisions.next();
            if (!collisionCriteria.test(potentialCollision)) continue;
            if (equalsCriteria.test(potentialCollision)) break;
            ++collisionCount;
        }
        return collisionCount;
    }

    public static <T> String getId(SyntaxElementInfo<? extends T> syntaxInfo) {
        return DocumentationIdProvider.getId(syntaxInfo);
    }

    public static <T> String getId(SyntaxInfo<? extends T> syntaxInfo) {
        Iterator<SyntaxElementInfo<TriggerItem>> syntaxElementIterator;
        Class syntaxClass = syntaxInfo.type();
        if (Effect.class.isAssignableFrom(syntaxClass)) {
            syntaxElementIterator = Skript.getEffects().iterator();
        } else if (Condition.class.isAssignableFrom(syntaxClass)) {
            syntaxElementIterator = Skript.getConditions().iterator();
        } else if (Expression.class.isAssignableFrom(syntaxClass)) {
            syntaxElementIterator = Skript.getExpressions();
        } else if (Section.class.isAssignableFrom(syntaxClass)) {
            syntaxElementIterator = Skript.getSections().iterator();
        } else if (Structure.class.isAssignableFrom(syntaxClass)) {
            syntaxElementIterator = Skript.getStructures().iterator();
        } else {
            throw new IllegalStateException("Unsupported syntax type provided");
        }
        int collisionCount = DocumentationIdProvider.calculateCollisionCount(syntaxElementIterator, elementInfo -> elementInfo.getElementClass() == syntaxClass, elementInfo -> Arrays.equals(elementInfo.getPatterns(), syntaxInfo.patterns().toArray(new String[0])));
        DocumentationId documentationIdAnnotation = syntaxClass.getAnnotation(DocumentationId.class);
        if (documentationIdAnnotation == null) {
            return DocumentationIdProvider.addCollisionSuffix(syntaxClass.getSimpleName(), collisionCount);
        }
        return DocumentationIdProvider.addCollisionSuffix(documentationIdAnnotation.value(), collisionCount);
    }

    public static String getId(Function<?> function) {
        int collisionCount = DocumentationIdProvider.calculateCollisionCount(Functions.getFunctions().iterator(), javaFunction -> function.getName().equals(javaFunction.getName()), javaFunction -> javaFunction == function);
        return DocumentationIdProvider.addCollisionSuffix(function.getName(), collisionCount);
    }

    private static String getClassInfoId(ClassInfo<?> classInfo) {
        return Objects.requireNonNullElse(classInfo.getDocumentationID(), classInfo.getCodeName());
    }

    public static String getId(ClassInfo<?> classInfo) {
        String classInfoId = DocumentationIdProvider.getClassInfoId(classInfo);
        int collisionCount = DocumentationIdProvider.calculateCollisionCount(Classes.getClassInfos().iterator(), otherClassInfo -> classInfoId.equals(DocumentationIdProvider.getClassInfoId(otherClassInfo)), otherClassInfo -> classInfo == otherClassInfo);
        return DocumentationIdProvider.addCollisionSuffix(classInfoId, collisionCount);
    }

    private static String getEventId(BukkitSyntaxInfos.Event<?> eventInfo) {
        return Objects.requireNonNullElse(eventInfo.documentationId(), eventInfo.id());
    }

    public static String getId(BukkitSyntaxInfos.Event<?> eventInfo) {
        String eventId = DocumentationIdProvider.getEventId(eventInfo);
        int collisionCount = DocumentationIdProvider.calculateCollisionCount(Skript.instance().syntaxRegistry().syntaxes(BukkitSyntaxInfos.Event.KEY).iterator(), otherEventInfo -> eventId.equals(DocumentationIdProvider.getEventId(otherEventInfo)), otherEventInfo -> Arrays.equals(otherEventInfo.patterns().toArray(), eventInfo.patterns().toArray()));
        return DocumentationIdProvider.addCollisionSuffix(eventId, collisionCount);
    }

    public static String getId(Property<?> property) {
        String propertyId = property.getDocumentationID();
        int collisionCount = DocumentationIdProvider.calculateCollisionCount(Skript.instance().registry(PropertyRegistry.class).iterator(), otherProperty -> propertyId.equals(otherProperty.getDocumentationID()), otherProperty -> property == otherProperty);
        return DocumentationIdProvider.addCollisionSuffix(propertyId, collisionCount);
    }
}

