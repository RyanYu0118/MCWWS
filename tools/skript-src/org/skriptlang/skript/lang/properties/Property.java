/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.ApiStatus$Experimental
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 */
package org.skriptlang.skript.lang.properties;

import ch.njol.skript.Skript;
import java.util.Locale;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.addon.SkriptAddon;
import org.skriptlang.skript.lang.properties.PropertyRegistry;
import org.skriptlang.skript.lang.properties.handlers.ContainsHandler;
import org.skriptlang.skript.lang.properties.handlers.TypedValueHandler;
import org.skriptlang.skript.lang.properties.handlers.WXYZHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ConditionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.ExpressionPropertyHandler;
import org.skriptlang.skript.lang.properties.handlers.base.PropertyHandler;

@ApiStatus.Experimental
public record Property<Handler extends PropertyHandler<?>>(String name, String description, String[] since, SkriptAddon provider, @NotNull Class<? extends Handler> handler) {
    private static final PropertyRegistry PROPERTY_REGISTRY = Skript.getAddonInstance().registry(PropertyRegistry.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> NAME = Property.of("name", "A name, such as a script's name or a player's account name.", "2.13", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> DISPLAY_NAME = Property.of("display name", "A more prominently displayed name, such as a player's display name or an entity's custom name. Often more easily changed than the regular name.", "2.13", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);
    public static final Property<ContainsHandler<?, ?>> CONTAINS = Property.of("contains", "Something that can contain other things, such as an inventory or a string.", "2.13", (SkriptAddon)Skript.instance(), ContainsHandler.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> AMOUNT = Property.of("amount", "The amount of something, say the number of items in a stack or in a queue.", "2.13", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> SIZE = Property.of("size", "The size of something, say the number of elements in a queue.", "2.13", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> SCALE = Property.of("scale", "The scale of something, say the x/y/z scales of a display entity.", "2.14", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> NUMBER = Property.of("number", "The number of something, say the number of elements in a queue.", "2.13", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);
    public static final Property<ConditionPropertyHandler<?>> IS_EMPTY = Property.of("empty", "Whether something is empty or not.", "2.13", (SkriptAddon)Skript.instance(), ConditionPropertyHandler.class);
    public static final Property<TypedValueHandler<?, ?>> TYPED_VALUE = Property.of("typed value", "A value of a specific type, e.g. 'string value of x'.", "2.13", (SkriptAddon)Skript.instance(), TypedValueHandler.class);
    public static final Property<WXYZHandler<?, ?>> WXYZ = Property.of("wxyz component", "The W, X, Y, or Z components of something, e.g. the x coordinate of a location or vector.", "2.14", (SkriptAddon)Skript.instance(), WXYZHandler.class);
    public static final Property<ExpressionPropertyHandler<?, ?>> SPEED = Property.of("speed", "The speed at which something is moving.", "2.14", (SkriptAddon)Skript.instance(), ExpressionPropertyHandler.class);

    public Property(String name, String description, String[] since, SkriptAddon provider, @NotNull Class<? extends Handler> handler) {
        this.name = name.toLowerCase(Locale.ENGLISH);
        this.description = description;
        this.since = since;
        this.provider = provider;
        this.handler = handler;
    }

    public String getDocumentationID() {
        return this.name.replace(' ', '-').toLowerCase(Locale.ENGLISH);
    }

    private void register() {
        PROPERTY_REGISTRY.register(this);
    }

    @Contract(value="_, _, _, _, _ -> new")
    @NotNull
    public static <HandlerClass extends PropertyHandler<?>, Handler extends HandlerClass> Property<Handler> of(@NotNull String name, @NotNull String description, @NotNull String since, @NotNull SkriptAddon provider, @NotNull Class<HandlerClass> handler) {
        return new Property<HandlerClass>(name, description, new String[]{since}, provider, handler);
    }

    @Contract(value="_, _, _, _, _ -> new")
    @NotNull
    public static <HandlerClass extends PropertyHandler<?>, Handler extends HandlerClass> Property<Handler> of(@NotNull String name, @NotNull String description, @NotNull @NotNull String @NotNull [] since, @NotNull SkriptAddon provider, @NotNull Class<HandlerClass> handler) {
        return new Property<HandlerClass>(name, description, since, provider, handler);
    }

    public static void registerDefaultProperties() {
        NAME.register();
        DISPLAY_NAME.register();
        CONTAINS.register();
        AMOUNT.register();
        SIZE.register();
        NUMBER.register();
        IS_EMPTY.register();
        TYPED_VALUE.register();
        SCALE.register();
        SPEED.register();
    }

    public record PropertyInfo<Handler extends PropertyHandler<?>>(Property<Handler> property, Handler handler) {
    }
}

