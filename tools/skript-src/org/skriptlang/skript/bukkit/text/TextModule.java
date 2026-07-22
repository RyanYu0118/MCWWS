/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 */
package org.skriptlang.skript.bukkit.text;

import ch.njol.skript.registrations.Classes;
import net.kyori.adventure.text.Component;
import org.skriptlang.skript.addon.AddonModule;
import org.skriptlang.skript.addon.HierarchicalAddonModule;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.bukkit.text.TextComponentParser;
import org.skriptlang.skript.bukkit.text.TextComponentUtils;
import org.skriptlang.skript.bukkit.text.elements.effects.EffActionBar;
import org.skriptlang.skript.bukkit.text.elements.effects.EffBroadcast;
import org.skriptlang.skript.bukkit.text.elements.effects.EffMessage;
import org.skriptlang.skript.bukkit.text.elements.effects.EffResetTitle;
import org.skriptlang.skript.bukkit.text.elements.effects.EffSendTitle;
import org.skriptlang.skript.bukkit.text.elements.expressions.ExprColored;
import org.skriptlang.skript.bukkit.text.elements.expressions.ExprRawString;
import org.skriptlang.skript.bukkit.text.elements.expressions.ExprStringColor;
import org.skriptlang.skript.bukkit.text.types.AudienceClassInfo;
import org.skriptlang.skript.bukkit.text.types.TextComponentClassInfo;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.converter.Converters;

public class TextModule
extends HierarchicalAddonModule {
    public TextModule(AddonModule parentModule) {
        super(parentModule);
    }

    @Override
    public void initSelf(SkriptAddon addon) {
        Classes.registerClass(new TextComponentClassInfo(addon));
        Classes.registerClass(new AudienceClassInfo());
        Converters.registerConverter(String.class, Component.class, string -> TextComponentParser.instance().parseSafe(string));
        Converters.registerConverter(Component.class, String.class, component -> TextComponentParser.instance().toLegacyString((Component)component));
        Comparators.registerComparator(Component.class, String.class, (component, string) -> {
            TextComponentParser parser = TextComponentParser.instance();
            String string1 = parser.toString((Component)component);
            String string2 = parser.toString(parser.parseSafe(string));
            return Comparators.compare(string1, string2);
        });
        Comparators.registerComparator(Component.class, Component.class, (component1, component2) -> {
            TextComponentParser parser = TextComponentParser.instance();
            String string1 = parser.toString((Component)component1);
            String string2 = parser.toString((Component)component2);
            return Comparators.compare(string1, string2);
        });
        Arithmetics.registerOperation(Operator.ADDITION, Component.class, Component.class, TextComponentUtils::appendToEnd);
        Arithmetics.registerOperation(Operator.ADDITION, Component.class, String.class, (component, string) -> TextComponentUtils.appendToEnd(component, TextComponentParser.instance().parseSafe(string)), (string, component) -> TextComponentUtils.appendToEnd(TextComponentParser.instance().parseSafe(string), component));
    }

    @Override
    public void loadSelf(SkriptAddon addon) {
        this.register(addon, EffActionBar::register, EffBroadcast::register, EffMessage::register, EffResetTitle::register, EffSendTitle::register, ExprColored::register, ExprRawString::register, ExprStringColor::register);
    }

    @Override
    public String name() {
        return "text";
    }
}

