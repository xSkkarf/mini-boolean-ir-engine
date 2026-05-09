package it.unipv.ir.crawler;

import java.util.List;

/**
 * Represents a single crawled Wikipedia article before preprocessing.
 * Persisted to disk as JSON by DocumentStore.
 */
public class RawDocument {

    private int docId;
    private String title;
    private String body;
    private List<String> categories;
    private String url;

    // Jackson needs a no-arg constructor
    public RawDocument() {}

    public RawDocument(int docId, String title, String body,
                       List<String> categories, String url) {
        this.docId      = docId;
        this.title      = title;
        this.body       = body;
        this.categories = categories;
        this.url        = url;
    }

    // -------------------------------------------------------------------------
    // Getters / setters (Jackson uses these)
    // -------------------------------------------------------------------------

    public int getDocId()               { return docId; }
    public void setDocId(int docId)     { this.docId = docId; }

    public String getTitle()            { return title; }
    public void setTitle(String t)      { this.title = t; }

    public String getBody()             { return body; }
    public void setBody(String b)       { this.body = b; }

    public List<String> getCategories()             { return categories; }
    public void setCategories(List<String> cats)    { this.categories = cats; }

    public String getUrl()              { return url; }
    public void setUrl(String url)      { this.url = url; }

    @Override
    public String toString() {
        return "RawDocument{docId=" + docId + ", title='" + title + "'}";
    }
}
