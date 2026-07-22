/*
 * Decompiled with CFR 0.152.
 */
package com.btk5h.skriptmirror.util;

import java.util.Arrays;

public class StringSimilarity {
    public static Result compare(String left, String right, int threshold) {
        String first = left;
        String second = right;
        if (first == null || second == null) {
            throw new IllegalArgumentException("CharSequences must not be null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must not be negative");
        }
        int n = first.length();
        int m = second.length();
        if (n == 0) {
            return m <= threshold ? new Result(left, right, m) : null;
        }
        if (m == 0) {
            return n <= threshold ? new Result(left, right, n) : null;
        }
        if (n > m) {
            String tmp = first;
            first = second;
            second = tmp;
            n = m;
            m = second.length();
        }
        if (m - n > threshold) {
            return null;
        }
        int[] p = new int[n + 1];
        int[] d = new int[n + 1];
        int boundary = Math.min(n, threshold) + 1;
        for (int i = 0; i < boundary; ++i) {
            p[i] = i;
        }
        Arrays.fill(p, boundary, p.length, Integer.MAX_VALUE);
        Arrays.fill(d, Integer.MAX_VALUE);
        for (int j = 1; j <= m; ++j) {
            int max;
            char secondJ = second.charAt(j - 1);
            d[0] = j;
            int min = Math.max(1, j - threshold);
            int n2 = max = j > Integer.MAX_VALUE - threshold ? n : Math.min(n, j + threshold);
            if (min > 1) {
                d[min - 1] = Integer.MAX_VALUE;
            }
            for (int i = min; i <= max; ++i) {
                d[i] = first.charAt(i - 1) == secondJ ? p[i - 1] : 1 + Math.min(Math.min(d[i - 1], p[i]), p[i - 1]);
            }
            int[] tempD = p;
            p = d;
            d = tempD;
        }
        if (p[n] <= threshold) {
            return new Result(left, right, p[n]);
        }
        return null;
    }

    public static class Result
    implements Comparable<Result> {
        private final String left;
        private final String right;
        private final int editDistance;

        private Result(String left, String right, int editDistance) {
            this.left = left;
            this.right = right;
            this.editDistance = editDistance;
        }

        public String getLeft() {
            return this.left;
        }

        public String getRight() {
            return this.right;
        }

        public int getEditDistance() {
            return this.editDistance;
        }

        @Override
        public int compareTo(Result o) {
            return this.getEditDistance() - o.getEditDistance();
        }
    }
}

