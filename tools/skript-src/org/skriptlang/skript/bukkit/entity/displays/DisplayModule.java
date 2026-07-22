/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay$ItemDisplayTransform
 *  org.bukkit.entity.TextDisplay$TextAlignment
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 */
package org.skriptlang.skript.bukkit.entity.displays;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.classes.data.DefaultChangers;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.entity.displays.DisplayData;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayBillboard;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayBrightness;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayGlowOverride;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayHeightWidth;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayInterpolation;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayShadow;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayTeleportDuration;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayTransformationRotation;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayTransformationScaleTranslation;
import org.skriptlang.skript.bukkit.entity.displays.elements.expressions.ExprDisplayViewRange;
import org.skriptlang.skript.bukkit.entity.displays.item.elements.expressions.ExprItemDisplayTransform;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.conditions.CondTextDisplayHasDropShadow;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.conditions.CondTextDisplaySeeThroughBlocks;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.effects.EffTextDisplayDropShadow;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.effects.EffTextDisplaySeeThroughBlocks;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.expressions.ExprTextDisplayAlignment;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.expressions.ExprTextDisplayLineWidth;
import org.skriptlang.skript.bukkit.entity.displays.text.elements.expressions.ExprTextDisplayOpacity;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;

public class DisplayModule
extends HierarchicalAddonModule {
    public DisplayModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    protected void initSelf(SkriptAddon addon) {
        Classes.registerClass(new ClassInfo<Display>(Display.class, "display").user("displays?").name("Display Entity").description("A text, block or item display entity.").since("2.10").defaultExpression(new EventValueExpression<Display>(Display.class)).changer(DefaultChangers.nonLivingEntityChanger).property(Property.SCALE, "The scale multipliers to use for a displays. The x, y, and z scales of the display will be multiplied by the respective components of the vector.", Skript.instance(), new ExpressionPropertyHandler<Display, Vector>(this){

            @Override
            @NotNull
            public Vector convert(Display propertyHolder) {
                return Vector.fromJOML((Vector3f)propertyHolder.getTransformation().getScale());
            }

            @Override
            public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
                return switch (mode) {
                    case Changer.ChangeMode.SET, Changer.ChangeMode.RESET -> CollectionUtils.array(Vector.class);
                    default -> null;
                };
            }

            @Override
            public void change(Display propertyHolder, Object @Nullable [] delta, Changer.ChangeMode mode) {
                Vector3f vector = null;
                if (mode == Changer.ChangeMode.RESET) {
                    vector = new Vector3f(1.0f, 1.0f, 1.0f);
                }
                if (delta != null) {
                    vector = ((Vector)delta[0]).toVector3f();
                }
                if (vector == null || !vector.isFinite()) {
                    return;
                }
                Transformation transformation = propertyHolder.getTransformation();
                Transformation change = new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), vector, transformation.getRightRotation());
                propertyHolder.setTransformation(change);
            }

            @Override
            @NotNull
            public Class<Vector> returnType() {
                return Vector.class;
            }
        }));
        Classes.registerClass(new EnumClassInfo<Display.Billboard>(Display.Billboard.class, "billboard", "billboards").user("billboards?").name("Display Billboard").description("Represents the billboard setting of a display.").since("2.10"));
        Classes.registerClass(new EnumClassInfo<TextDisplay.TextAlignment>(TextDisplay.TextAlignment.class, "textalignment", "text alignments").user("text ?alignments?").name("Display Text Alignment").description("Represents the text alignment setting of a text display.").since("2.10"));
        Classes.registerClass(new EnumClassInfo<ItemDisplay.ItemDisplayTransform>(ItemDisplay.ItemDisplayTransform.class, "itemdisplaytransform", "item display transforms").user("item ?display ?transforms?").name("Item Display Transforms").description("Represents the transform setting of an item display.").since("2.10"));
        Converters.registerConverter(Entity.class, Display.class, entity -> {
            Display display;
            return entity instanceof Display ? (display = (Display)entity) : null;
        }, 2);
    }

    @Override
    protected void loadSelf(SkriptAddon addon) {
        this.register(addon, DisplayData::register, ExprDisplayBillboard::register, ExprDisplayBrightness::register, ExprDisplayGlowOverride::register, ExprDisplayHeightWidth::register, ExprDisplayInterpolation::register, ExprDisplayShadow::register, ExprDisplayTeleportDuration::register, ExprDisplayTransformationRotation::register, ExprDisplayTransformationScaleTranslation::register, ExprDisplayViewRange::register, ExprItemDisplayTransform::register, CondTextDisplayHasDropShadow::register, CondTextDisplaySeeThroughBlocks::register, EffTextDisplayDropShadow::register, EffTextDisplaySeeThroughBlocks::register, ExprTextDisplayAlignment::register, ExprTextDisplayLineWidth::register, ExprTextDisplayOpacity::register);
    }

    @Override
    public String name() {
        return "displays";
    }
}

