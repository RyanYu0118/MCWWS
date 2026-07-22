/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.DyeColor
 *  org.bukkit.block.Banner
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.banner.Pattern
 *  org.bukkit.block.banner.PatternType
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.BannerMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.bukkitutil.ItemUtils;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.skript.util.slot.Slot;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.DyeColor;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

@Name(value="Banner Patterns")
@Description(value={"Gets or sets the banner patterns of a banner.", "In order to set a specific position of a banner, there needs to be that many patterns already on the banner.", "This expression will add filler patterns to the banner to allow the specified position to be set.", "For Example, setting the 3rd banner pattern of a banner that has no patterns on it, will internally add 3 base patterns, allowing the 3rd banner pattern to be set."})
@Example.Examples(value={@Example(value="broadcast banner patterns of {_banneritem}"), @Example(value="broadcast 1st banner pattern of block at location(0,0,0)"), @Example(value="clear banner patterns of {_banneritem}")})
@Since(value={"2.10"})
public class ExprBannerPatterns
extends PropertyExpression<Object, Pattern> {
    private Expression<?> objects;
    private Expression<Integer> patternNumber = null;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern <= 1) {
            this.objects = exprs[0];
        } else if (matchedPattern == 2) {
            this.patternNumber = exprs[0];
            this.objects = exprs[1];
        } else {
            this.patternNumber = exprs[1];
            this.objects = exprs[0];
        }
        this.setExpr(this.objects);
        return true;
    }

    protected Pattern @Nullable [] get(Event event, Object[] source) {
        ArrayList<Pattern> patterns = new ArrayList<Pattern>();
        Integer placement = this.patternNumber != null ? this.patternNumber.getSingle(event) : null;
        for (Object object : this.objects.getArray(event)) {
            ItemMeta itemMeta;
            if (object instanceof Block) {
                Block block = (Block)object;
                BlockState blockState = block.getState();
                if (!(blockState instanceof Banner)) continue;
                Banner banner = (Banner)blockState;
                if (placement != null && banner.numberOfPatterns() >= placement) {
                    patterns.add(banner.getPattern(placement - 1));
                    continue;
                }
                if (placement != null) continue;
                patterns.addAll(banner.getPatterns());
                continue;
            }
            ItemStack itemStack = ItemUtils.asItemStack(object);
            if (itemStack == null || !((itemMeta = itemStack.getItemMeta()) instanceof BannerMeta)) continue;
            BannerMeta bannerMeta = (BannerMeta)itemMeta;
            if (placement != null && bannerMeta.numberOfPatterns() >= placement) {
                patterns.add(bannerMeta.getPattern(placement - 1));
                continue;
            }
            if (placement != null) continue;
            patterns.addAll(bannerMeta.getPatterns());
        }
        return patterns.toArray(new Pattern[0]);
    }

    private Consumer<BannerMeta> getPlacementMetaChanger(Changer.ChangeMode mode, int placement, @Nullable Pattern pattern) {
        return switch (mode) {
            case Changer.ChangeMode.SET -> bannerMeta -> {
                if (bannerMeta.numberOfPatterns() < placement) {
                    int toAdd = placement - bannerMeta.numberOfPatterns();
                    for (int i = 0; i < toAdd; ++i) {
                        bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BASE));
                    }
                }
                bannerMeta.setPattern(placement - 1, pattern);
            };
            case Changer.ChangeMode.DELETE -> bannerMeta -> {
                if (bannerMeta.numberOfPatterns() >= placement) {
                    bannerMeta.removePattern(placement - 1);
                }
            };
            default -> bannerMeta -> {};
        };
    }

    private Consumer<Banner> getPlacementBlockChanger(Changer.ChangeMode mode, int placement, @Nullable Pattern pattern) {
        return switch (mode) {
            case Changer.ChangeMode.SET -> banner -> {
                if (banner.numberOfPatterns() < placement) {
                    int toAdd = placement - banner.numberOfPatterns();
                    for (int i = 0; i < toAdd; ++i) {
                        banner.addPattern(new Pattern(DyeColor.GRAY, PatternType.BASE));
                    }
                }
                banner.setPattern(placement - 1, pattern);
            };
            case Changer.ChangeMode.DELETE -> banner -> {
                if (banner.numberOfPatterns() >= placement) {
                    banner.removePattern(placement - 1);
                }
            };
            default -> banner -> {};
        };
    }

    private Consumer<BannerMeta> getAllMetaChanger(Changer.ChangeMode mode, List<Pattern> patterns) {
        return switch (mode) {
            case Changer.ChangeMode.SET -> bannerMeta -> bannerMeta.setPatterns(patterns);
            case Changer.ChangeMode.DELETE -> bannerMeta -> bannerMeta.setPatterns(new ArrayList());
            case Changer.ChangeMode.ADD -> bannerMeta -> patterns.forEach(arg_0 -> ((BannerMeta)bannerMeta).addPattern(arg_0));
            case Changer.ChangeMode.REMOVE -> bannerMeta -> {
                List current = bannerMeta.getPatterns();
                current.removeAll(patterns);
                bannerMeta.setPatterns(current);
            };
            default -> bannerMeta -> {};
        };
    }

    private Consumer<Banner> getAllBlockChanger(Changer.ChangeMode mode, List<Pattern> patterns) {
        return switch (mode) {
            case Changer.ChangeMode.SET -> banner -> banner.setPatterns(patterns);
            case Changer.ChangeMode.DELETE -> banner -> banner.setPatterns(new ArrayList());
            case Changer.ChangeMode.ADD -> banner -> patterns.forEach(arg_0 -> ((Banner)banner).addPattern(arg_0));
            case Changer.ChangeMode.REMOVE -> banner -> {
                List current = banner.getPatterns();
                current.removeAll(patterns);
                banner.setPatterns(current);
            };
            default -> banner -> {};
        };
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case Changer.ChangeMode.SET, Changer.ChangeMode.DELETE -> {
                if (this.patternNumber != null) {
                    yield CollectionUtils.array(Pattern.class);
                }
                yield CollectionUtils.array(Pattern[].class);
            }
            case Changer.ChangeMode.ADD, Changer.ChangeMode.REMOVE -> {
                if (this.patternNumber != null) {
                    yield null;
                }
                yield CollectionUtils.array(Pattern[].class);
            }
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Consumer<Banner> blockChanger;
        Consumer<BannerMeta> metaChanger;
        ArrayList<Pattern> patterns;
        Integer patternNum;
        int placement = 0;
        if (this.patternNumber != null && (patternNum = this.patternNumber.getSingle(event)) != null) {
            placement = patternNum;
        }
        List<Pattern> list = delta != null ? Arrays.stream(delta).map(Pattern.class::cast).toList() : (patterns = new ArrayList<Pattern>());
        if (placement >= 1) {
            Pattern pattern = patterns.size() == 1 ? (Pattern)patterns.get(0) : null;
            metaChanger = this.getPlacementMetaChanger(mode, placement, pattern);
            blockChanger = this.getPlacementBlockChanger(mode, placement, pattern);
        } else {
            metaChanger = this.getAllMetaChanger(mode, patterns);
            blockChanger = this.getAllBlockChanger(mode, patterns);
        }
        for (Object object : this.objects.getArray(event)) {
            ItemMeta itemMeta;
            Block block;
            BlockState blockState;
            if (object instanceof Block && (blockState = (block = (Block)object).getState()) instanceof Banner) {
                Banner banner = (Banner)blockState;
                blockChanger.accept(banner);
                banner.update(true, false);
                continue;
            }
            ItemStack itemStack = ItemUtils.asItemStack(object);
            if (itemStack == null || !((itemMeta = itemStack.getItemMeta()) instanceof BannerMeta)) continue;
            BannerMeta bannerMeta = (BannerMeta)itemMeta;
            metaChanger.accept(bannerMeta);
            itemStack.setItemMeta((ItemMeta)bannerMeta);
            if (object instanceof Slot) {
                Slot slot = (Slot)object;
                slot.setItem(itemStack);
                continue;
            }
            if (object instanceof ItemType) {
                ItemType itemType = (ItemType)object;
                itemType.setItemMeta((ItemMeta)bannerMeta);
                continue;
            }
            if (!(object instanceof ItemStack)) continue;
            ItemStack itemStack1 = (ItemStack)object;
            itemStack1.setItemMeta((ItemMeta)bannerMeta);
        }
    }

    @Override
    public boolean isSingle() {
        return this.patternNumber != null && this.getExpr().isSingle();
    }

    @Override
    public Class<Pattern> getReturnType() {
        return Pattern.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        if (this.patternNumber != null) {
            builder.append("banner pattern", this.patternNumber);
        } else {
            builder.append((Object)"banner patterns");
        }
        builder.append("of", this.objects);
        return builder.toString();
    }

    static {
        Skript.registerExpression(ExprBannerPatterns.class, Pattern.class, ExpressionType.PROPERTY, "[all [[of] the]|the] banner pattern[s] of %itemstacks/itemtypes/slots/blocks%", "%itemstacks/itemtypes/slots/blocks%'[s] banner pattern[s]", "[the] %integer%[st|nd|rd|th] [banner] pattern of %itemstacks/itemtypes/slots/blocks%", "%itemstacks/itemtypes/slots/blocks%'[s] %integer%[st|nd|rd|th] [banner] pattern");
    }
}

