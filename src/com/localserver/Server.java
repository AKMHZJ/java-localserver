package com.localserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class Server {
    private final Map<String, Object> config;
    private Selector selector;
    private Router router;

    public Server(Map<String, Object> config) {
        this.config = config;
        // Assuming single server config for now or taking the first one for the router
        // In a multi-server setup, we'd map ports to routers
        List<Object> servers = (List<Object>) config.get("servers");
        if (servers != null && !servers.isEmpty()) {
             this.router = new Router((Map<String, Object>) servers.get(0));
        } else {
             this.router = new Router(config); // Fallback
        }
    }

    public void start() throws IOException {
        selector = Selector.open();
        // Setup servers based on config
        List<Object> servers = (List<Object>) config.get("servers");
        if (servers != null) {
            for (Object s : servers) {
                Map<String, Object> serverConfig = (Map<String, Object>) s;
                String host = (String) serverConfig.getOrDefault("host", "localhost");
                
                Object portObj = serverConfig.getOrDefault("port", 8080);
                int port;
                if (portObj instanceof Number) {
                    port = ((Number) portObj).intValue();
                } else {
                    port = Integer.parseInt(portObj.toString());
                }
                
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.bind(new InetSocketAddress(host, port));
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("Listening on " + host + ":" + port);
            }
        }

        System.out.println("Server started.");

        while (true) {
            try {
                if (selector.select() == 0) continue;
                
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) continue;

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        }
                    } catch (IOException e) {
                        System.err.println("Error handling key: " + e.getMessage());
                        key.cancel();
                        try {
                            key.channel().close();
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Selector error: " + e.getMessage());
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            clientChannel.close();
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            String rawRequest = new String(buffer.array(), 0, bytesRead);
            // System.out.println("Received: " + rawRequest); // Debug logging

            HttpRequest request = new HttpRequest(rawRequest);
            HttpResponse response = router.handle(request);
            
            ByteBuffer respBuffer = ByteBuffer.wrap(response.toBytes());
            while(respBuffer.hasRemaining()) {
                clientChannel.write(respBuffer);
            }
            clientChannel.close(); // Close for now (non-keep-alive)
        }
    }
}
