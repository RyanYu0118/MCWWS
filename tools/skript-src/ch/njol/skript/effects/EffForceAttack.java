/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Damageable
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxStringBuilder;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

@Name(value="Force Attack")
@Description(value={"Makes a living entity attack an entity with a melee attack.", "Using 'attack' will make the attacker use the item in their main hand and will apply extra data from the item, including enchantments and attributes.", "Using 'damage' with a number of hearts will not account for the item in the main hand and will always be the number provided."})
@Example.Examples(value={@Example(value="spawn a wolf at location(0, 0, 0)\nmake last spawned wolf attack all players\n"), @Example(value="spawn a zombie at location(0, 0, 0)\nmake player damage last spawned zombie by 2\n")})
@Since(value={"2.5.1, 2.13 (multiple, amount)"})
@RequiredPlugins(value={"Minecraft 1.15.2+"})
public class EffForceAttack
extends Effect
implements SyntaxRuntimeErrorProducer {
    private Expression<LivingEntity> attackers;
    private Expression<Entity> victims;
    @Nullable
    private Expression<Number> amount;
    private Node node;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.attackers = exprs[0];
        this.victims = exprs[1];
        if (matchedPattern >= 2) {
            this.amount = exprs[2];
        }
        this.node = this.getParser().getNode();
        return true;
    }

    @Override
    protected void execute(Event event) {
        Double amount = null;
        if (this.amount != null) {
            Number number = this.amount.getSingle(event);
            if (number == null) {
                return;
            }
            Double preAmount = number.doubleValue();
            if (preAmount <= 0.0) {
                this.error("Cannot damage an entity by 0 or less. Consider healing instead.");
                return;
            }
            if (!Double.isFinite(preAmount)) {
                return;
            }
            amount = preAmount * 2.0;
        }
        LivingEntity[] attackers = this.attackers.getArray(event);
        Entity[] victims = this.victims.getArray(event);
        if (amount == null) {
            for (Entity victim : victims) {
                for (LivingEntity attacker : attackers) {
                    attacker.attack(victim);
                }
            }
        } else {
            for (Entity victim : victims) {
                if (!(victim instanceof Damageable)) continue;
                Damageable damageable = (Damageable)victim;
                for (LivingEntity attacker : attackers) {
                    damageable.damage(amount.doubleValue(), (Entity)attacker);
                }
            }
        }
    }

    @Override
    public Node getNode() {
        return this.node;
    }

    @Override
    public String toString(Event event, boolean debug) {
        SyntaxStringBuilder builder = new SyntaxStringBuilder(event, debug);
        builder.append("make", this.attackers);
        if (this.amount == null) {
            builder.append("attack", this.victims);
        } else {
            builder.append("damage", this.victims, "by", this.amount);
        }
        return builder.toString();
    }

    static {
        Skript.registerEffect(EffForceAttack.class, "make %livingentities% attack %entities%", "force %livingentities% to attack %entities%", "make %livingentities% damage %entities% by %number% [heart[s]]", "force %livingentities% to damage %entities% by %number% [heart[s]]");
    }
}

