package it.unipv.ir.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists RawDocument objects as individual JSON files under a configurable
 * root directory. Each document is stored as {docId}.json.
 *
 * This keeps the crawling phase completely decoupled from indexing:
 * you can crawl once, then rebuild indexes as many times as needed.
 */
public class DocumentStore {

    private final Path rootDir;
    private final ObjectMapper mapper = new ObjectMapper();

    public DocumentStore(String rootDirPath) throws IOException {
        this.rootDir = Paths.get(rootDirPath);
        Files.createDirectories(rootDir);
    }

    /** Persist a single document. Overwrites if it already exists. */
    public void save(RawDocument doc) throws IOException {
        File f = rootDir.resolve(doc.getDocId() + ".json").toFile();
        mapper.writeValue(f, doc);
    }

    /** Load a single document by ID. Returns null if not found. */
    public RawDocument load(int docId) throws IOException {
        File f = rootDir.resolve(docId + ".json").toFile();
        if (!f.exists()) return null;
        return mapper.readValue(f, RawDocument.class);
    }

    /** Load every document in the store (used by IndexBuilder). */
    public List<RawDocument> loadAll() throws IOException {
        List<RawDocument> docs = new ArrayList<>();
        File[] files = rootDir.toFile().listFiles(
            (dir, name) -> name.endsWith(".json"));
        if (files == null) return docs;
        for (File f : files) {
            docs.add(mapper.readValue(f, RawDocument.class));
        }
        return docs;
    }

    /** How many documents are currently stored. */
    public int size() {
        File[] files = rootDir.toFile().listFiles(
            (dir, name) -> name.endsWith(".json"));
        return files == null ? 0 : files.length;
    }

    /** Check whether a document with this ID is already stored. */
    public boolean contains(int docId) {
        return rootDir.resolve(docId + ".json").toFile().exists();
    }

    public Path getRootDir() { return rootDir; }
}
