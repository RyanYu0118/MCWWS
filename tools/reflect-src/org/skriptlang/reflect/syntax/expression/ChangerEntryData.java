/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.ScriptLoader
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.config.Node
 *  ch.njol.skript.config.SectionNode
 *  ch.njol.util.NonNullPair
 *  ch.njol.util.coll.CollectionUtils
 *  org.jetbrains.annotations.Nullable
 *  org.skriptlang.skript.lang.entry.EntryData
 */
package org.skriptlang.reflect.syntax.expression;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.util.NonNullPair;
import ch.njol.util.coll.CollectionUtils;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryData;

public class ChangerEntryData
extends EntryData<NonNullPair<SectionNode, Class<?>[]>> {
    public ChangerEntryData(String key, boolean optional) {
        super(key, null, optional);
    }

    @Nullable
    public NonNullPair<SectionNode, Class<?>[]> getValue(Node node) {
        String key = node.getKey();
        assert (key != null);
        key = ScriptLoader.replaceOptions((String)node.getKey());
        String rawTypes = key.substring(this.getKey().length()).trim();
        if (rawTypes.isEmpty()) {
            return new NonNullPair((Object)((SectionNode)node), (Object)new Class[0]);
        }
        Class[] acceptedClasses = (Class[])Arrays.stream(rawTypes.split(",")).map(String::trim).map(SkriptUtil::getUserClassInfoAndPlural).map(meta -> {
            ClassInfo classInfo = (ClassInfo)meta.getFirst();
            boolean plural = (Boolean)meta.getSecond();
            if (plural) {
                return CollectionUtils.arrayType((Class)classInfo.getC());
            }
            return classInfo.getC();
        }).toArray(Class[]::new);
        return new NonNullPair((Object)((SectionNode)node), (Object)acceptedClasses);
    }

    public boolean canCreateWith(Node node) {
        if (!(node instanceof SectionNode)) {
            return false;
        }
        String key = node.getKey();
        if (key == null) {
            return false;
        }
        key = ScriptLoader.replaceOptions((String)key);
        return key.startsWith(this.getKey());
    }
}

