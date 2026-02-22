package com.localserver;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            String configPath = "config.json";
            if (args.length > 0) {
                configPath = args[0];
            }
            ConfigLoader loader = new ConfigLoader(configPath);
            Map<String, Object> config = loader.parse();
            System.out.println("Configuration loaded successfully.");
            
            Server server = new Server(config);
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
