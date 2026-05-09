package it.unipv.ir.spelling;

/**
 * A single spelling correction suggestion.
 */
public class CorrectionResult {

    private final String term;
    private final int    editDistance;

    public CorrectionResult(String term, int editDistance) {
        this.term         = term;
        this.editDistance = editDistance;
    }

    public String getTerm()         { return term; }
    public int    getEditDistance() { return editDistance; }

    @Override
    public String toString() {
        return term + " (ed=" + editDistance + ")";
    }
}
