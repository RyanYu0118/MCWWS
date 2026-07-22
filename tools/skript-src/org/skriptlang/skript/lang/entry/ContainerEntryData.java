/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.lang.entry;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryData;
import org.skriptlang.skript.lang.entry.EntryValidator;

public class ContainerEntryData
extends EntryData<EntryContainer> {
    private final EntryValidator entryValidator;
    @Nullable
    private EntryContainer entryContainer;

    public ContainerEntryData(String key, boolean optional, EntryValidator entryValidator) {
        super(key, null, optional);
        this.entryValidator = entryValidator;
    }

    public ContainerEntryData(String key, boolean optional, EntryValidator.EntryValidatorBuilder validatorBuilder) {
        super(key, null, optional);
        this.entryValidator = validatorBuilder.build();
    }

    public ContainerEntryData(String key, boolean optional, boolean multiple, EntryValidator entryValidator) {
        super(key, null, optional, multiple);
        this.entryValidator = entryValidator;
    }

    public ContainerEntryData(String key, boolean optional, boolean multiple, EntryValidator.EntryValidatorBuilder validatorBuilder) {
        super(key, null, optional, multiple);
        this.entryValidator = validatorBuilder.build();
    }

    public EntryValidator getEntryValidator() {
        return this.entryValidator;
    }

    @Override
    @Nullable
    public EntryContainer getValue(Node node) {
        return this.entryContainer;
    }

    @Override
    public boolean canCreateWith(Node node) {
        if (!(node instanceof SectionNode)) {
            return false;
        }
        SectionNode sectionNode = (SectionNode)node;
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        key = ScriptLoader.replaceOptions(key);
        if (!this.getKey().equalsIgnoreCase(key)) {
            return false;
        }
        this.entryContainer = this.entryValidator.validate(sectionNode);
        return true;
    }
}

