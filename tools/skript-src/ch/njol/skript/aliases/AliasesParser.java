/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.aliases;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.AliasesProvider;
import ch.njol.skript.aliases.InvalidMinecraftIdException;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.localization.ArgsMessage;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import ch.njol.util.NonNullPair;
import ch.njol.util.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class AliasesParser {
    private static final Message m_empty_name = new Message("aliases.empty name");
    private static final ArgsMessage m_invalid_variation_section = new ArgsMessage("aliases.invalid variation section");
    private static final Message m_unexpected_section = new Message("aliases.unexpected section");
    private static final Message m_useless_variation = new Message("aliases.useless variation");
    private static final ArgsMessage m_not_enough_brackets = new ArgsMessage("aliases.not enough brackets");
    private static final ArgsMessage m_too_many_brackets = new ArgsMessage("aliases.too many brackets");
    private static final ArgsMessage m_unknown_variation = new ArgsMessage("aliases.unknown variation");
    private static final ArgsMessage m_invalid_minecraft_id = new ArgsMessage("aliases.invalid minecraft id");
    private static final Message m_empty_alias = new Message("aliases.empty alias");
    protected final AliasesProvider provider;
    private final Map<String, Function<String, Boolean>> conditions;

    public AliasesParser(AliasesProvider provider) {
        this.provider = provider;
        this.conditions = new HashMap<String, Function<String, Boolean>>();
    }

    public void load(SectionNode root) {
        Skript.debug("Loading aliases node " + root.getKey() + " from " + root.getConfig().getFileName() + " (" + this.provider.getAliasCount() + " aliases loaded)");
        for (Node node : root) {
            String key = node.getKey();
            if (key == null) {
                Skript.error(m_empty_name.toString());
                continue;
            }
            if (node instanceof SectionNode) {
                AliasesProvider.VariationGroup vars = this.loadVariations((SectionNode)node);
                if (vars != null) {
                    String groupName = node.getKey();
                    assert (groupName != null);
                    this.provider.addVariationGroup(groupName, vars);
                    continue;
                }
                Skript.error(m_invalid_variation_section.toString(key));
                continue;
            }
            if (!(node instanceof EntryNode)) {
                Skript.error(m_unexpected_section.toString());
                continue;
            }
            if (this.conditions.containsKey(key)) {
                boolean success = this.conditions.get(key).apply(((EntryNode)node).getValue());
                if (success) continue;
                Skript.debug("Condition " + key + " was NOT met; not loading more");
                return;
            }
            String value = ((EntryNode)node).getValue();
            this.loadAlias(key, value);
        }
    }

    protected Map<String, String> parseBlockStates(String input) {
        HashMap<String, String> parsed = new HashMap<String, String>();
        int pos = 0;
        while (pos != -1) {
            String pair;
            int comma = input.indexOf(44, pos);
            if (comma == -1) {
                pair = input.substring(pos);
                pos = -1;
            } else {
                pair = input.substring(pos, comma);
                pos = comma + 1;
            }
            String[] parts = pair.split("=");
            parsed.put(parts[0], parts[1]);
        }
        return parsed;
    }

    @Nullable
    protected AliasesProvider.VariationGroup loadVariations(SectionNode root) {
        String name = root.getKey();
        assert (name != null);
        if (!name.startsWith("{") || !name.endsWith("}")) {
            return null;
        }
        AliasesProvider.VariationGroup vars = new AliasesProvider.VariationGroup();
        for (Node node : root) {
            String pattern = node.getKey();
            assert (pattern != null);
            List<String> keys = this.parseKeyPattern(pattern);
            AliasesProvider.Variation var = this.parseVariation(((EntryNode)node).getValue());
            boolean useful = false;
            for (String key : keys) {
                assert (key != null);
                if (key.equals("{default}")) {
                    key = "";
                    useful = true;
                }
                vars.put(key, var);
            }
            if (useful || var.getId() != null || !var.getTags().isEmpty() || !var.getBlockStates().isEmpty()) continue;
            Skript.warning(m_useless_variation.toString());
        }
        return vars;
    }

    protected AliasesProvider.Variation parseVariation(String item) {
        Map<String, String> blockStates;
        String typeName;
        Map<String, Object> tags;
        Object id;
        String trimmed = item.trim();
        assert (trimmed != null);
        item = trimmed;
        int firstBracket = item.indexOf(123);
        if (firstBracket == -1) {
            id = item;
            tags = new HashMap<String, Object>();
        } else {
            Object json;
            if (firstBracket == 0) {
                throw new AssertionError((Object)("missing space between id and tags in " + item));
            }
            id = item.substring(0, firstBracket - 1);
            int jsonEndIndex = item.indexOf("} [");
            if (jsonEndIndex == -1) {
                json = item.substring(firstBracket);
            } else {
                json = item.substring(firstBracket, jsonEndIndex + 1);
                id = (String)id + item.substring(jsonEndIndex + 2);
            }
            json = "[" + ((String)json).substring(1, ((String)json).length() - 1) + "]";
            tags = Collections.singletonMap("components", json);
        }
        int stateIndex = ((String)id).indexOf(91);
        if (stateIndex != -1) {
            if (stateIndex == 0) {
                throw new AssertionError((Object)("missing id or - in " + (String)id));
            }
            typeName = ((String)id).substring(0, stateIndex);
            String statesInput = ((String)id).substring(stateIndex + 1, ((String)id).length() - 1);
            assert (statesInput != null);
            blockStates = this.parseBlockStates(statesInput);
        } else {
            typeName = id;
            blockStates = new HashMap<String, String>();
        }
        if (typeName.equals("-")) {
            typeName = null;
        }
        return new AliasesProvider.Variation(typeName, typeName == null ? -1 : typeName.indexOf(45), tags, blockStates);
    }

    protected List<String> parseKeyPattern(String name) {
        int c;
        ArrayList<String> versions = new ArrayList<String>();
        boolean simple = true;
        IntStack optionals = new IntStack(4);
        IntStack choices = new IntStack(4);
        for (int i = 0; i < name.length(); i += Character.charCount(c)) {
            int start;
            c = name.codePointAt(i);
            if (c == 91) {
                optionals.push(i);
                simple = false;
                continue;
            }
            if (c == 40) {
                choices.push(i);
                simple = false;
                continue;
            }
            if (c == 93) {
                try {
                    start = optionals.pop();
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    Skript.error(m_too_many_brackets.toString(i, Character.valueOf(']')));
                    return versions;
                }
                versions.addAll(this.parseKeyPattern(name.substring(0, start) + name.substring(start + 1, i) + name.substring(i + 1)));
                versions.addAll(this.parseKeyPattern(name.substring(0, start) + name.substring(i + 1)));
                continue;
            }
            if (c != 41) continue;
            try {
                start = choices.pop();
            }
            catch (ArrayIndexOutOfBoundsException e) {
                Skript.error(m_too_many_brackets.toString(i, Character.valueOf(')')));
                return versions;
            }
            int optionStart = start;
            int nested = 0;
            for (int j = start + 1; j < i; j += Character.charCount(c)) {
                c = name.codePointAt(j);
                if (c == 40 || c == 91) {
                    ++nested;
                    continue;
                }
                if (c == 41 || c == 93) {
                    --nested;
                    continue;
                }
                if (c != 124 || nested != 0) continue;
                versions.addAll(this.parseKeyPattern(name.substring(0, start) + name.substring(optionStart + 1, j) + name.substring(i + 1)));
                optionStart = j;
            }
            assert (nested == 0);
            versions.addAll(this.parseKeyPattern(name.substring(0, start) + name.substring(optionStart + 1, i) + name.substring(i + 1)));
        }
        if (!optionals.isEmpty() || !choices.isEmpty()) {
            int errorStart = !optionals.isEmpty() ? optionals.pop() : choices.pop();
            char errorChar = (char)name.codePointAt(errorStart);
            Skript.error(m_not_enough_brackets.toString(errorStart, Character.valueOf(errorChar)));
            optionals.clear();
            choices.clear();
            return versions;
        }
        if (simple) {
            versions.add(name);
        }
        return versions;
    }

    protected Map<String, AliasesProvider.Variation> parseKeyVariations(String name) {
        int count;
        int incremented;
        PatternSlot slot;
        int varStart = -1;
        int varEnd = 0;
        ArrayList<PatternSlot> slots = new ArrayList<PatternSlot>();
        int i = 0;
        while (i < name.length()) {
            int c = name.codePointAt(i);
            if (c == 123) {
                varStart = i;
                String part = name.substring(varEnd, i);
                assert (part != null);
                slots.add(new PatternSlot(part));
            } else if (c == 125) {
                if (varStart == -1) {
                    Skript.error(m_not_enough_brackets.toString());
                    continue;
                }
                String varName = name.substring(varStart, i + 1);
                assert (varName != null);
                AliasesProvider.VariationGroup vars = this.provider.getVariationGroup(varName);
                if (vars == null) {
                    Skript.error(m_unknown_variation.toString(varName));
                    continue;
                }
                slots.add(new VariationSlot(vars));
                varStart = -1;
                varEnd = i + 1;
            }
            i += Character.charCount(c);
        }
        String part = name.substring(varEnd);
        assert (part != null);
        slots.add(new PatternSlot(part));
        if (varStart != -1) {
            Skript.error(m_not_enough_brackets.toString());
        }
        LinkedHashMap<String, AliasesProvider.Variation> variations = new LinkedHashMap<String, AliasesProvider.Variation>();
        if (slots.size() == 1 && !((slot = (PatternSlot)slots.get(0)) instanceof VariationSlot)) {
            variations.put(this.fixName(name), new AliasesProvider.Variation(null, -1, Collections.emptyMap(), Collections.emptyMap()));
            return variations;
        }
        do {
            count = slots.size();
            incremented = 0;
            StringBuilder pattern = new StringBuilder();
            String id = null;
            int insertPoint = -1;
            HashMap<String, Object> tags = new HashMap<String, Object>();
            HashMap<String, String> states = new HashMap<String, String>();
            for (int i2 = 0; i2 < count; ++i2) {
                PatternSlot slot2 = (PatternSlot)slots.get(i2);
                if (slot2 instanceof VariationSlot) {
                    VariationSlot varSlot = (VariationSlot)slot2;
                    pattern.append(varSlot.getName());
                    AliasesProvider.Variation var = varSlot.getVariation();
                    String varId = var.getId();
                    if (varId != null) {
                        id = varId;
                    }
                    if (var.getInsertPoint() != -1) {
                        insertPoint = var.getInsertPoint();
                    }
                    tags.putAll(var.getTags());
                    states.putAll(var.getBlockStates());
                    if (i2 != incremented || !varSlot.increment()) continue;
                    ++incremented;
                    continue;
                }
                if (i2 == incremented) {
                    ++incremented;
                }
                pattern.append(slot2.content);
            }
            variations.put(this.fixName(pattern.toString()), new AliasesProvider.Variation(id, insertPoint, tags, states));
        } while (incremented != count);
        return variations;
    }

    protected void loadAlias(String name, String data) {
        List<String> patterns = this.parseKeyPattern(name);
        LinkedHashMap<String, AliasesProvider.Variation> variations = new LinkedHashMap<String, AliasesProvider.Variation>();
        for (String pattern : patterns) {
            assert (pattern != null);
            variations.putAll(this.parseKeyVariations(pattern));
        }
        int start = 0;
        int indexStart = 0;
        while (start - 1 != data.length()) {
            String item;
            int comma = StringUtils.indexOfOutsideGroup(data, ',', '{', '}', indexStart);
            if (comma == -1) {
                if (indexStart == 0) {
                    item = data.trim();
                    assert (item != null);
                    this.loadSingleAlias(variations, item);
                    break;
                }
                comma = data.length();
            }
            item = data.substring(start, comma).trim();
            assert (item != null);
            this.loadSingleAlias(variations, item);
            indexStart = start = comma + 1;
        }
    }

    protected NonNullPair<String, String> getAliasPlural(String name) {
        int marker = name.indexOf(166);
        if (marker == -1) {
            String trimmed = name.trim();
            assert (trimmed != null);
            return new NonNullPair<String, String>(trimmed, trimmed);
        }
        int pluralEnd = -1;
        for (int i = marker; i < name.length(); ++i) {
            int c = name.codePointAt(i);
            if (Character.isWhitespace(c)) {
                pluralEnd = i;
                break;
            }
            i += Character.charCount(c);
        }
        if (pluralEnd == -1) {
            String singular = name.substring(0, marker);
            Object plural = singular + name.substring(marker + 1);
            singular = singular.trim();
            plural = ((String)plural).trim();
            assert (singular != null);
            assert (plural != null);
            return new NonNullPair<String, Object>(singular, plural);
        }
        String base = name.substring(0, marker);
        Object singular = base + name.substring(pluralEnd);
        Object plural = base + name.substring(marker + 1);
        singular = ((String)singular).trim();
        plural = ((String)plural).trim();
        assert (singular != null);
        assert (plural != null);
        return new NonNullPair<Object, Object>(singular, plural);
    }

    protected void loadSingleAlias(Map<String, AliasesProvider.Variation> variations, String item) {
        AliasesProvider.Variation base = this.parseVariation(item);
        for (Map.Entry<String, AliasesProvider.Variation> entry : variations.entrySet()) {
            String name = entry.getKey();
            assert (name != null);
            AliasesProvider.Variation var = entry.getValue();
            assert (var != null);
            AliasesProvider.Variation merged = base.merge(var);
            String id = merged.getId();
            if (id == null) {
                Skript.warning(m_empty_alias.toString());
                continue;
            }
            id = id.toLowerCase(Locale.ENGLISH).intern();
            assert (id != null);
            try {
                NonNullPair<String, Integer> plain = Noun.stripGender(name, name);
                NonNullPair<String, String> forms = this.getAliasPlural(plain.getFirst());
                this.provider.addAlias(new AliasesProvider.AliasName(forms.getFirst(), forms.getSecond(), plain.getSecond()), id, merged.getTags(), merged.getBlockStates());
            }
            catch (InvalidMinecraftIdException e) {
                Skript.error(m_invalid_minecraft_id.toString(e.getId()));
            }
        }
    }

    protected String fixName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        int firstNonWhitespace = -1;
        int lastNonWhitespace = -1;
        int lastWhitespace = -1;
        int stripped = 0;
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (c > ' ') {
                int adjustedIndex = i - stripped;
                if (firstNonWhitespace == -1) {
                    firstNonWhitespace = adjustedIndex;
                }
                lastNonWhitespace = adjustedIndex;
            } else {
                int oldLastWhitespace = lastWhitespace;
                lastWhitespace = i;
                if (oldLastWhitespace != -1 && oldLastWhitespace == i - 1 || i < name.length() - 1 && name.charAt(i + 1) == '\u00a6') {
                    ++stripped;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.substring(firstNonWhitespace, lastNonWhitespace + 1);
    }

    public void registerCondition(String name, Function<String, Boolean> condition) {
        this.conditions.put(name, condition);
    }

    private static class IntStack {
        private int[] ints;
        private int pos;

        public IntStack(int capacity) {
            this.ints = new int[capacity];
            this.pos = 0;
        }

        public void push(int value) {
            if (this.pos == this.ints.length - 1) {
                this.enlargeArray();
            }
            this.ints[this.pos++] = value;
        }

        public int pop() {
            return this.ints[--this.pos];
        }

        public boolean isEmpty() {
            return this.pos == 0;
        }

        private void enlargeArray() {
            int[] newArray = new int[this.ints.length * 2];
            System.arraycopy(this.ints, 0, newArray, 0, this.ints.length);
            this.ints = newArray;
        }

        public void clear() {
            this.pos = 0;
        }
    }

    protected static class PatternSlot {
        public final String content;

        public PatternSlot(String content) {
            this.content = content;
        }
    }

    protected static class VariationSlot
    extends PatternSlot {
        public final AliasesProvider.VariationGroup vars;
        private int counter;

        public VariationSlot(AliasesProvider.VariationGroup vars) {
            super("");
            this.vars = vars;
        }

        public String getName() {
            return this.vars.keys.get(this.counter);
        }

        public AliasesProvider.Variation getVariation() {
            return this.vars.values.get(this.counter);
        }

        public boolean increment() {
            ++this.counter;
            if (this.counter == this.vars.keys.size()) {
                this.counter = 0;
                return true;
            }
            return false;
        }
    }
}

