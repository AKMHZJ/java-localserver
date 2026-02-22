#!/usr/bin/env python3
import os
import sys

print("Content-Type: text/html\r\n\r\n")
print("<html><body><h1>Hello from Python CGI!</h1>")
print("<p>Environment Variables:</p><ul>")
for k, v in os.environ.items():
    if k.startswith("HTTP_") or k in ["REQUEST_METHOD", "QUERY_STRING", "PATH_INFO"]:
        print(f"<li>{k}: {v}</li>")
print("</ul></body></html>")
