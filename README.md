# Concurrent Web Crawler

A multi-threaded web crawler implementation in Java that efficiently fetches, parses, and stores web pages while respecting robots.txt rules and implementing rate limiting.

## 📋 Overview

This project is a concurrent web crawler developed as a group practice for USJ (Universidad San Jorge). It demonstrates advanced concurrent programming concepts in Java, including thread management, thread pools, blocking queues, and concurrent data structures.

The crawler starts from a seed URL (by default, Spanish Wikipedia) and recursively discovers and fetches web pages, extracting links and storing the content to a local file.

## ✨ Features

- **Multi-threaded Architecture**: Utilizes multiple threads for fetching and storing operations to maximize efficiency
- **Concurrent Data Structures**: Implements thread-safe queues and hash maps for managing URLs and results
- **Robots.txt Compliance**: Respects robots.txt files to avoid crawling disallowed URLs
- **Rate Limiting**: Implements a 2-second delay between requests to the same host
- **Retry Mechanism**: Automatically retries failed requests up to 3 times
- **Timeout Handling**: Configurable timeout (5 seconds) for HTTP requests
- **Duplicate Prevention**: Tracks visited URLs to avoid redundant fetches
- **HTTPS-only**: Only follows secure HTTPS links
- **Thread Pool Management**: Uses ExecutorService for efficient thread pool management
- **Graceful Shutdown**: Properly terminates all threads and resources

## 🏗️ Architecture

The project follows a producer-consumer pattern with the following components:

### Core Components

#### 1. **ThreadManager** (`Concurrency/ThreadManager.java`)
- Orchestrates the entire crawling process
- Creates and manages the Fetcher and Storer threads
- Monitors work completion and triggers graceful shutdown

#### 2. **Fetcher** (`Fetching/Fetcher.java`)
- **Producer**: Fetches web pages from URLs in the queue
- Uses a thread pool (10 threads) to fetch multiple pages concurrently
- Handles robots.txt files before fetching regular pages
- Implements rate limiting to respect server resources
- Manages retry logic and timeout handling

#### 3. **Storer** (`Storing/Storer.java`)
- **Consumer**: Processes fetched documents
- Parses HTML documents to extract links
- Writes page content to output file
- Uses two thread pools:
  - 30 threads for writing operations
  - 10 threads for parsing operations

#### 4. **ConcurrentStructures** (`Concurrency/ConcurrentStructures.java`)
- Central data structure container
- Manages shared resources between threads:
  - `urlQueue`: BlockingQueue for URLs to fetch
  - `resultQueue`: BlockingQueue for fetched documents
  - `visitedURLs`: ConcurrentHashMap to track visited URLs
  - `timestamps`: ConcurrentHashMap for rate limiting
  - `robotInfo`: Manages robots.txt data

### Supporting Classes

- **FetcherThread** (`Fetching/FetcherThread.java`): Individual fetch task using Jsoup
- **Parser** (`Storing/Parser.java`): Extracts HTTPS links from HTML documents
- **OutputWriter** (`Storing/OutputWriter.java`): Thread-safe file writing with RandomAccessFile
- **RobotInfo** (`Fetching/RobotInfo.java`): Parses and manages robots.txt rules
- **Tuple** (`Main/Tuple.java`): Generic pair data structure
- **CanWork** (`Concurrency/CanWork.java`): Interface for work status checking

## 🔧 Requirements

- Java Development Kit (JDK) 8 or higher
- Jsoup library (for HTML parsing and HTTP requests)
- Internet connection

## 📦 Dependencies

The project uses the following external library:

- **Jsoup**: HTML parser and web scraper
  - Used for fetching web pages
  - Used for parsing HTML and extracting links
  - Documentation: https://jsoup.org/

## 🚀 Installation

1. Clone the repository:
```bash
git clone https://github.com/LilSamu/usj-concurrent-group-practice.git
cd usj-concurrent-group-practice
```

2. Ensure Jsoup is in your classpath. Download it from:
   - https://jsoup.org/download
   
   Or add it as a Maven dependency:
   ```xml
   <dependency>
       <groupId>org.jsoup</groupId>
       <artifactId>jsoup</artifactId>
       <version>1.15.3</version>
   </dependency>
   ```

