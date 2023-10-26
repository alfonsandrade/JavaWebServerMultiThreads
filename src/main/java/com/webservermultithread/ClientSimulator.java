package com.webservermultithread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientSimulator {

    private static final Logger logger = LoggerFactory.getLogger(ClientSimulator.class);

    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int NUM_CLIENTS = 10;  // Number of clients to simulate
    private static final long START_INTERVAL = 100;  // milliseconds
    private static final long MAX_INTERVAL = 3000;  // milliseconds

    public static void main(String[] args) {
        List<Thread> threads = new ArrayList<>();
        long currentInterval = START_INTERVAL;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.info("Scheduler triggered.");
                ClientSimulator.main(new String[]{});  // Recursive call
            } catch (Exception e) {
                logger.error("Error in the scheduler task.", e);
            }
        }, 5, 5, TimeUnit.SECONDS);

        for (int i = 0; i < NUM_CLIENTS; i++) {
            try {
                Thread.sleep(currentInterval);  // Introducing interval

                Thread thread = new Thread(() -> {
                    try (Socket socket = new Socket(HOST, PORT);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                        // Send an HTTP GET request
                        out.println("GET / HTTP/1.1");
                        out.println("Host: " + HOST);
                        out.println("Connection: Close");
                        out.println();

                        // Read the server's response
                        String responseLine;
                        while ((responseLine = in.readLine()) != null) {
                            logger.info("Server response: {}", responseLine);
                        }

                    } catch (IOException e) {
                        logger.error("Failed to connect to server on port {}", PORT, e);
                    }
                });

                threads.add(thread);
                thread.start();

                // Update interval (increase for the first 30 seconds and decrease for the next 30 seconds)
                if (i < NUM_CLIENTS / 2) {
                    currentInterval += (MAX_INTERVAL - START_INTERVAL) / (NUM_CLIENTS / 2);
                } else {
                    currentInterval -= (MAX_INTERVAL - START_INTERVAL) / (NUM_CLIENTS / 2);
                }
            } catch (InterruptedException e) {
                logger.error("Thread sleep interrupted.", e);
            }
        }

        // Optional: Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                logger.error("Thread join interrupted.", e);
            }
        }
    }
}
