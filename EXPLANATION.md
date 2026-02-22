# Java Local Server - Explanation

## Overview
This project is a custom HTTP/1.1 server written in Java using `java.nio` for non-blocking I/O. It supports static file serving, CGI execution (Python), file uploads, and session management.

## Architecture

### Core Components
1.  **Server (`Server.java`)**: The main entry point that initializes the `Selector` and binds `ServerSocketChannel`s. It runs a single-threaded event loop that handles `OP_ACCEPT` and `OP_READ` events.
2.  **ConfigLoader (`ConfigLoader.java`)**: A custom JSON parser that reads `config.json` to configure server ports, hosts, roots, and routes.
3.  **Router (`Router.java`)**: Dispatches requests based on the URL path. It handles:
    *   **Static Files**: Serves files from the configured root directory.
    *   **CGI**: Executes scripts (e.g., `.py`) via `ProcessBuilder`.
    *   **Uploads**: Handles `PUT` requests to `/upload`.
    *   **Session**: Manages user sessions via cookies.
4.  **HttpRequest (`HttpRequest.java`)**: Parses raw bytes into a structured request object (Method, Path, Headers, Body, Cookies).
5.  **HttpResponse (`HttpResponse.java`)**: Constructs HTTP responses with appropriate status codes and headers.
6.  **CGIHandler (`CGIHandler.java`)**: Manages the execution of external scripts, passing environment variables (`REQUEST_METHOD`, `QUERY_STRING`, etc.) and piping input/output.

### Non-Blocking I/O
The server uses a single thread to handle multiple connections.
*   **Selector**: Monitors multiple channels for events (connection ready, data ready).
*   **Channels**: `SocketChannel`s are used in non-blocking mode.
*   **Buffers**: `ByteBuffer`s are used to read/write data efficiently.

## Features
*   **HTTP/1.1 Compliance**: Handles standard methods (GET, POST, PUT, DELETE).
*   **CGI Support**: Executes Python scripts and streams output back to the client.
*   **Sessions**: Uses `SESSIONID` cookie to track visits.
*   **Uploads**: Supports file upload via `PUT` method.
*   **Configuration**: Flexible JSON-based configuration.

## Usage
1.  Compile: `javac -d out src/com/localserver/*.java`
2.  Run: `java -cp out com.localserver.Main [config_file]`
3.  Test:
    *   Static: `curl http://localhost:8080/`
    *   CGI: `curl -X POST http://localhost:8080/cgi-bin/hello.py`
    *   Session: `curl -c cookies.txt -b cookies.txt http://localhost:8080/session`
    *   Upload: `curl -X PUT --data "content" http://localhost:8080/upload/test.txt`

## Stress Testing
A `stress_test.py` script is included to verify availability under load.
`python3 stress_test.py`
