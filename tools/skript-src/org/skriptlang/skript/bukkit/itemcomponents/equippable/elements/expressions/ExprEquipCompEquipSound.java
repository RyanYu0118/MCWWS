/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.item.Equippable
 *  net.kyori.adventure.key.Key
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.Sound
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions;

import ch.njol.skript.bukkitutil.SoundUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Equip Sound")
@Description(value={"The sound to be played when the item is equipped.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="set the equip sound of {_item} to \"entity.experience_orb.pickup\""), @Example(value="set {_component} to the equippable component of {_item}\nset the equip sound of {_component} to \"block.note_block.pling\"\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class ExprEquipCompEquipSound
extends SimplePropertyExpression<EquippableWrapper, String>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprEquipCompEquipSound.infoBuilder(ExprEquipCompEquipSound.class, String.class, "equip sound", "equippablecomponents", true).supplier(ExprEquipCompEquipSound::new)).build());
    }

    @Override
    @Nullable
    public String convert(EquippableWrapper wrapper) {
        return ((Equippable)wrapper.getComponent()).equipSound().toString();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return CollectionUtils.array(String.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        String soundString;
        Sound enumSound = null;
        if (delta != null && (enumSound = SoundUtils.getSound(soundString = (String)delta[0])) == null) {
            this.error("Could not find a sound with the id '" + soundString + "'.");
            return;
        }
        NamespacedKey key = enumSound != null ? Registry.SOUNDS.getKey(enumSound) : null;
        this.getExpr().stream(event).forEach(arg_0 -> ExprEquipCompEquipSound.lambda$change$0((Key)key, arg_0));
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "equip sound";
    }

    private static /* synthetic */ void lambda$change$0(Key key, EquippableWrapper wrapper) {
        wrapper.editBuilder(builder -> builder.equipSound(key));
    }
}

