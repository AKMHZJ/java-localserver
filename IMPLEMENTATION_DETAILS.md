# Implementation Details

## File Structure
*   `src/com/localserver/`: Source code.
    *   `Main.java`: Bootstrap.
    *   `Server.java`: NIO Loop.
    *   `ConfigLoader.java`: JSON Parser.
    *   `Router.java`: Request dispatching.
    *   `CGIHandler.java`: CGI execution.
    *   `HttpRequest.java`: Request parsing.
    *   `HttpResponse.java`: Response generation.
    *   `SessionManager.java`: In-memory session store.
*   `config.json`: Server configuration.
*   `www/`: Web root.
    *   `cgi-bin/`: CGI scripts.
    *   `uploads/`: Upload directory.

## Key Design Decisions

### 1. Custom JSON Parser
To avoid external dependencies like Jackson or Gson, a simple recursive descent parser (`ConfigLoader`) was implemented to read the JSON configuration. It supports objects, arrays, strings, and numbers.

### 2. Single-Threaded NIO
Used `java.nio.channels.Selector` to manage concurrency without creating a thread per client. This ensures high scalability and low memory footprint. The event loop iterates over selected keys, handling accepts and reads sequentially.

### 3. CGI Execution
`ProcessBuilder` is used to spawn new processes for CGI scripts.
*   **Environment Variables**: `REQUEST_METHOD`, `QUERY_STRING`, `PATH_INFO`, etc., are passed to the script.
*   **I/O Redirection**: The request body is written to the process's `stdin`, and `stdout` is read to form the response body.
*   **Header Parsing**: The handler parses the CGI output to separate headers (like `Content-Type`) from the body.

### 4. Session Management
A simple in-memory `HashMap` stores session data, keyed by a UUID. The `SESSIONID` cookie is used to associate requests with sessions.

### 5. Routing
The `Router` resolves paths against the document root.
*   **Security**: Checks for path traversal (`..`) by verifying the resolved path starts with the root path.
*   **Extensions**: Detects `.py` extension to delegate to `CGIHandler`.

## Challenges & Solutions
*   **Partial Reads**: The current implementation assumes headers fit in the initial buffer. For a production-grade server, a state machine would be needed to buffer partial reads until a full request is available.
*   **CGI Output**: Parsing CGI output required careful handling of newlines (`
` vs `
`) and ensuring headers are correctly extracted before the body.
*   **Blocking I/O in CGI**: `Process.waitFor()` or reading streams is blocking. In a pure non-blocking server, this should be handled asynchronously (e.g., using a separate thread pool for CGI or non-blocking pipes). For this project, it blocks the main thread briefly, which is a known limitation of this simple implementation.

## Future Improvements
*   Implement `POST` multipart/form-data parsing.
*   Add connection timeouts (keep-alive).
*   Implement chunked transfer encoding support.
*   Add HTTPS support (SSLEngine).
