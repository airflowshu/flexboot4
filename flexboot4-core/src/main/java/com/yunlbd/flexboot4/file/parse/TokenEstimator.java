package com.yunlbd.flexboot4.file.parse;

public final class TokenEstimator {

    private TokenEstimator() {
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int len = text.length();
        int cjk = 0;
        int whitespace = 0;
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                whitespace++;
            }
            if (isCjk(ch)) {
                cjk++;
            }
        }
        int nonWhitespace = Math.max(0, len - whitespace);
        // 粗略估算：中文/日韩字符约 1 字 ≈ 1 token；英文约 4 字 ≈ 1 token
        int latin = Math.max(0, nonWhitespace - cjk);
        int estimated = cjk + (latin + 3) / 4;
        return Math.max(estimated, 1);
    }

    private static boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO;
    }
}

