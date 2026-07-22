/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Unmodifiable
 */
package ch.njol.skript.lang;

import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.ExpressionInfo;
import ch.njol.skript.lang.SkriptEventInfo;
import ch.njol.skript.lang.SyntaxElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.SequencedCollection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.skriptlang.skript.bukkit.registration.BukkitSyntaxInfos;
import org.skriptlang.skript.docs.Origin;
import org.skriptlang.skript.lang.structure.StructureInfo;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;
import org.skriptlang.skript.util.Priority;

@Deprecated(since="2.14", forRemoval=true)
public class SyntaxElementInfo<E extends SyntaxElement>
implements SyntaxInfo<E> {
    @Nullable
    private final SyntaxInfo<E> source;
    public final Class<E> elementClass;
    public final String[] patterns;
    public final String originClassPath;

    public SyntaxElementInfo(String[] patterns, Class<E> elementClass, String originClassPath) throws IllegalArgumentException {
        if (Modifier.isAbstract(elementClass.getModifiers())) {
            throw new SkriptAPIException("Class " + elementClass.getName() + " is abstract");
        }
        this.source = null;
        this.patterns = patterns;
        this.elementClass = elementClass;
        this.originClassPath = originClassPath;
        try {
            elementClass.getConstructor(new Class[0]);
        }
        catch (NoSuchMethodException e) {
            throw new Error(String.valueOf(elementClass) + " does not have a public nullary constructor", e);
        }
        catch (SecurityException e) {
            throw new IllegalStateException("Skript cannot run properly because a security manager is blocking it!");
        }
    }

    @ApiStatus.Internal
    protected SyntaxElementInfo(SyntaxInfo<E> source) throws IllegalArgumentException {
        this.source = source;
        this.patterns = source.patterns().toArray(new String[0]);
        this.elementClass = source.type();
        this.originClassPath = source.origin().name();
    }

    public Class<E> getElementClass() {
        return this.elementClass;
    }

    public String[] getPatterns() {
        return Arrays.copyOf(this.patterns, this.patterns.length);
    }

    public String getOriginClassPath() {
        return this.originClassPath;
    }

    @ApiStatus.Internal
    @Contract(value="_ -> new")
    public static <I extends SyntaxElementInfo<E>, E extends SyntaxElement> I fromModern(SyntaxInfo<? extends E> info) {
        if (info instanceof SyntaxElementInfo) {
            SyntaxElementInfo oldInfo = (SyntaxElementInfo)info;
            return (I)oldInfo;
        }
        if (info instanceof BukkitSyntaxInfos.Event) {
            BukkitSyntaxInfos.Event event = (BukkitSyntaxInfos.Event)info;
            return (I)new SkriptEventInfo(event);
        }
        if (info instanceof DefaultSyntaxInfos.Structure) {
            DefaultSyntaxInfos.Structure structure = (DefaultSyntaxInfos.Structure)info;
            return (I)new StructureInfo(structure);
        }
        if (info instanceof DefaultSyntaxInfos.Expression) {
            DefaultSyntaxInfos.Expression expression = (DefaultSyntaxInfos.Expression)info;
            return (I)new ExpressionInfo(expression);
        }
        return (I)new SyntaxElementInfo<E>(info);
    }

    @Override
    @ApiStatus.Internal
    public SyntaxInfo.Builder<? extends SyntaxInfo.Builder<?, E>, E> toBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    @ApiStatus.Internal
    public Origin origin() {
        if (this.source != null) {
            return this.source.origin();
        }
        return Origin.UNKNOWN;
    }

    @Override
    @ApiStatus.Internal
    public Class<E> type() {
        return this.getElementClass();
    }

    @Override
    @ApiStatus.Internal
    public E instance() {
        if (this.source != null) {
            return this.source.instance();
        }
        try {
            return (E)((SyntaxElement)this.type().getDeclaredConstructor(new Class[0]).newInstance(new Object[0]));
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @ApiStatus.Internal
    public @Unmodifiable SequencedCollection<String> patterns() {
        if (this.source != null) {
            return this.source.patterns();
        }
        return List.of(this.getPatterns());
    }

    @Override
    @ApiStatus.Internal
    public Priority priority() {
        if (this.source != null) {
            this.source.priority();
        }
        return SyntaxInfo.COMBINED;
    }
}

