package it.unipv.ir.preprocessing;

import java.util.ArrayList;
import java.util.List;

/**
 * Porter Stemmer (1980) implemented from scratch in Java.
 *
 * Reference: M.F. Porter, "An algorithm for suffix stripping",
 * Program, 14(3), 130-137, July 1980.
 *
 * The algorithm reduces English words to their morphological stem
 * through a sequence of rule-based suffix-stripping steps (1a, 1b,
 * 1c, 2, 3, 4, 5a, 5b). This is the standard choice for English IR.
 */
public class PorterStemmer {

    public List<String> stemAll(List<String> tokens) {
        List<String> result = new ArrayList<>(tokens.size());
        for (String t : tokens) result.add(stem(t));
        return result;
    }

    public String stem(String word) {
        if (word == null || word.length() <= 2) return word;
        char[] b = word.toCharArray();
        int k = b.length - 1;
        if (k <= 1) return word;

        b = step1a(b, k); k = b.length - 1;
        b = step1b(b, k); k = b.length - 1;
        b = step1c(b, k); k = b.length - 1;
        b = step2(b, k);  k = b.length - 1;
        b = step3(b, k);  k = b.length - 1;
        b = step4(b, k);  k = b.length - 1;
        b = step5a(b, k); k = b.length - 1;
        b = step5b(b, k);

        return new String(b);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** True if b[i] is a consonant. */
    private boolean cons(char[] b, int i) {
        switch (b[i]) {
            case 'a': case 'e': case 'i': case 'o': case 'u': return false;
            case 'y': return i == 0 || !cons(b, i - 1);
            default:  return true;
        }
    }

    /**
     * m() measures the number of consonant sequences between 0 and j.
     * if C denotes a sequence of consonants and V a sequence of vowels:
     *   [C](VC){m}[V] => measure m
     */
    private int m(char[] b, int j) {
        int n = 0;
        int i = 0;
        while (true) {
            if (i > j) return n;
            if (!cons(b, i)) break;
            i++;
        }
        i++;
        while (true) {
            while (true) {
                if (i > j) return n;
                if (cons(b, i)) break;
                i++;
            }
            i++;
            n++;
            while (true) {
                if (i > j) return n;
                if (!cons(b, i)) break;
                i++;
            }
            i++;
        }
    }

    /** Does b[0..j] contain a vowel? */
    private boolean vowelInStem(char[] b, int j) {
        for (int i = 0; i <= j; i++)
            if (!cons(b, i)) return true;
        return false;
    }

    /** b[j-1] and b[j] are double consonants? */
    private boolean doublec(char[] b, int j) {
        if (j < 1) return false;
        if (b[j] != b[j - 1]) return false;
        return cons(b, j);
    }

    /**
     * cvc(i) is true if b[i-2 i-1 i] is consonant-vowel-consonant,
     * and also if the second c is not w, x or y. Used in step 1b and 2.
     */
    private boolean cvc(char[] b, int i) {
        if (i < 2 || !cons(b, i) || cons(b, i - 1) || !cons(b, i - 2))
            return false;
        int ch = b[i];
        return ch != 'w' && ch != 'x' && ch != 'y';
    }

    private boolean ends(char[] b, String s) {
        int l = s.length();
        int o = b.length - l;
        if (o < 0) return false;
        for (int i = 0; i < l; i++)
            if (b[o + i] != s.charAt(i)) return false;
        return true;
    }

    /** Set b to b[0..j] + s. */
    private char[] setto(char[] b, int j, String s) {
        char[] nb = new char[j + 1 + s.length()];
        System.arraycopy(b, 0, nb, 0, j + 1);
        for (int i = 0; i < s.length(); i++) nb[j + 1 + i] = s.charAt(i);
        return nb;
    }

    // -------------------------------------------------------------------------
    // Steps
    // -------------------------------------------------------------------------

    private char[] step1a(char[] b, int k) {
        if (ends(b, "sses"))  return setto(b, k - 4, "ss");
        if (ends(b, "ies"))   return setto(b, k - 3, "i");
        if (ends(b, "ss"))    return b;
        if (ends(b, "s"))     return setto(b, k - 1, "");
        return b;
    }

    private char[] step1b(char[] b, int k) {
        if (ends(b, "eed")) {
            int j = k - 3;
            if (m(b, j) > 0) return setto(b, k - 1, "");
            return b;
        }
        boolean flag = false;
        if (ends(b, "ed")) {
            int j = k - 2;
            if (vowelInStem(b, j)) { b = setto(b, j, ""); flag = true; }
        } else if (ends(b, "ing")) {
            int j = k - 3;
            if (vowelInStem(b, j)) { b = setto(b, j, ""); flag = true; }
        }
        if (flag) {
            k = b.length - 1;
            if      (ends(b, "at")) return setto(b, k - 2, "ate");
            else if (ends(b, "bl")) return setto(b, k - 2, "ble");
            else if (ends(b, "iz")) return setto(b, k - 2, "ize");
            else if (doublec(b, k)) {
                int ch = b[k];
                if (ch != 'l' && ch != 's' && ch != 'z')
                    return setto(b, k - 1, "");
            } else if (m(b, k) == 1 && cvc(b, k)) {
                return setto(b, k, "e");
            }
        }
        return b;
    }

    private char[] step1c(char[] b, int k) {
        if (ends(b, "y") && vowelInStem(b, k - 1))
            return setto(b, k - 1, "i");
        return b;
    }

    private char[] step2(char[] b, int k) {
        if (k < 2) return b;
        switch (b[k - 1]) {
            case 'a':
                if (ends(b,"ational") && m(b,k-7)>0) return setto(b,k-7,"ate");
                if (ends(b,"tional")  && m(b,k-6)>0) return setto(b,k-6,"tion");
                break;
            case 'c':
                if (ends(b,"enci") && m(b,k-4)>0) return setto(b,k-4,"ence");
                if (ends(b,"anci") && m(b,k-4)>0) return setto(b,k-4,"ance");
                break;
            case 'e':
                if (ends(b,"izer") && m(b,k-4)>0) return setto(b,k-4,"ize");
                break;
            case 'l':
                if (ends(b,"abli") && m(b,k-4)>0) return setto(b,k-4,"able");
                if (ends(b,"alli") && m(b,k-4)>0) return setto(b,k-4,"al");
                if (ends(b,"entli")&& m(b,k-5)>0) return setto(b,k-5,"ent");
                if (ends(b,"eli")  && m(b,k-3)>0) return setto(b,k-3,"e");
                if (ends(b,"ousli")&& m(b,k-5)>0) return setto(b,k-5,"ous");
                break;
            case 'o':
                if (ends(b,"ization") && m(b,k-7)>0) return setto(b,k-7,"ize");
                if (ends(b,"ation")   && m(b,k-5)>0) return setto(b,k-5,"ate");
                if (ends(b,"ator")    && m(b,k-4)>0) return setto(b,k-4,"ate");
                break;
            case 's':
                if (ends(b,"alism")   && m(b,k-5)>0) return setto(b,k-5,"al");
                if (ends(b,"iveness") && m(b,k-7)>0) return setto(b,k-7,"ive");
                if (ends(b,"fulness") && m(b,k-7)>0) return setto(b,k-7,"ful");
                if (ends(b,"ousness") && m(b,k-7)>0) return setto(b,k-7,"ous");
                break;
            case 't':
                if (ends(b,"aliti") && m(b,k-5)>0) return setto(b,k-5,"al");
                if (ends(b,"iviti") && m(b,k-5)>0) return setto(b,k-5,"ive");
                if (ends(b,"biliti")&& m(b,k-6)>0) return setto(b,k-6,"ble");
                break;
        }
        return b;
    }

    private char[] step3(char[] b, int k) {
        switch (b[k]) {
            case 'e':
                if (ends(b,"icate") && m(b,k-5)>0) return setto(b,k-5,"ic");
                if (ends(b,"ative") && m(b,k-5)>0) return setto(b,k-5,"");
                if (ends(b,"alize") && m(b,k-5)>0) return setto(b,k-5,"al");
                break;
            case 'i':
                if (ends(b,"iciti") && m(b,k-5)>0) return setto(b,k-5,"ic");
                break;
            case 'l':
                if (ends(b,"ical") && m(b,k-4)>0) return setto(b,k-4,"ic");
                if (ends(b,"ful")  && m(b,k-3)>0) return setto(b,k-3,"");
                break;
            case 's':
                if (ends(b,"ness") && m(b,k-4)>0) return setto(b,k-4,"");
                break;
        }
        return b;
    }

    private char[] step4(char[] b, int k) {
        if (k < 2) return b;
        switch (b[k - 1]) {
            case 'a': if (ends(b,"al")   && m(b,k-2)>1) return setto(b,k-2,""); break;
            case 'c': if (ends(b,"ance") && m(b,k-4)>1) return setto(b,k-4,"");
                      if (ends(b,"ence") && m(b,k-4)>1) return setto(b,k-4,""); break;
            case 'e': if (ends(b,"er")   && m(b,k-2)>1) return setto(b,k-2,""); break;
            case 'i': if (ends(b,"ic")   && m(b,k-2)>1) return setto(b,k-2,""); break;
            case 'l': if (ends(b,"able") && m(b,k-4)>1) return setto(b,k-4,"");
                      if (ends(b,"ible") && m(b,k-4)>1) return setto(b,k-4,""); break;
            case 'n': if (ends(b,"ant")  && m(b,k-3)>1) return setto(b,k-3,"");
                      if (ends(b,"ement")&& m(b,k-5)>1) return setto(b,k-5,"");
                      if (ends(b,"ment") && m(b,k-4)>1) return setto(b,k-4,"");
                      if (ends(b,"ent")  && m(b,k-3)>1) return setto(b,k-3,""); break;
            case 'o': if (ends(b,"ion")  && m(b,k-3)>1) {
                          char c = b[k-3];
                          if (c == 's' || c == 't') return setto(b,k-3,"");
                      }
                      if (ends(b,"ou")   && m(b,k-2)>1) return setto(b,k-2,""); break;
            case 's': if (ends(b,"ism")  && m(b,k-3)>1) return setto(b,k-3,""); break;
            case 't': if (ends(b,"ate")  && m(b,k-3)>1) return setto(b,k-3,"");
                      if (ends(b,"iti")  && m(b,k-3)>1) return setto(b,k-3,""); break;
            case 'u': if (ends(b,"ous")  && m(b,k-3)>1) return setto(b,k-3,""); break;
            case 'v': if (ends(b,"ive")  && m(b,k-3)>1) return setto(b,k-3,""); break;
            case 'z': if (ends(b,"ize")  && m(b,k-3)>1) return setto(b,k-3,""); break;
        }
        return b;
    }

    private char[] step5a(char[] b, int k) {
        if (ends(b, "e")) {
            int j = k - 1;
            int mm = m(b, j);
            if (mm > 1 || (mm == 1 && !cvc(b, j)))
                return setto(b, j, "");
        }
        return b;
    }

    private char[] step5b(char[] b, int k) {
        if (m(b, k) > 1 && doublec(b, k) && b[k] == 'l')
            return setto(b, k - 1, "");
        return b;
    }
}
