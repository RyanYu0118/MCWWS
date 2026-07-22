/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.structure;

import ch.njol.skript.lang.SyntaxElementInfo;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxInfo;

@Deprecated(since="2.14", forRemoval=true)
public class StructureInfo<E extends Structure>
extends SyntaxElementInfo<E> {
    @Nullable
    public final EntryValidator entryValidator;
    public final boolean simple;
    public final DefaultSyntaxInfos.Structure.NodeType nodeType;

    public StructureInfo(String[] patterns, Class<E> c, String originClassPath) throws IllegalArgumentException {
        this(patterns, c, originClassPath, false);
    }

    public StructureInfo(String[] patterns, Class<E> elementClass, String originClassPath, boolean simple) throws IllegalArgumentException {
        this(patterns, elementClass, originClassPath, null, simple ? DefaultSyntaxInfos.Structure.NodeType.SIMPLE : DefaultSyntaxInfos.Structure.NodeType.SECTION);
    }

    public StructureInfo(String[] patterns, Class<E> elementClass, String originClassPath, @Nullable EntryValidator entryValidator) throws IllegalArgumentException {
        this(patterns, elementClass, originClassPath, entryValidator, DefaultSyntaxInfos.Structure.NodeType.SECTION);
    }

    public StructureInfo(String[] patterns, Class<E> elementClass, String originClassPath, @Nullable EntryValidator entryValidator, DefaultSyntaxInfos.Structure.NodeType nodeType) throws IllegalArgumentException {
        super(patterns, elementClass, originClassPath);
        this.entryValidator = entryValidator;
        this.nodeType = nodeType;
        this.simple = nodeType.canBeSimple();
    }

    @ApiStatus.Internal
    public StructureInfo(DefaultSyntaxInfos.Structure<E> source) {
        super(source);
        this.entryValidator = source.entryValidator();
        this.nodeType = source.nodeType();
        this.simple = source.nodeType().canBeSimple();
    }

    @ApiStatus.Internal
    protected StructureInfo(SyntaxInfo<E> source) {
        super(source);
        this.entryValidator = null;
        this.nodeType = DefaultSyntaxInfos.Structure.NodeType.SIMPLE;
        this.simple = this.nodeType.canBeSimple();
    }
}

