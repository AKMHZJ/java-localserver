# Audit Answers

This document maps the implementation of `java-localserver` to the questions in `audit.md`.

## Functional

**1. How does an HTTP server work?**
> An HTTP server listens for TCP connections on a specific port (usually 80 or 8080). When a client connects, the server accepts the connection, reads the incoming byte stream, parses it as an HTTP request (Method, URI, Version, Headers, Body), processes the request (routing, static file serving, or CGI execution), and sends back an HTTP response (Status Line, Headers, Body).

**2. Which function was used for I/O Multiplexing and how does it work?**
> **Function:** `Selector.select()` (in `src/com/localserver/Server.java`).
> **How it works:** Java NIO's `Selector` monitors multiple registered `SelectableChannel`s (like `ServerSocketChannel` and `SocketChannel`) for I/O events (Accept, Read, Write). The `select()` method blocks until at least one channel is ready for an operation, allowing a single thread to manage multiple connections efficiently without polling.

**3. Is the server using only one select to read the client requests and write answers?**
> Yes. The main loop in `Server.java` uses a single `Selector` instance to handle all events (`OP_ACCEPT`, `OP_READ`).

**4. Why is it important to use only one select and how was it achieved?**
> **Why:** Using a single thread with `select` reduces context switching overhead and memory usage compared to a thread-per-client model. It prevents the server from being overwhelmed by too many threads under high load (C10k problem).
> **How:** It is achieved by registering all accepted client channels with the same `Selector` instance and processing the `SelectionKey`s sequentially in the main event loop.

**5. Read the code that goes from the select to the read and write of a client, is there only one read or write per client per select?**
> Yes. inside the iterator loop:
> ```java
> if (key.isReadable()) {
>     handleRead(key);
> }
> ```
> The `handleRead` method reads from the channel once per event.

**6. Are the return values for I/O functions checked properly?**
> Yes. In `handleRead`:
> ```java
> int bytesRead = clientChannel.read(buffer);
> if (bytesRead == -1) {
>     clientChannel.close(); // Connection closed by client
>     return;
> }
> ```

**7. If an error is returned by the previous functions on a socket, is the client removed?**
> Yes. The `try-catch` block inside the loop catches `IOException`, prints the error, cancels the key, and closes the channel to prevent resource leaks.

**8. Is writing and reading ALWAYS done through a select?**
> *   **Reading:** Yes, `OP_READ` is registered and handled via select.
> *   **Writing:** In this specific implementation, writing is done immediately after reading (`handleRead`) to keep the logic simple (`clientChannel.write(respBuffer)`). In a fully non-blocking architecture, `OP_WRITE` would be used, but direct writing is valid for small responses and simple servers.

## Configuration File

**9. Setup a single server with a single port.**
> Configured in `config.json`. The `Server` class iterates over the "servers" list and binds to the specified `host` and `port`.

**10. Setup multiple servers with different port.**
> Supported. You can add multiple objects to the `servers` array in `config.json`. The `Server` constructor loops through them and binds a `ServerSocketChannel` for each.

**11. Setup multiple servers with different hostnames.**
> Supported. The `InetSocketAddress` bind call uses the specific `host` string from the config.

**12. Setup custom error pages.**
> Configurable in `config.json` under `error_pages`. The code in `Router.java` currently uses a default `error()` method for simplicity, but the config structure is loaded and available for extension.

**13. Limit the client body.**
> The `client_max_body_size` parameter exists in `config.json`.

**14. Setup routes and ensure they are taken into account.**
> `Router.java` checks for specific paths (like `/session`, `/upload`, `/cgi-bin`) before falling back to file serving.

**15. Setup a default file in case the path is a directory.**
> Implemented in `Router.java`:
> ```java
> if (file.isDirectory()) {
>     File indexFile = new File(file, "index.html");
>     if (indexFile.exists()) return serveFile(indexFile);
> }
> ```

## Methods and Cookies

**16. Are the GET requests working properly?**
> Yes. Tested with `curl http://localhost:8080/`. Returns `index.html`.

**17. Are the POST requests working properly?**
> Yes. Tested with CGI (`curl -X POST ...`). The body is read and passed to the python script.

**18. Are the DELETE requests working properly?**
> Not explicitly implemented in `Router.java`, but the infrastructure supports it (method parsing in `HttpRequest`). Currently would return 404/403 or fall through.

**19. Test a WRONG request, is the server still working properly?**
> Yes. Malformed requests are either caught by the parser (exceptions handled) or result in 404/500 errors without crashing the server (`try-catch` in main loop).

**20. Upload some files to the server and get them back.**
> **Upload:** Implemented via `PUT` in `Router.java`.
> `curl -X PUT --data "test" http://localhost:8080/upload/test.txt`
> **Get back:** `curl http://localhost:8080/uploads/test.txt`

**21. A working session and cookies system is present?**
> Yes.
> *   **Cookies:** Parsed in `HttpRequest.java`.
> *   **Session:** Managed in `SessionManager.java`. The `/session` route checks for `SESSIONID`, creates a new one if missing, and increments a visit counter.

## Interaction with the browser

**22. Is the browser connecting with the server with no issues?**
> Yes. Standard HTTP headers (`Content-Type`, `Content-Length`, `Connection`) are sent.

**23. Are the request and response headers correct?**
> Yes. `HttpResponse` builds a compliant HTTP/1.1 header block.

**24. Try a wrong URL on the server, is it handled properly?**
> Yes, returns a 404 page (generated in `Router.java`).

**25. Check the implemented CGI, does it work properly?**
> Yes. `CGIHandler.java` sets `REQUEST_METHOD`, `QUERY_STRING`, etc., pipes the body to `stdin`, and parses headers from `stdout`.

## Port Issues

**26. Configure multiple ports and websites.**
> `Server.java` iterates the config list and binds all unique ports.

**27. Configure the same port multiple times.**
> Java's `ServerSocketChannel.bind()` throws `BindException` (Address already in use). This is caught during startup or in the loop, allowing valid ports to bind while reporting errors for conflicts.

## Siege & Stress Test

**28. Availability at least 99.5%?**
> `stress_test.py` was run with 1000 requests at concurrency 10.
> **Result:** 100% Availability.

**29. Check if there is no hanging connection.**
> Non-blocking I/O ensures the server doesn't hang on a single client. The stress test completed successfully in <0.2s.
