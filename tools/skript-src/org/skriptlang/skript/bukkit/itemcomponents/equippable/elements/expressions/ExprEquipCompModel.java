/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  org.bukkit.NamespacedKey
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable.elements.expressions;

import ch.njol.skript.bukkitutil.NamespacedUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableExperimentSyntax;
import org.skriptlang.skript.bukkit.itemcomponents.equippable.EquippableWrapper;
import org.skriptlang.skript.registration.DefaultSyntaxInfos;
import org.skriptlang.skript.registration.SyntaxRegistry;

@Name(value="Equippable Component - Model")
@Description(value={"The model of the item when equipped.\nThe model key is represented as a namespaced key.\nA namespaced key can be formatted as 'namespace:id' or 'id'. It can only contain one ':' to separate the namespace and the id. Only alphanumeric characters, periods, underscores, and dashes can be used.\nNOTE: Equippable component elements are experimental. Thus, they are subject to change and may not work as intended.\n"})
@Example.Examples(value={@Example(value="set the equipped model key of {_item} to \"custom_model\""), @Example(value="set {_component} to the equippable component of {_item}\nset the equipped model id of {_component} to \"custom_model\"\n")})
@RequiredPlugins(value={"Minecraft 1.21.2+"})
@Since(value={"2.13"})
public class ExprEquipCompModel
extends SimplePropertyExpression<EquippableWrapper, String>
implements EquippableExperimentSyntax {
    public static void register(SyntaxRegistry registry) {
        registry.register(SyntaxRegistry.EXPRESSION, ((DefaultSyntaxInfos.Expression.Builder)ExprEquipCompModel.infoBuilder(ExprEquipCompModel.class, String.class, "equipped (model|asset) (key|id)", "equippablecomponents", true).supplier(ExprEquipCompModel::new)).build());
    }

    @Override
    @Nullable
    public String convert(EquippableWrapper wrapper) {
        Key key = wrapper.getAssetId();
        return key == null ? null : key.toString();
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
        String string;
        Object object;
        NamespacedKey key = null;
        if (delta != null && (object = delta[0]) instanceof String && (key = NamespacedUtils.checkValidationAndSend(string = (String)object, this)) == null) {
            return;
        }
        NamespacedKey finalKey = key;
        this.getExpr().stream(event).forEach(wrapper -> wrapper.setAssetId((Key)finalKey));
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "equipped model key";
    }
}

