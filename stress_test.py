import socket
import threading
import time
import sys

HOST = '127.0.0.1'
PORT = 8080
NUM_REQUESTS = 1000
CONCURRENCY = 10

success_count = 0
fail_count = 0
lock = threading.Lock()

def make_request():
    global success_count, fail_count
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(2)
        s.connect((HOST, PORT))
        request = "GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".encode()
        s.sendall(request)
        response = b""
        while True:
            data = s.recv(4096)
            if not data:
                break
            response += data
        s.close()
        
        if b"HTTP/1.1 200 OK" in response:
            with lock:
                success_count += 1
        else:
            with lock:
                fail_count += 1
    except Exception as e:
        with lock:
            if fail_count == 0:
                print(f"Error: {e}")
            fail_count += 1

def worker():
    for _ in range(NUM_REQUESTS // CONCURRENCY):
        make_request()

threads = []
start_time = time.time()

for _ in range(CONCURRENCY):
    t = threading.Thread(target=worker, daemon=True) # daemon thread just in case
    threads.append(t)
    t.start()

for t in threads:
    t.join()

end_time = time.time()
total_time = end_time - start_time
total_requests = success_count + fail_count
availability = (success_count / total_requests) * 100 if total_requests > 0 else 0

print(f"Total Requests: {total_requests}")
print(f"Success: {success_count}")
print(f"Fail: {fail_count}")
print(f"Availability: {availability:.2f}%")
print(f"Time: {total_time:.2f}s")
if total_time > 0:
    print(f"RPS: {total_requests / total_time:.2f}")

if availability >= 99.5:
    print("PASS")
    sys.exit(0)
else:
    print("FAIL")
    sys.exit(1)
