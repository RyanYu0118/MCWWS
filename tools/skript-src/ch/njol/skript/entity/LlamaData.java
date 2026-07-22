/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Llama
 *  org.bukkit.entity.Llama$Color
 *  org.bukkit.entity.TraderLlama
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.variables.Variables;
import ch.njol.util.coll.CollectionUtils;
import java.util.Objects;
import org.bukkit.entity.Llama;
import org.bukkit.entity.TraderLlama;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LlamaData
extends EntityData<Llama> {
    private static final Patterns<LlamaState> PATTERNS = new Patterns(new Object[][]{{"llama", new LlamaState(null, false)}, {"creamy llama", new LlamaState(Llama.Color.CREAMY, false)}, {"white llama", new LlamaState(Llama.Color.WHITE, false)}, {"brown llama", new LlamaState(Llama.Color.BROWN, false)}, {"gray llama", new LlamaState(Llama.Color.GRAY, false)}, {"trader llama", new LlamaState(null, true)}, {"creamy trader llama", new LlamaState(Llama.Color.CREAMY, true)}, {"white trader llama", new LlamaState(Llama.Color.WHITE, true)}, {"brown trader llama", new LlamaState(Llama.Color.BROWN, true)}, {"gray trader llama", new LlamaState(Llama.Color.GRAY, true)}});
    private static final Llama.Color[] LLAMA_COLORS = Llama.Color.values();
    @Nullable
    private Llama.Color color = null;
    private boolean isTrader;

    public LlamaData() {
    }

    public LlamaData(@Nullable Llama.Color color, boolean isTrader) {
        this.color = color;
        this.isTrader = isTrader;
        this.codeNameIndex = PATTERNS.getMatchedPattern(new LlamaState(color, isTrader), 0).orElse(0);
    }

    public LlamaData(@Nullable LlamaState llamaState) {
        if (llamaState != null) {
            this.color = llamaState.color;
            this.isTrader = llamaState.trader;
            this.codeNameIndex = PATTERNS.getMatchedPattern(llamaState, 0).orElse(0);
        } else {
            this.color = null;
            this.isTrader = false;
            this.codeNameIndex = PATTERNS.getMatchedPattern(new LlamaState(this.color, this.isTrader), 0).orElse(0);
        }
    }

    @Override
    protected boolean init(Literal<?>[] exprs, int matchedCodeName, int matchedPattern, SkriptParser.ParseResult parseResult) {
        LlamaState llamaState = PATTERNS.getInfo(matchedCodeName);
        assert (llamaState != null);
        this.color = llamaState.color;
        this.isTrader = llamaState.trader;
        return true;
    }

    @Override
    protected boolean init(@Nullable Class<? extends Llama> entityClass, @Nullable Llama llama) {
        if (entityClass != null) {
            this.isTrader = TraderLlama.class.isAssignableFrom(entityClass);
        }
        if (llama != null) {
            this.color = llama.getColor();
            this.isTrader = llama instanceof TraderLlama;
            this.codeNameIndex = PATTERNS.getMatchedPattern(new LlamaState(this.color, this.isTrader), 0).orElse(0);
        }
        return true;
    }

    @Override
    public void set(Llama llama) {
        Llama.Color color = this.color;
        if (color == null) {
            color = CollectionUtils.getRandom(LLAMA_COLORS);
        }
        assert (color != null);
        llama.setColor(color);
    }

    @Override
    protected boolean match(Llama llama) {
        if (this.isTrader && !(llama instanceof TraderLlama)) {
            return false;
        }
        return this.dataMatch(this.color, llama.getColor());
    }

    @Override
    public Class<? extends Llama> getType() {
        return this.isTrader ? TraderLlama.class : Llama.class;
    }

    @Override
    @NotNull
    public EntityData<?> getSuperType() {
        return new LlamaData();
    }

    @Override
    protected int hashCode_i() {
        int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(this.color);
        result = prime * result + (this.isTrader ? 1 : 0);
        return result;
    }

    @Override
    protected boolean equals_i(EntityData<?> entityData) {
        if (!(entityData instanceof LlamaData)) {
            return false;
        }
        LlamaData other = (LlamaData)entityData;
        return this.isTrader == other.isTrader && other.color == this.color;
    }

    @Override
    public boolean isSupertypeOf(EntityData<?> entityData) {
        if (!(entityData instanceof LlamaData)) {
            return false;
        }
        LlamaData other = (LlamaData)entityData;
        if (this.isTrader && !other.isTrader) {
            return false;
        }
        return this.dataMatch(this.color, other.color);
    }

    static {
        EntityData.register(LlamaData.class, "llama", Llama.class, 0, PATTERNS.getPatterns());
        Variables.yggdrasil.registerSingleClass(Llama.Color.class, "Llama.Color");
    }

    public record LlamaState(Llama.Color color, boolean trader) {
    }
}

