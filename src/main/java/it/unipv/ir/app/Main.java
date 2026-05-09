package it.unipv.ir.app;

import it.unipv.ir.crawler.DocumentStore;
import it.unipv.ir.crawler.WikipediaCrawler;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * CLI entry point for the Mini Boolean IR System.
 *
 * Usage:
 *   java -jar ir-system.jar crawl   [store_path] [target_count]
 *   java -jar ir-system.jar index   [store_path]
 *   java -jar ir-system.jar search  [store_path]
 *   java -jar ir-system.jar all     [store_path]   (crawl + index + search)
 *
 * Defaults:
 *   store_path   = ./data/documents
 *   target_count = 10000
 *
 * Search syntax supported at the interactive prompt:
 *   quantum mechanics          → AND query
 *   quantum OR mechanics       → OR query
 *   "quantum mechanics"        → phrase query
 *   comp*ter                   → wildcard query
 *   quantum NEAR/3 mechanics   → proximity query
 *   quantum NOT mechanics      → AND NOT query
 *   category:physics           → parametric query
 *   similar:42                 → find documents similar to doc #42
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final String DEFAULT_STORE = "./data/documents";
    private static final int    DEFAULT_COUNT = 10_000;

    public static void main(String[] args) throws Exception {
        String command    = args.length > 0 ? args[0].toLowerCase() : "search";
        String storePath  = args.length > 1 ? args[1] : DEFAULT_STORE;
        int    targetCount = args.length > 2
            ? Integer.parseInt(args[2]) : DEFAULT_COUNT;

        switch (command) {
            case "crawl":
                runCrawl(storePath, targetCount);
                break;
            case "index":
                runIndex(storePath);
                break;
            case "search":
                runSearch(storePath);
                break;
            case "all":
                runCrawl(storePath, targetCount);
                runSearch(storePath);
                break;
            default:
                printUsage();
        }
    }

    // -------------------------------------------------------------------------
    // Phase runners
    // -------------------------------------------------------------------------

    private static void runCrawl(String storePath, int targetCount)
            throws Exception {
        LOG.info("Starting crawl → " + storePath
            + "  target=" + targetCount);
        DocumentStore store = new DocumentStore(storePath);
        WikipediaCrawler crawler = new WikipediaCrawler(store, targetCount);
        crawler.crawl();
        LOG.info("Crawl complete. Documents stored: " + store.size());
    }

    private static IndexBundle runIndex(String storePath) throws Exception {
        LOG.info("Building indexes from " + storePath);
        DocumentStore store  = new DocumentStore(storePath);
        IndexBuilder  builder = new IndexBuilder();
        IndexBundle   bundle  = builder.build(store);
        LOG.info("Index build complete.");
        LOG.info("  Terms: " + bundle.positionalIndex.getTermCount());
        LOG.info("  Docs:  " + bundle.positionalIndex.getDocCount());
        return bundle;
    }

    private static void runSearch(String storePath) throws Exception {
        DocumentStore store  = new DocumentStore(storePath);
        IndexBundle   bundle = new IndexBuilder().build(store);
        SearchEngine  engine = new SearchEngine(bundle, store);

        System.out.println("\n=== Mini Boolean IR System ===");
        System.out.println("Documents indexed : " + bundle.positionalIndex.getDocCount());
        System.out.println("Vocabulary size   : " + bundle.positionalIndex.getTermCount());
        System.out.println("Type a query and press Enter. Type 'quit' to exit.\n");

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit"))
                break;
            if (line.isEmpty()) continue;

            // Special command: similar:<docId>
            if (line.startsWith("similar:")) {
                int docId = Integer.parseInt(line.substring(8).trim());
                engine.findSimilar(docId, 0.3).forEach(s ->
                    System.out.println("  " + s));
                continue;
            }

            long start = System.currentTimeMillis();
            SearchEngine.SearchResponse resp = engine.search(line);
            long elapsed = System.currentTimeMillis() - start;

            System.out.printf("Query: \"%s\"  |  hits: %d  |  time: %dms%n",
                resp.query, resp.totalHits, elapsed);

            if (resp.results.isEmpty()) {
                System.out.println("  No results.");
                if (!resp.suggestions.isEmpty()) {
                    System.out.println("  Did you mean: "
                        + String.join(", ", resp.suggestions) + "?");
                }
            } else {
                int rank = 1;
                for (SearchEngine.SearchResult r : resp.results) {
                    System.out.printf("  %2d. [%.3f] %s%n",
                        rank++, r.score, r.title);
                    if (!r.url.isEmpty())
                        System.out.printf("       %s%n", r.url);
                }
            }
            System.out.println();
        }
        scanner.close();
        System.out.println("Goodbye.");
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar ir-system.jar crawl  [store_path] [target_count]");
        System.out.println("  java -jar ir-system.jar index  [store_path]");
        System.out.println("  java -jar ir-system.jar search [store_path]");
        System.out.println("  java -jar ir-system.jar all    [store_path]");
    }
}
