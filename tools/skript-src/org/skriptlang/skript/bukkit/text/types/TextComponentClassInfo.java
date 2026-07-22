/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.json.JSONComponentSerializer
 *  org.jetbrains.annotations.ApiStatus$Internal
 */
package org.skriptlang.skript.bukkit.text.types;

import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.util.StringUtils;
import ch.njol.yggdrasil.Fields;
import java.io.StreamCorruptedException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.properties.Property;
import org.skriptlang.skript.lang.properties.handlers.ContainsHandler;

@ApiStatus.Internal
public final class TextComponentClassInfo
extends ClassInfo<Component> {
    public TextComponentClassInfo(SkriptAddon addon) {
        super(Component.class, "textcomponent");
        this.user("text ?components?").name("Text Component").description("Text components are used to represent how text is displayed in Minecraft.", "This includes colors, decorations, and more.").examples("\"<red><bold>This text is red and bold!\"").since("2.15").parser(new TextComponentParser()).serializer(new TextComponentSerializer()).property(Property.CONTAINS, "Components can contain other components.", addon, new ContainsHandler<Component, Component>(this){

            @Override
            public boolean contains(Component container, Component element) {
                org.skriptlang.skript.bukkit.text.TextComponentParser parser = org.skriptlang.skript.bukkit.text.TextComponentParser.instance();
                return StringUtils.contains(parser.toString(container), parser.toString(element), SkriptConfig.caseSensitive.value());
            }

            @Override
            public Class<? extends Component>[] elementTypes() {
                return new Class[]{Component.class};
            }
        });
    }

    private static final class TextComponentParser
    extends Parser<Component> {
        private TextComponentParser() {
        }

        @Override
        public boolean canParse(ParseContext context) {
            return false;
        }

        @Override
        public String toString(Component component, int flags) {
            return org.skriptlang.skript.bukkit.text.TextComponentParser.instance().toString(component);
        }

        @Override
        public String toVariableNameString(Component component) {
            return "textcomponent:" + component.hashCode();
        }
    }

    static final class TextComponentSerializer
    extends Serializer<Component> {
        TextComponentSerializer() {
        }

        @Override
        public Fields serialize(Component component) {
            return Fields.singletonObject("json", JSONComponentSerializer.json().serialize(component));
        }

        @Override
        protected Component deserialize(Fields fields) throws StreamCorruptedException {
            String json = fields.getObject("json", String.class);
            if (json == null) {
                throw new StreamCorruptedException();
            }
            return JSONComponentSerializer.json().deserialize((Object)json);
        }

        @Override
        public boolean mustSyncDeserialization() {
            return false;
        }

        @Override
        protected boolean canBeInstantiated() {
            return false;
        }
    }
}

