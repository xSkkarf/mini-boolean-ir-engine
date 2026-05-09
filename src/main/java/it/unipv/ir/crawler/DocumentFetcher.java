package it.unipv.ir.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches individual Wikipedia articles using the MediaWiki Action API.
 *
 * Uses the extract API (prop=extracts) for clean plain-text content and
 * the categories API (prop=categories) for zone/parametric indexing.
 * Also fetches links from a page so the crawler can follow them.
 */
public class DocumentFetcher {

    private static final String API_BASE =
        "https://en.wikipedia.org/w/api.php";

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public DocumentFetcher() {
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Fetch plain-text extract + categories for a page title.
     * Returns null if the page doesn't exist or is a disambiguation page.
     */
    public FetchedPage fetchPage(String title)
            throws IOException, InterruptedException {

        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String url = API_BASE
            + "?action=query"
            + "&titles=" + encoded
            + "&prop=" + URLEncoder.encode("extracts|categories|info", StandardCharsets.UTF_8)
            + "&exintro=false"
            + "&explaintext=true"
            + "&cllimit=20"
            + "&inprop=url"
            + "&redirects=1"
            + "&format=json";

        String body = get(url);
        JsonNode root  = mapper.readTree(body);
        JsonNode pages = root.path("query").path("pages");

        // MediaWiki returns a map of pageId -> page; grab the first entry
        JsonNode page = pages.fields().next().getValue();

        // Missing or invalid page
        if (page.has("missing") || page.has("invalid")) return null;

        String extract = page.path("extract").asText("").trim();
        if (extract.isEmpty()) return null;

        // Skip disambiguation pages
        if (extract.startsWith(title + " may refer to") ||
            extract.contains("This disambiguation page")) return null;

        String pageTitle = page.path("title").asText(title);
        String pageUrl   = page.path("fullurl").asText("");

        List<String> categories = new ArrayList<>();
        for (JsonNode cat : page.path("categories")) {
            String catTitle = cat.path("title").asText("");
            // Strip "Category:" prefix
            if (catTitle.startsWith("Category:")) {
                catTitle = catTitle.substring(9);
            }
            categories.add(catTitle);
        }

        return new FetchedPage(pageTitle, extract, categories, pageUrl);
    }

    /**
     * Fetch the list of linked page titles from a given article.
     * Used by the BFS crawler to discover new pages.
     */
    public List<String> fetchLinks(String title)
            throws IOException, InterruptedException {

        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String url = API_BASE
            + "?action=query"
            + "&titles=" + encoded
            + "&prop=links"
            + "&pllimit=100"
            + "&plnamespace=0"
            + "&redirects=1"
            + "&format=json";

        String body  = get(url);
        JsonNode root = mapper.readTree(body);
        JsonNode pages = root.path("query").path("pages");
        JsonNode page  = pages.fields().next().getValue();

        List<String> links = new ArrayList<>();
        for (JsonNode link : page.path("links")) {
            links.add(link.path("title").asText());
        }
        return links;
    }

    // -------------------------------------------------------------------------
    // HTTP helper
    // -------------------------------------------------------------------------

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent",
                "MiniIRSystem/1.0 (academic project; "
                + "contact: student@unipv.it)")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        HttpResponse<String> resp =
            http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new IOException(
                "HTTP " + resp.statusCode() + " for: " + url);
        }
        return resp.body();
    }

    // -------------------------------------------------------------------------
    // Inner value object
    // -------------------------------------------------------------------------

    public static class FetchedPage {
        public final String title;
        public final String extract;
        public final List<String> categories;
        public final String url;

        public FetchedPage(String title, String extract,
                           List<String> categories, String url) {
            this.title      = title;
            this.extract    = extract;
            this.categories = categories;
            this.url        = url;
        }
    }
}
