package it.unipv.ir.preprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Removes stop words from a token list.
 *
 * The stop list is loaded from the classpath resource "stopwords.txt"
 * (one word per line, comments starting with #). This makes it easy
 * to customise without recompiling.
 *
 * Typical English stop words: the, a, an, is, are, was, were, ...
 */
public class StopWordFilter {

    private final Set<String> stopWords;

    /** Load from the default classpath resource stopwords.txt. */
    public StopWordFilter() throws IOException {
        this.stopWords = new HashSet<>();
        loadFromClasspath("stopwords.txt");
    }

    /** Load from a custom set (useful for testing). */
    public StopWordFilter(Set<String> customStopWords) {
        this.stopWords = new HashSet<>(customStopWords);
    }

    /** Remove all stop words from the token list. */
    public List<String> filter(List<String> tokens) {
        List<String> result = new ArrayList<>(tokens.size());
        for (String t : tokens) {
            if (!stopWords.contains(t)) result.add(t);
        }
        return result;
    }

    public boolean isStopWord(String token) {
        return stopWords.contains(token);
    }

    public Set<String> getStopWords() { return stopWords; }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void loadFromClasspath(String resource) throws IOException {
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(resource);
        if (is == null) {
            // Fall back to a minimal built-in list so the system always works
            loadBuiltIn();
            return;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    stopWords.add(line.toLowerCase());
                }
            }
        }
    }

    private void loadBuiltIn() {
        String[] builtIn = {
            "a","an","the","and","or","but","in","on","at","to","for",
            "of","with","by","from","is","are","was","were","be","been",
            "being","have","has","had","do","does","did","will","would",
            "could","should","may","might","shall","can","not","no","nor",
            "so","yet","both","either","neither","each","every","all",
            "any","few","more","most","other","some","such","than","too",
            "very","just","as","it","its","this","that","these","those",
            "he","she","they","we","you","i","my","your","his","her","our",
            "their","what","which","who","whom","how","when","where","why"
        };
        for (String w : builtIn) stopWords.add(w);
    }
}
