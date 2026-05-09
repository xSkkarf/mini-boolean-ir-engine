# Mini Boolean IR System

A small-scale Boolean Information Retrieval system built in Java over a corpus of Wikipedia articles. This project was developed for the Information Retrieval course at **Università degli Studi di Pavia**.

---

## 🚀 Quick Start Guide

Follow these steps to get the system up and running on your machine.

### 1. Prerequisites
Ensure you have the following installed:
*   **Java 17** or later
*   **Maven 3.8** or later

### 2. Build the Project
Compile the code and package it into an executable "fat" JAR:
```bash
mvn package
```
This will create `target/ir-system-1.0-SNAPSHOT-jar-with-dependencies.jar`.

### 3. Fetch Data (Crawl Wikipedia)
The system needs documents to index. Use the built-in crawler to download articles from Wikipedia:
```bash
# Usage: java -jar <jar_path> crawl <data_directory> <number_of_docs>
java -jar target/ir-system-1.0-SNAPSHOT-jar-with-dependencies.jar crawl ./data/documents 100
```
*Tip: Start with 100 documents to test quickly. A full crawl of 10,000 docs takes ~45 minutes.*

### 4. Search the Index
Once you have documents in `./data/documents`, launch the interactive search engine:
```bash
java -jar target/ir-system-1.0-SNAPSHOT-jar-with-dependencies.jar search ./data/documents
```
The system will build the indexes in memory and then wait for your queries.

---

## 🔍 Query Syntax

The search engine supports a wide range of operators:

| Feature | Syntax Example | Description |
| :--- | :--- | :--- |
| **Simple / AND** | `quantum mechanics` | Finds documents containing **both** terms. |
| **OR** | `einstein OR newton` | Finds documents containing **either** term. |
| **NOT** | `apple NOT fruit` | Finds "apple" but excludes "fruit". |
| **Phrase** | `"theory of relativity"` | Matches the **exact** phrase in quotes. |
| **Wildcard** | `comp*ter` | Matches terms like "computer", "completer". |
| **Proximity** | `quantum NEAR/5 physics` | Terms within 5 words of each other. |
| **Field Search**| `category:physics` | Filters by Wikipedia category. |
| **Similarity** | `similar:10` | Finds documents similar to document ID 10. |

Type `exit` or `quit` to leave the search prompt.

---

## 🛠️ Project Architecture

The system is modularly designed:

*   **`crawler/`**: Wikipedia BFS crawler & JSON storage.
*   **`preprocessing/`**: Tokenization, Normalization, Stop-word removal, and Stemming.
*   **`datastructures/`**: Custom B-Tree, Posting Lists, Skip Lists, and Tries.
*   **`index/`**: Inverted, Positional, K-Gram, Permuterm, and Tiered indexes.
*   **`query/`**: Parsers and processors for boolean, phrase, and wildcard queries.
*   **`ranking/`**: Scoring engines (TF-IDF, BM25) and Top-K extraction.
*   **`compression/`**: V-Byte and Gamma encoding for space efficiency.
*   **`spelling/`**: Levenshtein-based spelling correction.
*   **`similarity/`**: Near-duplicate detection using Jaccard/Shingles.

---

## 🧪 Running Tests

To verify the installation and system integrity, run:
```bash
mvn test
```