package it.unipv.ir.spelling;

/**
 * Levenshtein edit distance between two strings.
 *
 * Operations (each cost 1):
 *   - Insert a character
 *   - Delete a character
 *   - Substitute a character
 *
 * Implemented using the standard O(m×n) dynamic programming algorithm.
 * Space is O(min(m,n)) using the two-row rolling optimisation.
 *
 * Reference: Levenshtein, V.I. (1966), "Binary codes capable of
 * correcting deletions, insertions, and reversals."
 */
public class EditDistance {

    /**
     * Compute the edit distance between s and t.
     * Returns 0 if s.equals(t), or Integer.MAX_VALUE if either is null.
     */
    public int distance(String s, String t) {
        if (s == null || t == null) return Integer.MAX_VALUE;
        if (s.equals(t))           return 0;
        if (s.isEmpty())           return t.length();
        if (t.isEmpty())           return s.length();

        // Ensure s is the shorter string for space optimisation
        if (s.length() > t.length()) { String tmp = s; s = t; t = tmp; }

        int m = s.length();
        int n = t.length();

        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        // Base case: distance from empty string
        for (int i = 0; i <= m; i++) prev[i] = i;

        for (int j = 1; j <= n; j++) {
            curr[0] = j;
            for (int i = 1; i <= m; i++) {
                int cost = s.charAt(i - 1) == t.charAt(j - 1) ? 0 : 1;
                curr[i] = Math.min(
                    Math.min(curr[i - 1] + 1,   // insert
                             prev[i]     + 1),   // delete
                    prev[i - 1]          + cost  // substitute
                );
            }
            // Swap rows
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }

    /**
     * Convenience: check if two strings are within maxDistance of each other.
     * Slightly faster in practice since we can bail early once the minimum
     * possible distance for the remaining rows exceeds maxDistance.
     */
    public boolean withinDistance(String s, String t, int maxDistance) {
        return distance(s, t) <= maxDistance;
    }
}
