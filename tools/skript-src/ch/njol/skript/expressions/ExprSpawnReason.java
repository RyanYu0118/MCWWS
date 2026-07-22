/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.entity.CreatureSpawnEvent$SpawnReason
 */
package ch.njol.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.EventValueExpression;
import org.bukkit.event.entity.CreatureSpawnEvent;

@Name(value="Spawn Reason")
@Description(value={"The <a href='#spawnreason'>spawn reason</a> in a <a href='#spawn'>spawn</a> event."})
@Example(value="on spawn:\n\tspawn reason is reinforcements or breeding\n\tcancel event\n")
@Since(value={"2.3"})
public class ExprSpawnReason
extends EventValueExpression<CreatureSpawnEvent.SpawnReason> {
    public ExprSpawnReason() {
        super(CreatureSpawnEvent.SpawnReason.class);
    }

    static {
        ExprSpawnReason.register(ExprSpawnReason.class, CreatureSpawnEvent.SpawnReason.class, "spawn[ing] reason");
    }
}

