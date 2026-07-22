/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.hooks.economy.classes;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.data.JavaClasses;
import ch.njol.skript.hooks.VaultHook;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.arithmetic.Arithmetics;
import org.skriptlang.skript.lang.arithmetic.Operator;
import org.skriptlang.skript.lang.comparator.Comparator;
import org.skriptlang.skript.lang.comparator.Comparators;
import org.skriptlang.skript.lang.comparator.Relation;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;

public class Money {
    final double amount;

    public Money(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return this.amount;
    }

    @Nullable
    public static Money parse(String s) {
        Money money;
        if (VaultHook.economy == null) {
            return null;
        }
        String singular = VaultHook.economy.currencyNameSingular();
        String plural = VaultHook.economy.currencyNamePlural();
        if (plural != null && !plural.isEmpty() && (money = Money.parseMoney(s, plural)) != null) {
            return money;
        }
        if (singular != null && !singular.isEmpty()) {
            return Money.parseMoney(s, singular);
        }
        return null;
    }

    @Nullable
    private static Money parseMoney(String s, String addition) {
        Double d;
        if (StringUtils.endsWithIgnoreCase(s, addition)) {
            Double d2 = Money.parseDouble(s.substring(0, s.length() - addition.length()).trim());
            if (d2 != null) {
                return new Money(d2);
            }
        } else if (StringUtils.startsWithIgnoreCase(s, addition) && (d = Money.parseDouble(s.substring(addition.length()).trim())) != null) {
            return new Money(d);
        }
        return null;
    }

    @Nullable
    private static Double parseDouble(String s) {
        if (!JavaClasses.DECIMAL_PATTERN.matcher(s).matches()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public String toString() {
        return VaultHook.economy.format(this.amount);
    }

    static {
        Classes.registerClass(new ClassInfo<Money>(Money.class, "money").user("money").name("Money").description("A certain amount of money. Please note that this differs from <a href='#number'>numbers</a> as it includes a currency symbol or name, but usually the two are interchangeable, e.g. you can both <code>add 100$ to the player's balance</code> and <code>add 100 to the player's balance</code>.").usage("&lt;number&gt; $ or $ &lt;number&gt;, where '$' is your server's currency, e.g. '10 rupees' or '\u00a35.00'").examples("add 10\u00a3 to the player's account", "remove Fr. 9.95 from the player's money", "set the victim's money to 0", "increase the attacker's balance by the level of the victim * 100").since("2.0").before("itemtype", "itemstack").requiredPlugins("Vault", "an economy plugin that supports Vault").parser(new Parser<Money>(){

            @Override
            @Nullable
            public Money parse(String s, ParseContext context) {
                return Money.parse(s);
            }

            @Override
            public String toString(Money m, int flags) {
                return m.toString();
            }

            @Override
            public String toVariableNameString(Money o) {
                return "money:" + o.amount;
            }
        }));
        Comparators.registerComparator(Money.class, Money.class, new Comparator<Money, Money>(){

            @Override
            public Relation compare(Money m1, Money m2) {
                return Relation.get(m1.amount - m2.amount);
            }

            @Override
            public boolean supportsOrdering() {
                return true;
            }
        });
        Comparators.registerComparator(Money.class, Number.class, new Comparator<Money, Number>(){

            @Override
            public Relation compare(Money m, Number n) {
                return Relation.get(m.amount - n.doubleValue());
            }

            @Override
            public boolean supportsOrdering() {
                return true;
            }
        });
        Converters.registerConverter(Money.class, Double.class, new Converter<Money, Double>(){

            @Override
            public Double convert(Money m) {
                return m.getAmount();
            }
        });
        Arithmetics.registerOperation(Operator.ADDITION, Money.class, (left, right) -> new Money(left.getAmount() + right.getAmount()));
        Arithmetics.registerOperation(Operator.SUBTRACTION, Money.class, (left, right) -> new Money(left.getAmount() - right.getAmount()));
        Arithmetics.registerOperation(Operator.MULTIPLICATION, Money.class, (left, right) -> new Money(left.getAmount() * right.getAmount()));
        Arithmetics.registerOperation(Operator.DIVISION, Money.class, (left, right) -> new Money(left.getAmount() / right.getAmount()));
        Arithmetics.registerDifference(Money.class, (left, right) -> {
            double result = Math.abs(left.getAmount() - right.getAmount());
            if (result < 1.0E-10) {
                return new Money(0.0);
            }
            return new Money(result);
        });
        Arithmetics.registerDefaultValue(Money.class, () -> new Money(0.0));
    }
}

