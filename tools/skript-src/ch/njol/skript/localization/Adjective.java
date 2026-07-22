/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.GeneralWords;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import java.util.HashMap;
import org.jetbrains.annotations.Nullable;

public class Adjective
extends Message {
    private static final int DEFINITE_ARTICLE = -100;
    private static final String DEFINITE_ARTICLE_TOKEN = "+";
    private final HashMap<Integer, String> genders = new HashMap();
    @Nullable
    String def;

    public Adjective(String key) {
        super(key);
    }

    @Override
    protected void onValueChange() {
        int c2;
        String v;
        this.genders.clear();
        this.def = v = this.getValue();
        if (v == null) {
            return;
        }
        int s = v.indexOf(64);
        int e = v.lastIndexOf(64);
        if (s == -1) {
            return;
        }
        if (s == e) {
            Skript.error("Invalid use of '@' in the adjective '" + this.key + "' in the language file: " + v);
            return;
        }
        this.def = v.substring(0, s) + v.substring(e + 1);
        int c = s;
        do {
            int g;
            c2 = v.indexOf(64, c + 1);
            int d = v.indexOf(58, c + 1);
            if (d == -1 || d > c2) {
                Skript.error("Missing colon (:) to separate the gender in the adjective '" + this.key + "' in the language file at index " + c + ": " + v);
                return;
            }
            String gender = v.substring(c + 1, d);
            int n = g = gender.equals(DEFINITE_ARTICLE_TOKEN) ? -100 : Noun.getGender(gender, this.key);
            if (this.genders.containsKey(g)) continue;
            this.genders.put(g, v.substring(0, s) + v.substring(d + 1, c2) + v.substring(e + 1));
        } while ((c = c2) < e);
    }

    @Override
    public String toString() {
        this.validate();
        return this.def;
    }

    public String toString(int gender, int flags) {
        this.validate();
        if ((flags & 2) != 0 && this.genders.containsKey(-100)) {
            gender = -100;
        } else if ((flags & 1) != 0) {
            gender = -2;
        }
        String a = this.genders.get(gender);
        if (a != null) {
            return a;
        }
        return this.def;
    }

    public static String toString(Adjective[] adjectives, int gender, int flags, boolean and) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < adjectives.length; ++i) {
            if (i != 0) {
                if (i == adjectives.length - 1) {
                    b.append(" ").append(and ? GeneralWords.and : GeneralWords.or).append(" ");
                } else {
                    b.append(", ");
                }
            }
            b.append(adjectives[i].toString(gender, flags));
        }
        return b.toString();
    }

    public String toString(Noun n, int flags) {
        return n.toString(this, flags);
    }
}

