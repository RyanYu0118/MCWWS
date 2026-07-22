/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.components.CustomModelDataComponent
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Color;
import ch.njol.skript.util.ColorRGB;
import ch.njol.util.Kleenean;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.Nullable;

@Name(value="Custom Model Data")
@Description(value={"Get/set the custom model data of an item. Using just `custom model data` will return an integer. Items without model data will return 0.", "Since 1.21.4, custom model data instead consists of a list of numbers (floats), a list of booleans (flags), a list of strings, and a list of colours. Accessing and modifying these lists can be done type-by-type, or all at once with `complete custom model data`. This is the more accurate and recommended method of using custom model data."})
@Example.Examples(value={@Example(value="set custom model data of player's tool to 3\nset {_model} to custom model data of player's tool\n"), @Example(value="set custom model data colours of {_flag} to red, white, and blue\nadd 10.5 to the model data floats of {_flag}\n"), @Example(value="set the full custom model data of {_item} to 10, \"sword\", and rgb(100, 200, 30)\n")})
@RequiredPlugins(value={"Minecraft 1.21.4+ (floats/flags/strings/colours/full model data)"})
@Since(value={"2.5", "2.12 (floats/flags/strings/colours/full model data)"})
public class ExprCustomModelData
extends PropertyExpression<ItemType, Object> {
    private static final boolean USE_NEW_CMD = Skript.classExists("org.bukkit.inventory.meta.components.CustomModelDataComponent");
    private CMDType dataType;
    private Class<?> returnType;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.dataType = CMDType.values()[parseResult.mark];
        this.returnType = Classes.getSuperClassInfo(this.dataType.types).getC();
        this.setExpr(expressions[0]);
        return true;
    }

    protected Object[] get(Event event, ItemType[] source) {
        int n2 = 0;
        ItemType[] itemTypeArray = source;
        int n3 = itemTypeArray.length;
        if (n2 < n3) {
            ItemType from = itemTypeArray[n2];
            ItemMeta meta = from.getItemMeta();
            if (this.dataType == CMDType.SINGLE_INT) {
                if (!(!meta.hasCustomModelData() || USE_NEW_CMD && meta.getCustomModelDataComponent().getFloats().isEmpty())) {
                    return new Integer[]{meta.getCustomModelData()};
                }
                return new Integer[]{0};
            }
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            IntFunction<T[]> arrayConstructor = n -> (Object[])Array.newInstance(this.getReturnType(), n);
            return switch (this.dataType.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> throw new IllegalStateException("Unreachable state for SINGLE_INT!");
                case 1 -> component.getFloats().toArray(arrayConstructor);
                case 2 -> component.getFlags().toArray(arrayConstructor);
                case 3 -> component.getStrings().toArray(arrayConstructor);
                case 4 -> (ColorRGB[])component.getColors().stream().map(ColorRGB::fromBukkitColor).toList().toArray(ColorRGB[]::new);
                case 5 -> {
                    ArrayList<ColorRGB> data = new ArrayList<ColorRGB>();
                    data.addAll(component.getFloats());
                    data.addAll(component.getFlags());
                    data.addAll(component.getStrings());
                    data.addAll(component.getColors().stream().map(ColorRGB::fromBukkitColor).toList());
                    yield data.toArray(arrayConstructor);
                }
            };
        }
        return (Object[])Array.newInstance(this.getReturnType(), 0);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE, Changer.ChangeMode.SET, Changer.ChangeMode.DELETE, Changer.ChangeMode.RESET -> {
                Class[] arrayClasses = new Class[this.dataType.types.length];
                for (int i = 0; i < this.dataType.types.length; ++i) {
                    arrayClasses[i] = this.dataType.types[i].arrayType();
                }
                yield arrayClasses;
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        for (ItemType item : (ItemType[])this.getExpr().getArray(event)) {
            ItemMeta meta = item.getItemMeta();
            switch (this.dataType.ordinal()) {
                case 0: {
                    long deltaValue = delta == null ? 0L : (long)((Number)delta[0]).intValue();
                    this.changeOld(meta, mode, deltaValue);
                    break;
                }
                case 1: 
                case 2: 
                case 3: 
                case 4: {
                    this.changeSingleType(meta, mode, delta);
                    break;
                }
                case 5: {
                    this.changeAll(meta, mode, delta);
                }
            }
            item.setItemMeta(meta);
        }
    }

    private void changeOld(ItemMeta meta, Changer.ChangeMode mode, long delta) {
        if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
            meta.setCustomModelData(null);
            return;
        }
        long oldData = 0L;
        if (!(!meta.hasCustomModelData() || USE_NEW_CMD && meta.getCustomModelDataComponent().getFloats().isEmpty())) {
            oldData = meta.getCustomModelData();
        }
        switch (mode) {
            case REMOVE: 
            case REMOVE_ALL: {
                delta = -delta;
            }
            case ADD: {
                delta = oldData + delta;
                meta.setCustomModelData(Integer.valueOf((int)delta));
                break;
            }
            case SET: {
                meta.setCustomModelData(Integer.valueOf((int)delta));
            }
        }
    }

    private <T> void changeSingleType(ItemMeta meta, Changer.ChangeMode mode, T @Nullable [] delta) {
        if (delta == null && mode != Changer.ChangeMode.DELETE && mode != Changer.ChangeMode.RESET) {
            return;
        }
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        List<Object> data = new ArrayList<T>(switch (this.dataType.ordinal()) {
            case 1 -> component.getFloats();
            case 2 -> component.getFlags();
            case 3 -> component.getStrings();
            case 4 -> component.getColors().stream().map(ColorRGB::fromBukkitColor).toList();
            default -> throw new IllegalStateException("Wrong changemode for changeSingleType");
        });
        switch (mode) {
            case REMOVE: {
                data.removeAll(Arrays.asList(delta));
                break;
            }
            case ADD: {
                data.addAll(Arrays.asList(delta));
                break;
            }
            case SET: {
                data = Arrays.asList(delta);
                break;
            }
            case DELETE: 
            case RESET: {
                data.clear();
            }
        }
        switch (this.dataType.ordinal()) {
            case 1: {
                component.setFloats(data);
                break;
            }
            case 2: {
                component.setFlags(data);
                break;
            }
            case 3: {
                component.setStrings(data);
                break;
            }
            case 4: {
                component.setColors(data.stream().map(colorRGB -> ((ColorRGB)colorRGB).asBukkitColor()).toList());
            }
        }
        meta.setCustomModelDataComponent(component);
    }

    private void changeAll(ItemMeta meta, Changer.ChangeMode mode, Object @Nullable [] delta) {
        if (mode == Changer.ChangeMode.DELETE || mode == Changer.ChangeMode.RESET) {
            meta.setCustomModelDataComponent(null);
            return;
        }
        if (delta == null) {
            return;
        }
        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        ArrayList<Float> floats = new ArrayList<Float>(component.getFloats());
        ArrayList<Boolean> flags = new ArrayList<Boolean>(component.getFlags());
        ArrayList<String> strings = new ArrayList<String>(component.getStrings());
        ArrayList<ColorRGB> colors = new ArrayList<ColorRGB>(component.getColors().stream().map(ColorRGB::fromBukkitColor).toList());
        switch (mode) {
            case REMOVE: {
                for (Object deltaValue : delta) {
                    if (deltaValue instanceof Float) {
                        floats.remove(deltaValue);
                        continue;
                    }
                    if (deltaValue instanceof Boolean) {
                        flags.remove(deltaValue);
                        continue;
                    }
                    if (deltaValue instanceof String) {
                        strings.remove(deltaValue);
                        continue;
                    }
                    if (!(deltaValue instanceof Color)) continue;
                    colors.remove(deltaValue);
                }
                break;
            }
            case SET: {
                floats.clear();
                flags.clear();
                strings.clear();
                colors.clear();
            }
            case ADD: {
                for (Object deltaValue : delta) {
                    if (deltaValue instanceof Float) {
                        Float aFloat = (Float)deltaValue;
                        floats.addLast(aFloat);
                        continue;
                    }
                    if (deltaValue instanceof Boolean) {
                        Boolean aBoolean = (Boolean)deltaValue;
                        flags.addLast(aBoolean);
                        continue;
                    }
                    if (deltaValue instanceof String) {
                        String string = (String)deltaValue;
                        strings.addLast(string);
                        continue;
                    }
                    if (!(deltaValue instanceof Color)) continue;
                    Color color = (Color)deltaValue;
                    colors.addLast((ColorRGB)color);
                }
                break;
            }
        }
        component.setFloats(floats);
        component.setFlags(flags);
        component.setStrings(strings);
        component.setColors(colors.stream().map(Color::asBukkitColor).toList());
        meta.setCustomModelDataComponent(component);
    }

    @Override
    public boolean isSingle() {
        return this.dataType == CMDType.SINGLE_INT && this.getExpr().isSingle();
    }

    @Override
    public Class<?> getReturnType() {
        return this.returnType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return this.dataType.types;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return (switch (this.dataType.ordinal()) {
            default -> throw new MatchException(null, null);
            case 5 -> "complete custom model data";
            case 1 -> "custom model data floats";
            case 2 -> "custom model data flags";
            case 3 -> "custom model data strings";
            case 4 -> "custom model data colors";
            case 0 -> "custom model data";
        }) + " of " + this.getExpr().toString(event, debug);
    }

    static {
        if (USE_NEW_CMD) {
            ArrayList<String> patterns = new ArrayList<String>();
            patterns.addAll(Arrays.asList(PropertyExpression.getPatterns("[custom] model data", "itemtypes")));
            patterns.addAll(Arrays.asList(PropertyExpression.getPatterns("[custom] model data (1:floats|2:flags|3:strings|4:colo[u]rs)", "itemtype")));
            patterns.addAll(Arrays.asList(PropertyExpression.getPatterns("(5:(complete|full)) [custom] model data", "itemtype")));
            Skript.registerExpression(ExprCustomModelData.class, Object.class, ExpressionType.PROPERTY, (String[])patterns.toArray(String[]::new));
        } else {
            ExprCustomModelData.register(ExprCustomModelData.class, Object.class, "[custom] model data", "itemtypes");
        }
    }

    private static enum CMDType {
        SINGLE_INT(Integer.class),
        FLOATS(Float.class),
        FLAGS(Boolean.class),
        STRINGS(String.class),
        COLORS(Color.class),
        ALL(Float.class, Boolean.class, String.class, Color.class);

        private final Class<?>[] types;

        private CMDType(Class<?> ... returns) {
            this.types = returns;
        }
    }
}

