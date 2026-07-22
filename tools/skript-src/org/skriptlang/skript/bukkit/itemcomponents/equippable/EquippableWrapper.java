/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.datacomponent.DataComponentType$Valued
 *  io.papermc.paper.datacomponent.DataComponentTypes
 *  io.papermc.paper.datacomponent.item.Equippable
 *  io.papermc.paper.datacomponent.item.Equippable$Builder
 *  io.papermc.paper.registry.RegistryKey
 *  io.papermc.paper.registry.set.RegistryKeySet
 *  net.kyori.adventure.key.Key
 *  org.bukkit.Registry
 *  org.bukkit.entity.EntityType
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package org.skriptlang.skript.bukkit.itemcomponents.equippable;

import ch.njol.skript.Skript;
import ch.njol.skript.util.ItemSource;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.set.RegistryKeySet;
import java.lang.reflect.Method;
import java.util.Collection;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.itemcomponents.ComponentUtils;
import org.skriptlang.skript.bukkit.itemcomponents.ComponentWrapper;

public class EquippableWrapper
extends ComponentWrapper<Equippable, Equippable.Builder> {
    private static final boolean HAS_MODEL_METHOD = Skript.methodExists(Equippable.class, "model", new Class[0]);
    @Nullable
    private static final Method COMPONENT_MODEL_METHOD;
    @Nullable
    private static final Method BUILDER_MODEL_METHOD;
    public static final boolean HAS_EQUIP_ON_INTERACT;
    public static final boolean HAS_CAN_BE_SHEARED;
    public static final boolean HAS_SHEAR_SOUND;

    public EquippableWrapper(ItemStack itemStack) {
        super(itemStack);
    }

    public EquippableWrapper(ItemSource<?> itemSource) {
        super(itemSource);
    }

    public EquippableWrapper(Equippable component) {
        super(component);
    }

    public EquippableWrapper(Equippable.Builder builder) {
        super(builder);
    }

    @Override
    public DataComponentType.Valued<Equippable> getDataComponentType() {
        return DataComponentTypes.EQUIPPABLE;
    }

    @Override
    protected Equippable getComponent(ItemStack itemStack) {
        Equippable equippable = (Equippable)itemStack.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            return equippable;
        }
        return (Equippable)Equippable.equippable((EquipmentSlot)EquipmentSlot.HEAD).build();
    }

    @Override
    protected Equippable.Builder getBuilder(ItemStack itemStack) {
        Equippable equippable = (Equippable)itemStack.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            return (Equippable.Builder)equippable.toBuilder();
        }
        return Equippable.equippable((EquipmentSlot)EquipmentSlot.HEAD);
    }

    @Override
    protected void setComponent(ItemStack itemStack, Equippable component) {
        itemStack.setData(DataComponentTypes.EQUIPPABLE, (Object)component);
    }

    @Override
    protected Equippable.Builder getBuilder(Equippable component) {
        return (Equippable.Builder)component.toBuilder();
    }

    @Override
    public EquippableWrapper clone() {
        EquippableWrapper clone = this.newWrapper();
        Equippable base = (Equippable)this.getComponent();
        clone.applyComponent(this.clone(base.slot()));
        return clone;
    }

    public Equippable clone(EquipmentSlot slot) {
        Equippable base = (Equippable)this.getComponent();
        Equippable.Builder builder = Equippable.equippable((EquipmentSlot)slot).allowedEntities(base.allowedEntities()).cameraOverlay(base.cameraOverlay()).damageOnHurt(base.damageOnHurt()).dispensable(base.dispensable()).equipSound(base.equipSound()).swappable(base.swappable());
        EquippableWrapper.setAssetId(builder, this.getAssetId());
        if (HAS_EQUIP_ON_INTERACT) {
            builder.equipOnInteract(base.equipOnInteract());
        }
        if (HAS_CAN_BE_SHEARED) {
            builder.canBeSheared(base.canBeSheared());
        }
        if (HAS_SHEAR_SOUND) {
            builder.shearSound(base.shearSound());
        }
        return (Equippable)builder.build();
    }

    @Override
    public Equippable newComponent() {
        return (Equippable)this.newBuilder().build();
    }

    @Override
    public Equippable.Builder newBuilder() {
        return Equippable.equippable((EquipmentSlot)EquipmentSlot.HEAD);
    }

    public EquippableWrapper newWrapper() {
        return EquippableWrapper.newInstance();
    }

    public Collection<EntityType> getAllowedEntities() {
        return EquippableWrapper.getAllowedEntities((Equippable)this.getComponent());
    }

    public void setAssetId(Key key) {
        Equippable.Builder builder = (Equippable.Builder)this.getBuilder();
        EquippableWrapper.setAssetId(builder, key);
        this.applyBuilder(builder);
    }

    public Key getAssetId() {
        return EquippableWrapper.getAssetId((Equippable)this.getComponent());
    }

    public static EquippableWrapper newInstance() {
        return new EquippableWrapper(Equippable.equippable((EquipmentSlot)EquipmentSlot.HEAD));
    }

    public static Key getAssetId(Equippable component) {
        if (HAS_MODEL_METHOD) {
            assert (COMPONENT_MODEL_METHOD != null);
            try {
                return (Key)COMPONENT_MODEL_METHOD.invoke((Object)component, new Object[0]);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return component.assetId();
    }

    public static Equippable.Builder setAssetId(Equippable.Builder builder, Key key) {
        if (HAS_MODEL_METHOD) {
            assert (BUILDER_MODEL_METHOD != null);
            try {
                BUILDER_MODEL_METHOD.invoke((Object)builder, key);
            }
            catch (Exception exception) {}
        } else {
            builder.assetId(key);
        }
        return builder;
    }

    public static Collection<EntityType> getAllowedEntities(Equippable component) {
        return ComponentUtils.registryKeySetToCollection(component.allowedEntities(), Registry.ENTITY_TYPE);
    }

    public static RegistryKeySet<EntityType> convertAllowedEntities(Collection<EntityType> entityTypes) {
        return ComponentUtils.collectionToRegistryKeySet(entityTypes, RegistryKey.ENTITY_TYPE);
    }

    static {
        HAS_EQUIP_ON_INTERACT = Skript.methodExists(Equippable.class, "equipOnInteract", new Class[0]);
        HAS_CAN_BE_SHEARED = Skript.methodExists(Equippable.class, "canBeSheared", new Class[0]);
        HAS_SHEAR_SOUND = Skript.methodExists(Equippable.class, "shearSound", new Class[0]);
        Method componentModelMethod = null;
        Method builderModelMethod = null;
        if (HAS_MODEL_METHOD) {
            try {
                componentModelMethod = Equippable.class.getDeclaredMethod("model", new Class[0]);
                builderModelMethod = Equippable.Builder.class.getDeclaredMethod("model", Key.class);
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
        }
        COMPONENT_MODEL_METHOD = componentModelMethod;
        BUILDER_MODEL_METHOD = builderModelMethod;
    }
}

