/*
 * Decompiled with CFR 0.152.
 */
package cat.necko.bags.utils;

public record Tuple<A, B>(A a, B b) {
    public static <A, B> Tuple<A, B> of(A a, B b) {
        return new Tuple<A, B>(a, b);
    }
}