3. Compile the project:

   **On Linux/Mac:**
   ```bash
   javac -cp ".:jsoup-1.15.3.jar" Main/*.java Concurrency/*.java Fetching/*.java Storing/*.java
   ```
   
   **On Windows:**
   ```cmd
   javac -cp ".;jsoup-1.15.3.jar" Main\*.java Concurrency\*.java Fetching\*.java Storing\*.java
   ```

## 💻 Usage

### Running the Crawler

**On Linux/Mac:**
```bash
java -cp ".:jsoup-1.15.3.jar" Main.Main
```

**On Windows:**
```cmd
java -cp ".;jsoup-1.15.3.jar" Main.Main
```

### Configuration

You can modify the following parameters in the source code:

#### Start URL
Edit `ConcurrentStructures.java` (in the constructor):
```java
URL start = new URL("https://es.wikipedia.org/wiki/Wikipedia:Portada");
```

#### Thread Pool Sizes
Edit the respective class files to change pool sizes:
- Fetcher threads: `Fetcher.java` (constructor) - default: 10
- Writing threads: `Storer.java` (constructor) - default: 30
- Parsing threads: `Storer.java` (constructor) - default: 10

#### Rate Limiting
Edit `FetcherThread.java` (field declaration):
```java
public final long rateLimitMilliseconds = 2000; // 2 seconds
```

#### Timeout
Edit `FetcherThread.java` (in the call method):
```java
return Jsoup.connect(url.toString()).timeout(5000).get(); // 5 seconds
```

#### Max Retries
Edit `FetcherThread.java` (field declaration):
```java
public final int maxTries = 3;
```

### Output

The crawler creates an `output.txt` file in the current directory containing:
- Fetched URLs with timestamps
- Raw HTML content of each page
- Separated by delimiters for easy parsing

## 📁 Project Structure

```
usj-concurrent-group-practice/
├── Main/
│   ├── Main.java              # Entry point
│   └── Tuple.java             # Generic pair class
├── Concurrency/
│   ├── ThreadManager.java     # Main thread orchestrator
│   ├── ConcurrentStructures.java  # Shared data structures
│   └── CanWork.java           # Work status interface
├── Fetching/
│   ├── Fetcher.java           # Main fetching logic
│   ├── FetcherThread.java     # Individual fetch task
│   └── RobotInfo.java         # Robots.txt handler
├── Storing/
│   ├── Storer.java            # Main storing logic
│   ├── Parser.java            # HTML parser
│   └── OutputWriter.java      # File writer
└── LICENSE
```

## 🔍 How It Works

1. **Initialization**: The crawler starts with a seed URL (Spanish Wikipedia by default)

2. **Fetching Phase**:
   - Fetcher thread takes URLs from the queue
   - Checks robots.txt for each new host
   - Applies rate limiting (2s between requests to same host)
   - Fetches page content using Jsoup
   - Places results in the result queue

3. **Storing Phase**:
   - Storer thread takes results from the result queue
   - Parses HTML to extract HTTPS links
   - Writes page content to output file
   - Adds new URLs to the URL queue

4. **Termination**:
   - Monitors both threads for activity
   - Waits for queues to be empty
   - Confirms no work is being done (5-second grace period)
   - Gracefully shuts down all threads and resources

## 🔒 Thread Safety

The project implements various thread-safety mechanisms:

- **Blocking Queues**: Thread-safe producer-consumer communication
- **ConcurrentHashMap**: Thread-safe visited URL tracking and timestamps
- **Synchronized Methods**: File offset management in OutputWriter
- **ExecutorService**: Managed thread pool lifecycle
- **Iterator.remove()**: Safe concurrent collection modification

## ⚠️ Limitations

- Only crawls HTTPS URLs
- Does not handle JavaScript-rendered content
- Robots.txt parsing is simplified (only handles `User-agent: *`)
- No depth limit implemented (relies on completion detection)
- Output file can grow very large
- No distributed crawling support

## 🤝 Contributing

This is a group practice project for USJ. Contributions are welcome for educational purposes.

## 📄 License

This project is available under the MIT License. See the [LICENSE](LICENSE) file for details.

## 👥 Authors

carbonox, lamari777, Arksuga0202, iPablol, LilSamu

## 🙏 Acknowledgments

- USJ Faculty for project guidance
- Jsoup library maintainers
- Wikipedia for serving as the default seed URL

---

**Note**: This is an educational project demonstrating concurrent programming concepts. Use responsibly and respect website terms of service and robots.txt files.
