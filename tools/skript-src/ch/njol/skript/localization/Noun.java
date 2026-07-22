/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.localization;

import ch.njol.skript.Skript;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.Message;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class Noun
extends Message {
    public static final String GENDERS_SECTION = "genders.";
    public static final int PLURAL = -2;
    public static final int NO_GENDER = -3;
    public static final String PLURAL_TOKEN = "x";
    public static final String NO_GENDER_TOKEN = "-";
    @Nullable
    private String singular;
    @Nullable
    private String plural;
    private int gender = 0;
    static final HashMap<String, Integer> genders = new HashMap();
    static final List<String> indefiniteArticles = new ArrayList<String>(3);
    static final List<String> definiteArticles = new ArrayList<String>(3);
    static String definitePluralArticle = "";

    public Noun(String key) {
        super(key);
    }

    @Override
    protected void onValueChange() {
        Object value = this.getValue();
        if (value == null) {
            this.plural = this.singular = this.key;
            this.gender = 0;
            return;
        }
        int g = ((String)value).lastIndexOf(64);
        if (g != -1) {
            this.gender = Noun.getGender(((String)value).substring(g + 1).trim(), this.key);
            value = ((String)value).substring(0, g).trim();
        } else {
            this.gender = 0;
        }
        PluralPair p = Noun.parsePlural((String)value);
        this.singular = p.singular;
        this.plural = p.plural;
        if (this.gender == -2 && !Objects.equals(this.singular, this.plural)) {
            Skript.warning("Noun '" + this.key + "' is of gender 'plural', but has different singular and plural values.");
        }
    }

    @Override
    public String toString() {
        this.validate();
        return this.singular;
    }

    public String toString(boolean plural) {
        this.validate();
        return plural ? this.plural : this.singular;
    }

    public String withIndefiniteArticle() {
        return this.toString(4);
    }

    public String getIndefiniteArticle() {
        this.validate();
        return this.gender == -2 || this.gender == -3 ? "" : indefiniteArticles.get(this.gender);
    }

    public String withDefiniteArticle() {
        return this.toString(2);
    }

    public String withDefiniteArticle(boolean plural) {
        return this.toString(2 | (plural ? 1 : 0));
    }

    public String getDefiniteArticle() {
        this.validate();
        return this.gender == -2 ? definitePluralArticle : (this.gender == -3 ? "" : definiteArticles.get(this.gender));
    }

    public int getGender() {
        this.validate();
        return this.gender;
    }

    public static String getArticleWithSpace(int gender, int flags) {
        if (gender == -2) {
            if ((flags & 2) != 0) {
                return definitePluralArticle + " ";
            }
        } else if (gender != -3) {
            if ((flags & 2) != 0) {
                if (gender < 0 || gender >= definiteArticles.size()) {
                    assert (false) : gender;
                    return "";
                }
                return definiteArticles.get(gender) + " ";
            }
            if ((flags & 4) != 0) {
                if (gender < 0 || gender >= indefiniteArticles.size()) {
                    assert (false) : gender;
                    return "";
                }
                return indefiniteArticles.get(gender) + " ";
            }
        }
        return "";
    }

    public final String getArticleWithSpace(int flags) {
        return Noun.getArticleWithSpace(this.getGender(), flags);
    }

    public String toString(int flags) {
        this.validate();
        StringBuilder b = new StringBuilder();
        b.append(this.getArticleWithSpace(flags));
        b.append((flags & 1) != 0 ? this.plural : this.singular);
        return b.toString();
    }

    public String withAmount(double amount) {
        this.validate();
        return Skript.toString(amount) + " " + (amount == 1.0 ? this.singular : this.plural);
    }

    public String withAmount(double amount, int flags) {
        this.validate();
        if (amount == 1.0) {
            if (this.gender == -3) {
                return this.toString((flags & 1) != 0);
            }
            if (this.gender == -2) {
                if ((flags & 2) != 0) {
                    return definitePluralArticle + " " + this.plural;
                }
                return this.plural;
            }
            if ((flags & 2) != 0) {
                return (flags & 1) != 0 ? definitePluralArticle + " " + this.plural : definiteArticles.get(this.gender) + " " + this.singular;
            }
            if ((flags & 4) != 0) {
                return indefiniteArticles.get(this.gender) + " " + this.singular;
            }
            if ((flags & 1) != 0) {
                return this.plural;
            }
        }
        return Skript.toString(amount) + " " + (amount == 1.0 ? this.singular : this.plural);
    }

    public String toString(Adjective a, int flags) {
        this.validate();
        StringBuilder b = new StringBuilder();
        b.append(this.getArticleWithSpace(flags));
        b.append(a.toString(this.gender, flags));
        b.append(" ");
        b.append((flags & 1) != 0 ? this.plural : this.singular);
        return b.toString();
    }

    public String toString(Adjective[] adjectives, int flags, boolean and) {
        this.validate();
        if (adjectives.length == 0) {
            return this.toString(flags);
        }
        StringBuilder b = new StringBuilder();
        b.append(this.getArticleWithSpace(flags));
        b.append(Adjective.toString(adjectives, this.getGender(), flags, and));
        b.append(" ");
        b.append(this.toString(flags));
        return b.toString();
    }

    public String getSingular() {
        this.validate();
        return this.singular;
    }

    public String getPlural() {
        this.validate();
        return this.plural;
    }

    @Deprecated(since="2.14", forRemoval=true)
    public static NonNullPair<String, String> getPlural(String input) {
        PluralPair pair = Noun.parsePlural(input);
        return new NonNullPair<String, String>(pair.singular, pair.plural);
    }

    public static PluralPair parsePlural(String input) {
        String x;
        StringBuilder singular = new StringBuilder();
        StringBuilder plural = new StringBuilder();
        int part = 3;
        char marker = '\u00a6';
        int markerCount = StringUtils.count(input, marker);
        int last = 0;
        int c = -1;
        while ((c = input.indexOf(marker, c + 1)) != -1) {
            x = input.substring(last, c);
            if ((part & 1) != 0) {
                singular.append(x);
            }
            if ((part & 2) != 0) {
                plural.append(x);
            }
            part = markerCount >= 2 ? part % 3 + 1 : (part == 2 ? 3 : 2);
            last = c + 1;
            --markerCount;
        }
        x = input.substring(last);
        if ((part & 1) != 0) {
            singular.append(x);
        }
        if ((part & 2) != 0) {
            plural.append(x);
        }
        return new PluralPair(singular.toString(), plural.toString());
    }

    public static String normalizePluralMarkers(String s) {
        int c = StringUtils.count(s, '\u00a6');
        if (c % 3 == 0) {
            return s;
        }
        if (c % 3 == 2) {
            int g = s.lastIndexOf(64);
            if (g == -1) {
                return s + "\u00a6";
            }
            return s.substring(0, g) + "\u00a6" + s.substring(g);
        }
        int x = s.lastIndexOf(166);
        int g = s.lastIndexOf(64);
        if (g == -1) {
            return s.substring(0, x) + "\u00a6" + s.substring(x) + "\u00a6";
        }
        return s.substring(0, x) + "\u00a6" + s.substring(x, g) + "\u00a6" + s.substring(g);
    }

    public static int getGender(String gender, String key) {
        if (gender.equalsIgnoreCase(PLURAL_TOKEN)) {
            return -2;
        }
        if (gender.equalsIgnoreCase(NO_GENDER_TOKEN)) {
            return -3;
        }
        Integer i = genders.get(gender);
        if (i != null) {
            return i;
        }
        Skript.warning("Undefined gender '" + gender + "' at " + key);
        return 0;
    }

    @Nullable
    public static String getGenderID(int gender) {
        if (gender == -2) {
            return PLURAL_TOKEN;
        }
        if (gender == -3) {
            return NO_GENDER_TOKEN;
        }
        return Language.get_(GENDERS_SECTION + gender + ".id");
    }

    public static NonNullPair<String, Integer> stripGender(String s, String key) {
        int c = ((String)s).lastIndexOf(64);
        int g = -1;
        if (c != -1) {
            g = Noun.getGender(((String)s).substring(c + 1).trim(), key);
            s = ((String)s).substring(0, c).trim();
        }
        return new NonNullPair<String, Integer>((String)s, g);
    }

    public static String stripIndefiniteArticle(String s) {
        for (String a : indefiniteArticles) {
            if (!StringUtils.startsWithIgnoreCase(s, a + " ")) continue;
            return s.substring(a.length() + 1);
        }
        return s;
    }

    public static String stripDefiniteArticle(String string) {
        for (String article : definiteArticles) {
            if (!StringUtils.startsWithIgnoreCase(string, article + " ")) continue;
            return string.substring(article.length() + 1);
        }
        return string;
    }

    public static boolean isIndefiniteArticle(String s) {
        return indefiniteArticles.contains(s.toLowerCase());
    }

    public static boolean isDefiniteArticle(String s) {
        return definiteArticles.contains(s.toLowerCase()) || definitePluralArticle.equalsIgnoreCase(s);
    }

    public static String toString(String singular, String plural, int gender, int flags) {
        return Noun.getArticleWithSpace(gender, flags) + ((flags & 1) != 0 ? plural : singular);
    }

    static {
        Language.addListener(() -> {
            String dpa;
            genders.clear();
            indefiniteArticles.clear();
            definiteArticles.clear();
            for (int i = 0; i < 100 && Language.keyExistsDefault(GENDERS_SECTION + i + ".id"); ++i) {
                String g = Language.get(GENDERS_SECTION + i + ".id");
                if (g.equalsIgnoreCase(PLURAL_TOKEN) || g.equalsIgnoreCase(NO_GENDER_TOKEN)) {
                    Skript.error("gender #" + i + " uses a reserved character as ID, please use something different!");
                    continue;
                }
                genders.put(g, i);
                String ia = Language.get_(GENDERS_SECTION + i + ".indefinite article");
                indefiniteArticles.add(ia == null ? "" : ia);
                String da = Language.get_(GENDERS_SECTION + i + ".definite article");
                definiteArticles.add(da == null ? "" : da);
            }
            if (genders.isEmpty()) {
                Skript.error("No genders defined in the language file!");
                indefiniteArticles.add("");
                definiteArticles.add("");
            }
            if ((dpa = Language.get_("genders.plural.definite article")) == null) {
                Skript.error("Missing entry 'genders.plural.definite article' in the language file!");
            }
            definitePluralArticle = dpa == null ? "" : dpa;
        }, Language.LanguageListenerPriority.EARLIEST);
    }

    public record PluralPair(String singular, String plural) {
    }
}

