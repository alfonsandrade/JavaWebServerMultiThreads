package com.webservermultithread;

import org.junit.jupiter.api.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private static final Logger logger = LoggerFactory.getLogger(ServerTest.class);
    private static final String BASE_URL = "http://localhost:8080/";
    private Server server;
    private Thread serverThread;

    @Test
    void testSuccessfulRequest() throws IOException {
        logger.info("Starting testSuccessfulRequest");
        URL url = new URL(BASE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        assertEquals(200, connection.getResponseCode());

        String content = readResponseContent(connection);
        logger.info("Received content: {}", content);
        assertEquals(200, connection.getResponseCode());
    }

    @Test
    void testImageRequest() throws IOException {
        logger.info("Starting testImageRequest");
        URL url = new URL(BASE_URL + "path/to/image");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        assertEquals(200, connection.getResponseCode());
        Document doc = Jsoup.parse(readResponseContent(connection));
        assertNotNull(doc.selectFirst("img[src$=.png]"));
        assertEquals("text/html", connection.getContentType());
        logger.info("Received content type: {}", connection.getContentType());
    }

    @Test
    void testNotFoundRequest() throws IOException {
        logger.info("Starting testNotFoundRequest");
        URL url = new URL(BASE_URL + "path/not/exist");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        System.out.println(connection.getResponseCode());
        assertEquals(404, connection.getResponseCode());
    }

    private String readResponseContent(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseContent.append(line);
        }
        reader.close();
        return responseContent.toString();
    }
}
