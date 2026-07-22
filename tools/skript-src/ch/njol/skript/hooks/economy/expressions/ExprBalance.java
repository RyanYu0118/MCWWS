/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.economy.expressions;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.RequiredPlugins;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.hooks.VaultHook;
import ch.njol.skript.hooks.economy.classes.Money;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name(value="Money")
@Description(value={"How much virtual money a player has (can be changed)."})
@Example.Examples(value={@Example(value="message \"You have %player's money%\" # the currency name will be added automatically"), @Example(value="remove 20$ from the player's balance # replace '$' by whatever currency you use"), @Example(value="add 200 to the player's account # or omit the currency altogether")})
@Since(value={"2.0, 2.5 (offline players)"})
@RequiredPlugins(value={"Vault", "an economy plugin that supports Vault"})
public class ExprBalance
extends SimplePropertyExpression<OfflinePlayer, Money> {
    @Override
    public Money convert(OfflinePlayer player) {
        try {
            return new Money(VaultHook.economy.getBalance(player));
        }
        catch (Exception ex) {
            return new Money(VaultHook.economy.getBalance(player.getName()));
        }
    }

    @Override
    public Class<? extends Money> getReturnType() {
        return Money.class;
    }

    @Override
    protected String getPropertyName() {
        return "money";
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.REMOVE_ALL) {
            return null;
        }
        return new Class[]{Money.class, Number.class};
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null) {
            for (OfflinePlayer p : (OfflinePlayer[])this.getExpr().getArray(event)) {
                VaultHook.economy.withdrawPlayer(p, VaultHook.economy.getBalance(p));
            }
            return;
        }
        double money = delta[0] instanceof Number ? ((Number)delta[0]).doubleValue() : ((Money)delta[0]).getAmount();
        block6: for (OfflinePlayer player : (OfflinePlayer[])this.getExpr().getArray(event)) {
            switch (mode) {
                case SET: {
                    double balance = VaultHook.economy.getBalance(player);
                    if (balance < money) {
                        VaultHook.economy.depositPlayer(player, money - balance);
                        continue block6;
                    }
                    if (!(balance > money)) continue block6;
                    VaultHook.economy.withdrawPlayer(player, balance - money);
                    continue block6;
                }
                case ADD: {
                    VaultHook.economy.depositPlayer(player, money);
                    continue block6;
                }
                case REMOVE: {
                    VaultHook.economy.withdrawPlayer(player, money);
                }
            }
        }
    }

    static {
        ExprBalance.register(ExprBalance.class, Money.class, "(money|balance|[bank] account)", "offlineplayers");
    }
}

