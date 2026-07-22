/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Keyed
 *  org.bukkit.Registry
 */
package ch.njol.skript.classes.registry;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.registry.RegistryParser;
import ch.njol.skript.classes.registry.RegistrySerializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.DefaultExpression;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;

public class RegistryClassInfo<R extends Keyed>
extends ClassInfo<R> {
    public RegistryClassInfo(Class<R> registryClass, Registry<R> registry, String codeName, String languageNode) {
        this(registryClass, registry, codeName, languageNode, new EventValueExpression<R>(registryClass), true);
    }

    public RegistryClassInfo(Class<R> registryClass, Registry<R> registry, String codeName, String languageNode, boolean registerComparator) {
        this(registryClass, registry, codeName, languageNode, new EventValueExpression<R>(registryClass), registerComparator);
    }

    public RegistryClassInfo(Class<R> registryClass, Registry<R> registry, String codeName, String languageNode, DefaultExpression<R> defaultExpression) {
        this(registryClass, registry, codeName, languageNode, defaultExpression, true);
    }

    public RegistryClassInfo(Class<R> registryClass, Registry<R> registry, String codeName, String languageNode, DefaultExpression<R> defaultExpression, boolean registerComparator) {
        super(registryClass, codeName);
        RegistryParser<R> registryParser = new RegistryParser<R>(registry, languageNode);
        this.usage(registryParser.getCombinedPatterns()).supplier(() -> registry.iterator()).serializer(new RegistrySerializer<R>(registry)).defaultExpression(defaultExpression).parser(registryParser);
        if (registerComparator) {
            Comparators.registerComparator(registryClass, registryClass, (o1, o2) -> Relation.get(o1.getKey().equals((Object)o2.getKey())));
        }
    }
}

