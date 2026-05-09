package it.unipv.ir.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses a raw query string into a ParsedQuery object.
 *
 * Supported syntax:
 *   quantum mechanics              → conjunctive (AND) query
 *   quantum OR mechanics           → disjunctive (OR) query
 *   "quantum mechanics"            → phrase query
 *   comp*ter                       → wildcard query
 *   quantum NEAR/3 mechanics       → proximity query (within 3 positions)
 *   quantum NOT mechanics          → AND NOT query
 *   category:physics               → parametric field query
 *
 * Tokens are lowercased but NOT stemmed here — the query processors
 * apply preprocessing as needed so they can decide what to stem.
 */
public class QueryParser {

    public enum QueryType {
        CONJUNCTIVE, DISJUNCTIVE, PHRASE, WILDCARD, PROXIMITY, PARAMETRIC
    }

    public static class ParsedQuery {
        public final QueryType   type;
        public final List<String> terms;       // main query terms
        public final String      phrase;       // for PHRASE type
        public final String      wildcard;     // for WILDCARD type
        public final int         proximityK;   // for PROXIMITY type
        public final String      field;        // for PARAMETRIC type
        public final boolean     hasNot;       // for CONJUNCTIVE with NOT

        // Full conjunctive query with optional NOT terms
        public final List<String> notTerms;

        ParsedQuery(QueryType type, List<String> terms, String phrase,
                    String wildcard, int proximityK, String field,
                    boolean hasNot, List<String> notTerms) {
            this.type       = type;
            this.terms      = terms;
            this.phrase     = phrase;
            this.wildcard   = wildcard;
            this.proximityK = proximityK;
            this.field      = field;
            this.hasNot     = hasNot;
            this.notTerms   = notTerms;
        }
    }

    /**
     * Parse a query string. Returns a ParsedQuery describing the type
     * and components of the query.
     */
    public ParsedQuery parse(String rawQuery) {
        String q = rawQuery.trim();
        if (q.isEmpty()) {
            return conjunctive(List.of());
        }

        // Phrase query: quoted string
        if (q.startsWith("\"") && q.endsWith("\"") && q.length() > 2) {
            String inner = q.substring(1, q.length() - 1).trim();
            return new ParsedQuery(QueryType.PHRASE,
                tokenize(inner), inner, null, 0, null, false, List.of());
        }

        // Parametric field query: field:value
        if (q.matches("\\w+:.+")) {
            int colon = q.indexOf(':');
            String field = q.substring(0, colon).toLowerCase();
            String value = q.substring(colon + 1).toLowerCase();
            return new ParsedQuery(QueryType.PARAMETRIC,
                tokenize(value), null, null, 0, field, false, List.of());
        }

        // Wildcard query: contains '*' and is a single token
        if (!q.contains(" ") && q.contains("*")) {
            return new ParsedQuery(QueryType.WILDCARD,
                List.of(q.toLowerCase()), null, q.toLowerCase(), 0, null, false, List.of());
        }

        // Proximity query: NEAR/k operator
        // Example: "quantum NEAR/3 mechanics"
        if (q.toUpperCase().contains("NEAR/")) {
            return parseProximity(q);
        }

        // Disjunctive: contains uppercase OR
        if (q.contains(" OR ")) {
            String[] parts = q.split("\\s+OR\\s+");
            List<String> terms = new ArrayList<>();
            for (String p : parts) terms.addAll(tokenize(p));
            return new ParsedQuery(QueryType.DISJUNCTIVE,
                terms, null, null, 0, null, false, List.of());
        }

        // Conjunctive with NOT: "quantum NOT mechanics"
        if (q.contains(" NOT ")) {
            String[] parts = q.split("\\s+NOT\\s+", 2);
            List<String> positive = tokenize(parts[0]);
            List<String> negative = tokenize(parts[1]);
            return new ParsedQuery(QueryType.CONJUNCTIVE,
                positive, null, null, 0, null, true, negative);
        }

        // Default: conjunctive (AND) query
        // Strip explicit AND keyword if present
        String stripped = q.replaceAll("\\s+AND\\s+", " ");
        return conjunctive(tokenize(stripped));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ParsedQuery parseProximity(String q) {
        // Pattern: term1 NEAR/k term2
        String upper = q.toUpperCase();
        int nearIdx = upper.indexOf("NEAR/");
        String left  = q.substring(0, nearIdx).trim().toLowerCase();
        String right = q.substring(nearIdx).trim();

        // Parse k from NEAR/k
        String[] parts = right.split("\\s+", 2);
        int k = 1;
        try {
            k = Integer.parseInt(parts[0].substring(5)); // after "NEAR/"
        } catch (NumberFormatException ignored) {}

        String term2 = parts.length > 1 ? parts[1].trim().toLowerCase() : "";

        List<String> terms = new ArrayList<>();
        terms.addAll(tokenize(left));
        terms.addAll(tokenize(term2));

        return new ParsedQuery(QueryType.PROXIMITY,
            terms, null, null, k, null, false, List.of());
    }

    private ParsedQuery conjunctive(List<String> terms) {
        return new ParsedQuery(QueryType.CONJUNCTIVE,
            terms, null, null, 0, null, false, List.of());
    }

    private List<String> tokenize(String text) {
        String[] parts = text.trim().toLowerCase().split("\\s+");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String clean = p.replaceAll("[^a-z0-9*]", "");
            if (!clean.isEmpty()) result.add(clean);
        }
        return result;
    }
}
