package it.unipv.ir.crawler;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Breadth-first crawler over Wikipedia.
 *
 * Seeds from a fixed list of topic pages and follows links up to a
 * configurable depth. Documents are stored via DocumentStore as they
 * are fetched, so a partial run is always resumable.
 *
 * Rate limiting: 200ms delay between requests to respect Wikipedia's
 * bot policy (max ~5 req/s for non-registered bots).
 */
public class WikipediaCrawler {

    private static final Logger LOG =
        Logger.getLogger(WikipediaCrawler.class.getName());

    // Wikipedia seed topics — diverse enough for good TF-IDF variation
    private static final List<String> SEEDS = Arrays.asList(
        "Computer science",
        "Physics",
        "World War II",
        "Mathematics",
        "Biology",
        "Philosophy",
        "Economics",
        "Music",
        "Geography",
        "Chemistry"
    );

    private static final long DELAY_MS     = 200;   // politeness delay
    private static final int  MAX_DEPTH    = 2;     // BFS hops from seed

    private final DocumentStore store;
    private final DocumentFetcher fetcher;
    private final int targetCount;

    public WikipediaCrawler(DocumentStore store, int targetCount) {
        this.store       = store;
        this.fetcher     = new DocumentFetcher();
        this.targetCount = targetCount;
    }

    /**
     * Run the crawl. Stops when targetCount documents have been stored
     * or the BFS frontier is exhausted.
     *
     * Each BFS entry is a (title, depth) pair.
     */
    public void crawl() throws IOException, InterruptedException {
        Set<String> visited = new HashSet<>();
        Queue<BfsEntry> queue = new ArrayDeque<>();

        // Seed the queue
        for (String seed : SEEDS) {
            queue.add(new BfsEntry(seed, 0));
            visited.add(normalise(seed));
        }

        int docId = store.size();   // resume-safe: continue from last ID

        while (!queue.isEmpty() && docId < targetCount) {
            BfsEntry entry = queue.poll();
            String title   = entry.title;
            int    depth   = entry.depth;

            // Skip if already stored (resumability)
            if (store.contains(docId)) { docId++; continue; }

            try {
                Thread.sleep(DELAY_MS);
                DocumentFetcher.FetchedPage page = fetcher.fetchPage(title);

                if (page == null) {
                    LOG.fine("Skipping: " + title);
                    continue;
                }

                RawDocument doc = new RawDocument(
                    docId, page.title, page.extract,
                    page.categories, page.url);
                store.save(doc);
                docId++;

                if (docId % 100 == 0) {
                    LOG.info("Crawled " + docId + " / " + targetCount
                        + " documents");
                }

                // Expand frontier if within depth limit
                if (depth < MAX_DEPTH) {
                    Thread.sleep(DELAY_MS);
                    List<String> links = fetcher.fetchLinks(title);
                    for (String link : links) {
                        String norm = normalise(link);
                        if (!visited.contains(norm)) {
                            visited.add(norm);
                            queue.add(new BfsEntry(link, depth + 1));
                        }
                    }
                }

            } catch (IOException e) {
                LOG.warning("Failed to fetch '" + title + "': " + e.getMessage());
            }
        }

        LOG.info("Crawl complete. Total documents: " + store.size());
    }

    private static String normalise(String title) {
        return title.trim().toLowerCase().replace(' ', '_');
    }

    // Simple BFS entry
    private static class BfsEntry {
        final String title;
        final int    depth;
        BfsEntry(String t, int d) { title = t; depth = d; }
    }
}
