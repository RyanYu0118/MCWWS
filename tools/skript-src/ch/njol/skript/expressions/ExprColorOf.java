/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.DyeColor
 *  org.bukkit.FireworkEffect
 *  org.bukkit.block.Banner
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.entity.Display
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.TextDisplay
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.material.Colorable
 *  org.bukkit.material.MaterialData
 *  org.bukkit.potion.PotionEffectType
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.skript.util.SkriptColor;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.bukkit.entity.displays.DisplayData;

@Name(value="Color of")
@Description(value={"The <a href='#color'>color</a> of an item, entity, block, firework effect, or text display.", "This can also be used to color chat messages with \"&lt;%color of ...%&gt;this text is colored!\".", "Do note that firework effects support setting, adding, removing, resetting, and deleting; text displays support setting and resetting; and items, entities, and blocks only support setting, and only for very few items/blocks."})
@Example(value="on click on wool:\n\tif event-block is tagged with minecraft tag \"wool\":\n\t\tmessage \"This wool block is <%color of block%>%color of block%<reset>!\"\n\t\tset the color of the block to black\n")
@Since(value={"1.2, 2.10 (displays)"})
public class ExprColorOf
extends PropertyExpression<Object, Color> {
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        return true;
    }

    protected Color[] get(Event event, Object[] source) {
        if (source instanceof FireworkEffect[]) {
            ArrayList colors = new ArrayList();
            for (FireworkEffect effect : (FireworkEffect[])source) {
                effect.getColors().stream().map(ColorRGB::fromBukkitColor).forEach(colors::add);
            }
            return colors.toArray(new Color[0]);
        }
        return this.get(source, object -> {
            if (object instanceof Display) {
                if (!(object instanceof TextDisplay)) {
                    return null;
                }
                TextDisplay display = (TextDisplay)object;
                if (display.isDefaultBackground()) {
                    return ColorRGB.fromBukkitColor(DisplayData.DEFAULT_BACKGROUND_COLOR);
                }
                org.bukkit.Color bukkitColor = display.getBackgroundColor();
                if (bukkitColor == null) {
                    return null;
                }
                return ColorRGB.fromBukkitColor(bukkitColor);
            }
            return this.getColor(object);
        });
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        Expression expression = this.getExpr();
        if (expression.canReturn(FireworkEffect.class)) {
            return CollectionUtils.array(Color[].class);
        }
        if ((mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.SET) && expression.canReturn(Display.class)) {
            return CollectionUtils.array(Color.class);
        }
        if (mode == Changer.ChangeMode.SET && (expression.canReturn(Entity.class) || expression.canReturn(Block.class) || expression.canReturn(ItemType.class))) {
            return CollectionUtils.array(Color.class);
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Color[] colors = delta != null ? (Color[])delta : null;
        Consumer<TextDisplay> displayChanger = this.getDisplayChanger(mode, colors);
        Consumer<FireworkEffect> fireworkChanger = this.getFireworkChanger(mode, colors);
        for (Object object : this.getExpr().getArray(event)) {
            MaterialData data;
            ItemStack stack;
            if (object instanceof TextDisplay) {
                TextDisplay display = (TextDisplay)object;
                displayChanger.accept(display);
                continue;
            }
            if (object instanceof FireworkEffect) {
                FireworkEffect effect = (FireworkEffect)object;
                fireworkChanger.accept(effect);
                continue;
            }
            if (mode == Changer.ChangeMode.SET && (object instanceof Block || object instanceof Colorable)) {
                Block block;
                BlockState blockState;
                assert (colors[0] != null);
                Colorable colorable = this.getColorable(object);
                if (colorable != null) {
                    try {
                        colorable.setColor(colors[0].asDyeColor());
                    }
                    catch (UnsupportedOperationException ex) {
                        Skript.error("Tried setting the color of a bed, but this isn't possible in your Minecraft version, since different colored beds are different materials. Instead, set the block to right material, such as a blue bed.");
                    }
                    continue;
                }
                if (!(object instanceof Block) || !((blockState = (block = (Block)object).getState()) instanceof Banner)) continue;
                Banner banner = (Banner)blockState;
                banner.setBaseColor(colors[0].asDyeColor());
                continue;
            }
            if (mode != Changer.ChangeMode.SET || !(object instanceof Item) && !(object instanceof ItemType)) continue;
            assert (colors[0] != null);
            ItemStack itemStack = stack = object instanceof Item ? ((Item)object).getItemStack() : ((ItemType)object).getRandom();
            if (stack == null || !((data = stack.getData()) instanceof Colorable)) continue;
            Colorable colorable = (Colorable)data;
            colorable.setColor(colors[0].asDyeColor());
            stack.setData(data);
            if (!(object instanceof Item)) continue;
            Item item = (Item)object;
            item.setItemStack(stack);
        }
    }

    @Override
    public Class<? extends Color> getReturnType() {
        return Color.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "color of " + this.getExpr().toString(event, debug);
    }

    private Consumer<TextDisplay> getDisplayChanger(Changer.ChangeMode mode, Color @Nullable [] colors) {
        Color color = colors != null && colors.length == 1 ? colors[0] : null;
        return switch (mode) {
            case Changer.ChangeMode.RESET -> display -> display.setDefaultBackground(true);
            case Changer.ChangeMode.SET -> display -> {
                if (color != null) {
                    if (display.isDefaultBackground()) {
                        display.setDefaultBackground(false);
                    }
                    display.setBackgroundColor(color.asBukkitColor());
                }
            };
            default -> display -> {};
        };
    }

    private Consumer<FireworkEffect> getFireworkChanger(Changer.ChangeMode mode, Color @Nullable [] colors) {
        return switch (mode) {
            case Changer.ChangeMode.ADD -> effect -> {
                for (Color color : colors) {
                    effect.getColors().add(color.asBukkitColor());
                }
            };
            case Changer.ChangeMode.REMOVE, Changer.ChangeMode.REMOVE_ALL -> effect -> {
                for (Color color : colors) {
                    effect.getColors().remove(color.asBukkitColor());
                }
            };
            case Changer.ChangeMode.RESET, Changer.ChangeMode.DELETE -> effect -> effect.getColors().clear();
            case Changer.ChangeMode.SET -> effect -> {
                effect.getColors().clear();
                for (Color color : colors) {
                    effect.getColors().add(color.asBukkitColor());
                }
            };
            default -> effect -> {};
        };
    }

    @Nullable
    private Colorable getColorable(Object colorable) {
        if (colorable instanceof Item || colorable instanceof ItemType) {
            ItemStack item;
            ItemStack itemStack = item = colorable instanceof Item ? ((Item)colorable).getItemStack() : ((ItemType)colorable).getRandom();
            if (item == null) {
                return null;
            }
            MaterialData data = item.getData();
            if (data instanceof Colorable) {
                return (Colorable)data;
            }
        } else if (colorable instanceof Block) {
            BlockState state = ((Block)colorable).getState();
            if (state instanceof Colorable) {
                return (Colorable)state;
            }
        } else if (colorable instanceof Colorable) {
            return (Colorable)colorable;
        }
        return null;
    }

    @Nullable
    private Color getColor(Object object) {
        Block block;
        BlockState blockState;
        Colorable colorable = this.getColorable(object);
        if (colorable != null) {
            DyeColor dyeColor = colorable.getColor();
            if (dyeColor == null) {
                return null;
            }
            return SkriptColor.fromDyeColor(dyeColor);
        }
        if (object instanceof Block && (blockState = (block = (Block)object).getState()) instanceof Banner) {
            Banner banner = (Banner)blockState;
            return SkriptColor.fromDyeColor(banner.getBaseColor());
        }
        if (object instanceof PotionEffectType) {
            PotionEffectType potionEffectType = (PotionEffectType)object;
            return ColorRGB.fromBukkitColor(potionEffectType.getColor());
        }
        return null;
    }

    static {
        Object types = "blocks/itemtypes/entities/fireworkeffects/potioneffecttypes";
        if (Skript.isRunningMinecraft(1, 19, 4)) {
            types = (String)types + "/displays";
        }
        ExprColorOf.register(ExprColorOf.class, Color.class, "colo[u]r[s]", (String)types);
    }
}

