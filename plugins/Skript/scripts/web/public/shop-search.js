(() => {
    'use strict';

    const KEYBOARD_NEIGHBORS = {
        '`': ['1', '~'],
        '1': ['`', '2', 'q'],
        '2': ['1', '3', 'q', 'w'],
        '3': ['2', '4', 'w', 'e'],
        '4': ['3', '5', 'e', 'r'],
        '5': ['4', '6', 'r', 't'],
        '6': ['5', '7', 't', 'y'],
        '7': ['6', '8', 'y', 'u'],
        '8': ['7', '9', 'u', 'i'],
        '9': ['8', '0', 'i', 'o'],
        '0': ['9', '-', 'o', 'p'],
        '-': ['0', '=', 'p', '['],
        '=': ['-', '[', ']'],
        q: ['1', '2', 'w', 'a'],
        w: ['q', '2', '3', 'e', 'a', 's'],
        e: ['w', '3', '4', 'r', 's', 'd'],
        r: ['e', '4', '5', 't', 'd', 'f'],
        t: ['r', '5', '6', 'y', 'f', 'g'],
        y: ['t', '6', '7', 'u', 'g', 'h'],
        u: ['y', '7', '8', 'i', 'h', 'j'],
        i: ['u', '8', '9', 'o', 'j', 'k'],
        o: ['i', '9', '0', 'p', 'k', 'l'],
        p: ['o', '0', '-', '[', 'l', ';'],
        a: ['q', 'w', 's', 'z'],
        s: ['a', 'w', 'e', 'd', 'z', 'x'],
        d: ['s', 'e', 'r', 'f', 'x', 'c'],
        f: ['d', 'r', 't', 'g', 'c', 'v'],
        g: ['f', 't', 'y', 'h', 'v', 'b'],
        h: ['g', 'y', 'u', 'j', 'b', 'n'],
        j: ['h', 'u', 'i', 'k', 'n', 'm'],
        k: ['j', 'i', 'o', 'l', 'm', ','],
        l: ['k', 'o', 'p', ';', ',', '.'],
        z: ['a', 's', 'x'],
        x: ['z', 's', 'd', 'c'],
        c: ['x', 'd', 'f', 'v'],
        v: ['c', 'f', 'g', 'b'],
        b: ['v', 'g', 'h', 'n'],
        n: ['b', 'h', 'j', 'm'],
        m: ['n', 'j', 'k', ','],
        '[': ['p', '-', '=', ']', ';', "'"],
        ']': ['[', '=', '\\', "'"],
        '\\': [']', "'"],
        ';': ['p', '[', 'l', "'", '/'],
        "'": [';', '[', ']', '/'],
        ',': ['m', 'k', 'l', '.'],
        '.': [',', 'l', ';', '/'],
        '/': ['.', ';', "'"]
    };

    const KEYBOARD_CHAR_SET_CACHE = new Map();

    /** 忽略拼音输入法误触的单引号分隔（如 指南针 → z'n'z、co'm） */
    function normalizeSearchText(value) {
        return String(value || '')
            .toLowerCase()
            .replace(/[''`´′＇]/g, '');
    }

    function compactSearchText(value) {
        return normalizeSearchText(value).replace(/[\s_'`´′＇_-]+/g, '');
    }

    function getKeyboardCharSet(ch) {
        if (KEYBOARD_CHAR_SET_CACHE.has(ch)) {
            return KEYBOARD_CHAR_SET_CACHE.get(ch);
        }
        const set = new Set([ch]);
        const neighbors = KEYBOARD_NEIGHBORS[ch];
        if (neighbors) {
            neighbors.forEach((n) => set.add(n));
        }
        KEYBOARD_CHAR_SET_CACHE.set(ch, set);
        return set;
    }

    /** 同一键或 QWERTY 邻键（仅拉丁字母/数字/符号） */
    function charsKeyboardClose(a, b) {
        if (a === b) return true;
        if (!a || !b || a.length !== 1 || b.length !== 1) return false;
        if (!/^[a-z0-9`\-=[\]\\;',./]$/.test(a) || !/^[a-z0-9`\-=[\]\\;',./]$/.test(b)) {
            return false;
        }
        return getKeyboardCharSet(a).has(b);
    }

    /** 连续子串：每位为原字符或键盘邻键 */
    function keyboardFuzzyIncludes(haystack, needle) {
        if (!needle) return true;
        if (!haystack || needle.length > haystack.length) return false;
        if (needle.length < 2) {
            return haystack.includes(needle);
        }

        const limit = haystack.length - needle.length;
        for (let start = 0; start <= limit; start += 1) {
            let ok = true;
            for (let i = 0; i < needle.length; i += 1) {
                if (!charsKeyboardClose(needle[i], haystack[start + i])) {
                    ok = false;
                    break;
                }
            }
            if (ok) return true;
        }
        return false;
    }

    function prefixKeyboardMatch(word, prefix) {
        if (!prefix) return true;
        if (!word || prefix.length > word.length) return false;
        for (let i = 0; i < prefix.length; i += 1) {
            if (!charsKeyboardClose(prefix[i], word[i])) return false;
        }
        return true;
    }

    function normalizePinyinNasal(value) {
        return normalizeSearchText(value)
            .replace(/\s+/g, '')
            .replace(/zh/g, 'z')
            .replace(/ch/g, 'c')
            .replace(/sh/g, 's')
            .replace(/ang/g, 'an')
            .replace(/eng/g, 'en')
            .replace(/ing/g, 'in');
    }

    function pinyinPrefixComboMatch(syllables, query) {
        const parts = syllables.map((part) => String(part || '').toLowerCase()).filter(Boolean);
        const q = normalizeSearchText(query).replace(/\s+/g, '');
        if (!parts.length || !q) return false;

        const canMatchFrom = (start) => {
            const memo = new Set();
            const dfs = (idx, rest) => {
                if (!rest) return true;
                if (idx >= parts.length) return false;
                const key = `${idx}|${rest}`;
                if (memo.has(key)) return false;
                const part = parts[idx];
                const maxLen = Math.min(part.length, rest.length);
                for (let len = 1; len <= maxLen; len += 1) {
                    if (rest.startsWith(part.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                        return true;
                    }
                }
                memo.add(key);
                return false;
            };
            return dfs(start, q);
        };

        for (let i = 0; i < parts.length; i += 1) {
            if (canMatchFrom(i)) return true;
        }
        return false;
    }

    const LETTER_SORT_CACHE = new Map();

    function getItemSortLetter(item) {
        if (!item || !item.id) return '';
        if (LETTER_SORT_CACHE.has(item.id)) {
            return LETTER_SORT_CACHE.get(item.id);
        }

        let letter = '';
        const name = String(item.name || '').trim();
        const firstChar = name.charAt(0);
        if (/[a-zA-Z]/.test(firstChar)) {
            letter = firstChar.toUpperCase();
        } else if (typeof pinyinPro !== 'undefined' && name) {
            try {
                const initial = pinyinPro.pinyin(name, {
                    pattern: 'first',
                    toneType: 'none',
                    type: 'array'
                });
                const ch = Array.isArray(initial) && initial[0]
                    ? String(initial[0]).charAt(0).toUpperCase()
                    : '';
                if (/[A-Z]/.test(ch)) {
                    letter = ch;
                } else {
                    const initials = pinyinPro.pinyin(name, {
                        pattern: 'initial',
                        toneType: 'none',
                        type: 'string'
                    }).replace(/\s+/g, '');
                    const fromInitial = initials.charAt(0).toUpperCase();
                    if (/[A-Z]/.test(fromInitial)) {
                        letter = fromInitial;
                    }
                }
            } catch (e) {
                // ignore
            }
        }

        if (!letter) {
            const idFirst = String(item.id).replace(/^minecraft:/i, '').charAt(0).toUpperCase();
            if (/[A-Z]/.test(idFirst)) {
                letter = idFirst;
            }
        }

        LETTER_SORT_CACHE.set(item.id, letter);
        return letter;
    }

    function splitSearchWords(value) {
        return normalizeSearchText(value)
            .split(/[\s_'`´′＇_-]+/)
            .map((part) => part.trim())
            .filter(Boolean);
    }

    function prefixComboMatch(parts, query) {
        const words = parts.map((part) => String(part || '').toLowerCase()).filter(Boolean);
        const q = normalizeSearchText(query).replace(/[\s_'`´′＇_-]+/g, '');
        if (!words.length || !q) return false;

        const canMatchFrom = (start) => {
            const memo = new Set();
            const dfs = (idx, rest) => {
                if (!rest) return true;
                if (idx >= words.length) return false;
                const key = `${idx}|${rest}`;
                if (memo.has(key)) return false;
                const word = words[idx];
                const maxLen = Math.min(word.length, rest.length);
                for (let len = 1; len <= maxLen; len += 1) {
                    if (rest.startsWith(word.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                        return true;
                    }
                }
                memo.add(key);
                return false;
            };
            return dfs(start, q);
        };

        for (let i = 0; i < words.length; i += 1) {
            if (canMatchFrom(i)) return true;
        }
        return false;
    }

    function keyboardPrefixComboMatch(parts, query) {
        const words = parts.map((part) => String(part || '').toLowerCase()).filter(Boolean);
        const q = compactSearchText(query);
        if (!words.length || !q || q.length < 2) return false;

        const canMatchFrom = (start) => {
            const memo = new Set();
            const dfs = (idx, rest) => {
                if (!rest) return true;
                if (idx >= words.length) return false;
                const key = `${idx}|${rest}`;
                if (memo.has(key)) return false;
                const word = words[idx];
                const maxLen = Math.min(word.length, rest.length);
                for (let len = 1; len <= maxLen; len += 1) {
                    if (prefixKeyboardMatch(word, rest.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                        return true;
                    }
                }
                memo.add(key);
                return false;
            };
            return dfs(start, q);
        };

        for (let i = 0; i < words.length; i += 1) {
            if (canMatchFrom(i)) return true;
        }
        return false;
    }

    function keyboardPinyinPrefixComboMatch(syllables, query) {
        const parts = syllables.map((part) => String(part || '').toLowerCase()).filter(Boolean);
        const q = compactSearchText(query);
        if (!parts.length || !q || q.length < 2) return false;

        const canMatchFrom = (start) => {
            const memo = new Set();
            const dfs = (idx, rest) => {
                if (!rest) return true;
                if (idx >= parts.length) return false;
                const key = `${idx}|${rest}`;
                if (memo.has(key)) return false;
                const part = parts[idx];
                const maxLen = Math.min(part.length, rest.length);
                for (let len = 1; len <= maxLen; len += 1) {
                    if (prefixKeyboardMatch(part, rest.slice(0, len)) && dfs(idx + 1, rest.slice(len))) {
                        return true;
                    }
                }
                memo.add(key);
                return false;
            };
            return dfs(start, q);
        };

        for (let i = 0; i < parts.length; i += 1) {
            if (canMatchFrom(i)) return true;
        }
        return false;
    }

    function textMatchesSearchQuery(text, query) {
        const lower = String(text || '').toLowerCase();
        const compactText = compactSearchText(lower);
        const compactQuery = compactSearchText(query);
        if (!compactQuery) return true;

        if (lower.includes(query) || compactText.includes(compactQuery)) return true;
        if (prefixComboMatch(splitSearchWords(lower), query)) return true;

        if (compactQuery.length < 2) return false;
        if (keyboardFuzzyIncludes(compactText, compactQuery)) return true;
        return keyboardPrefixComboMatch(splitSearchWords(lower), query);
    }

    function itemMatchesSearchQuery(item, query) {
        const compactQuery = compactSearchText(query);
        if (!compactQuery) return true;

        const itemNameLower = item.name.toLowerCase();
        const itemIdLower = item.id.toLowerCase();
        if (textMatchesSearchQuery(itemNameLower, query) || textMatchesSearchQuery(itemIdLower, query)) {
            return true;
        }

        if (typeof pinyinPro === 'undefined') return false;

        try {
            const namePinyinSpaced = pinyinPro.pinyin(item.name, { toneType: 'none', type: 'string' }).toLowerCase();
            const namePinyinSyllables = namePinyinSpaced.split(/\s+/).filter(Boolean);
            const namePinyin = namePinyinSpaced.replace(/\s+/g, '');
            const namePinyinInitial = pinyinPro.pinyin(item.name, { pattern: 'initial', toneType: 'none', type: 'string' }).toLowerCase().replace(/\s+/g, '');
            const namePinyinComputedInitial = namePinyinSyllables
                .map((part) => part.charAt(0))
                .join('');
            const fuzzyNamePinyin = normalizePinyinNasal(namePinyin);
            const fuzzyNamePinyinInitial = normalizePinyinNasal(namePinyinInitial);
            const fuzzyNamePinyinComputedInitial = normalizePinyinNasal(namePinyinComputedInitial);
            const fuzzyNamePinyinSyllables = namePinyinSyllables.map(normalizePinyinNasal);
            const fuzzyQuery = normalizePinyinNasal(query);

            if (textMatchesSearchQuery(namePinyin, query)
                || textMatchesSearchQuery(namePinyinInitial, query)
                || textMatchesSearchQuery(namePinyinComputedInitial, query)
                || textMatchesSearchQuery(fuzzyNamePinyin, fuzzyQuery)
                || textMatchesSearchQuery(fuzzyNamePinyinInitial, fuzzyQuery)
                || textMatchesSearchQuery(fuzzyNamePinyinComputedInitial, fuzzyQuery)) {
                return true;
            }

            if (pinyinPrefixComboMatch(namePinyinSyllables, query)
                || pinyinPrefixComboMatch(fuzzyNamePinyinSyllables, fuzzyQuery)) {
                return true;
            }

            if (compactQuery.length >= 2) {
                return keyboardPinyinPrefixComboMatch(namePinyinSyllables, query)
                    || keyboardPinyinPrefixComboMatch(fuzzyNamePinyinSyllables, fuzzyQuery);
            }
        } catch (e) {
            // ignore
        }

        return false;
    }

    globalThis.McShopSearch = {
        normalizeSearchText,
        getItemSortLetter,
        itemMatchesSearchQuery,
        reset: () => LETTER_SORT_CACHE.clear()
    };
})();
