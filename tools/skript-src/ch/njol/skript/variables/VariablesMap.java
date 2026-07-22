/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.variables;

import ch.njol.skript.variables.Variables;
import ch.njol.util.StringUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.util.IndexTrackingTreeMap;

final class VariablesMap {
    static final Comparator<String> VARIABLE_NAME_COMPARATOR = (s1, s2) -> {
        int i;
        char firstChar2;
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        }
        if (s2 == null) {
            return 1;
        }
        int len1 = s1.length();
        int len2 = s2.length();
        char firstChar1 = len1 > 0 ? s1.charAt(0) : (char)'\u0000';
        char c = firstChar2 = len2 > 0 ? s2.charAt(0) : (char)'\u0000';
        if (firstChar1 >= '1' && firstChar1 <= '9' && firstChar2 >= '1' && firstChar2 <= '9') {
            for (i = 1; i < len1 && VariablesMap.isDigit(s1.charAt(i)); ++i) {
            }
            if (i == len1) {
                for (i = 1; i < len2 && VariablesMap.isDigit(s2.charAt(i)); ++i) {
                }
                if (i == len2) {
                    if (len1 != len2) {
                        return len1 - len2;
                    }
                    return s1.compareTo((String)s2);
                }
            }
        }
        i = 0;
        int j = 0;
        boolean lastNumberNegative = false;
        boolean afterDecimalPoint = false;
        while (i < len1 && j < len2) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(j);
            if (VariablesMap.isDigit(c1) && VariablesMap.isDigit(c2)) {
                int isPositive;
                int i2 = StringUtils.findLastDigit(s1, i);
                int j2 = StringUtils.findLastDigit(s2, j);
                int z1 = 0;
                int z2 = 0;
                if (!afterDecimalPoint) {
                    if (c1 == '0') {
                        while (i < i2 - 1 && s1.charAt(i) == '0') {
                            ++i;
                            ++z1;
                        }
                    }
                    if (c2 == '0') {
                        while (j < j2 - 1 && s2.charAt(j) == '0') {
                            ++j;
                            ++z2;
                        }
                    }
                }
                boolean previousNegative = lastNumberNegative;
                lastNumberNegative = i - z1 > 0 && s1.charAt(i - z1 - 1) == '-';
                int n = isPositive = lastNumberNegative | previousNegative ? -1 : 1;
                if (!afterDecimalPoint && i2 - i != j2 - j) {
                    return (i2 - i - (j2 - j)) * isPositive;
                }
                while (i < i2 && j < j2) {
                    char d2;
                    char d1 = s1.charAt(i);
                    if (d1 != (d2 = s2.charAt(j))) {
                        return (d1 - d2) * isPositive;
                    }
                    ++i;
                    ++j;
                }
                if (afterDecimalPoint && i2 - i != j2 - j) {
                    return (i2 - i - (j2 - j)) * isPositive;
                }
                if (z1 != z2) {
                    return (z1 - z2) * isPositive;
                }
                afterDecimalPoint = true;
                continue;
            }
            if (c1 != c2) {
                return c1 - c2;
            }
            if (c1 != '.') {
                lastNumberNegative = false;
                afterDecimalPoint = false;
            }
            ++i;
            ++j;
        }
        if (i < len1) {
            return lastNumberNegative ? -1 : 1;
        }
        if (j < len2) {
            return lastNumberNegative ? 1 : -1;
        }
        return 0;
    };
    final HashMap<String, Object> hashMap = new HashMap();
    final TreeMap<String, Object> treeMap = new TreeMap();

    VariablesMap() {
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    @Nullable
    Object getVariable(String name) {
        if (!name.endsWith("*")) {
            return this.hashMap.get(name);
        }
        String[] split = Variables.splitVariableName(name);
        Map<String, Object> parent = this.treeMap;
        for (int i = 0; i < split.length; ++i) {
            String n = split[i];
            if (n.equals("*")) {
                assert (i == split.length - 1);
                return parent;
            }
            Object childNode = parent.get(n);
            if (childNode == null) {
                return null;
            }
            if (childNode instanceof Map) {
                parent = (Map)childNode;
                assert (i != split.length - 1);
                continue;
            }
            return null;
        }
        return null;
    }

    void setVariable(String name, @Nullable Object value) {
        if (!name.endsWith("*")) {
            if (value == null) {
                this.hashMap.remove(name);
            } else {
                this.hashMap.put(name, value);
            }
        }
        Object[] split = Variables.splitVariableName(name);
        IndexTrackingTreeMap<Object> parent = this.treeMap;
        for (int i = 0; i < split.length; ++i) {
            String childNodeName = split[i];
            IndexTrackingTreeMap childNode = parent.get(childNodeName);
            if (childNode == null) {
                if (i == split.length - 1) {
                    if (value == null) break;
                    ((TreeMap)parent).put((String)childNodeName, value);
                    break;
                }
                if (value == null) break;
                childNode = new IndexTrackingTreeMap((Comparator<? super String>)VARIABLE_NAME_COMPARATOR);
                ((TreeMap)parent).put((String)childNodeName, (Object)childNode);
                parent = childNode;
                continue;
            }
            if (childNode instanceof TreeMap) {
                IndexTrackingTreeMap<Object> childNodeMap = childNode;
                if (i == split.length - 1) {
                    if (value == null) {
                        ((TreeMap)childNodeMap).remove(null);
                        break;
                    }
                    ((TreeMap)childNodeMap).put(null, value);
                    break;
                }
                if (i == split.length - 2 && split[i + 1].equals("*")) {
                    assert (value == null);
                    this.deleteFromHashMap(StringUtils.join(split, "::", 0, i + 1), childNodeMap);
                    Object currentChildValue = childNodeMap.get(null);
                    if (currentChildValue == null) {
                        ((TreeMap)parent).remove(childNodeName);
                        break;
                    }
                    ((TreeMap)parent).put(childNodeName, currentChildValue);
                    break;
                }
                parent = childNodeMap;
                continue;
            }
            if (i == split.length - 1) {
                if (value == null) {
                    ((TreeMap)parent).remove(childNodeName);
                    break;
                }
                ((TreeMap)parent).put((String)childNodeName, value);
                break;
            }
            if (value == null) break;
            IndexTrackingTreeMap<Object> newChildNodeMap = new IndexTrackingTreeMap<Object>(VARIABLE_NAME_COMPARATOR);
            ((TreeMap)newChildNodeMap).put(null, (Object)childNode);
            ((TreeMap)parent).put(childNodeName, (Object)newChildNodeMap);
            parent = newChildNodeMap;
        }
    }

    void deleteFromHashMap(String parent, TreeMap<String, Object> current) {
        for (Map.Entry<String, Object> e : current.entrySet()) {
            if (e.getKey() == null) continue;
            String childName = parent + "::" + e.getKey();
            this.hashMap.remove(childName);
            Object val = e.getValue();
            if (!(val instanceof TreeMap)) continue;
            this.deleteFromHashMap(childName, (TreeMap)val);
        }
    }

    public VariablesMap copy() {
        VariablesMap copy = new VariablesMap();
        copy.hashMap.putAll(this.hashMap);
        TreeMap<String, Object> treeMapCopy = VariablesMap.copyTreeMap(this.treeMap);
        copy.treeMap.putAll(treeMapCopy);
        return copy;
    }

    private static TreeMap<String, Object> copyTreeMap(TreeMap<String, Object> original) {
        IndexTrackingTreeMap<Object> copy = new IndexTrackingTreeMap<Object>(VARIABLE_NAME_COMPARATOR);
        for (Map.Entry<String, Object> child : original.entrySet()) {
            String key = child.getKey();
            TreeMap<String, Object> value = child.getValue();
            if (value instanceof TreeMap) {
                value = VariablesMap.copyTreeMap(value);
            }
            ((TreeMap)copy).put(key, (Object)value);
        }
        return copy;
    }
}

