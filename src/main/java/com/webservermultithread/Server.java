package com.webservermultithread;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static ServerSocket server;
    public static void main(String[] args) {
        start();
    }
    public static void start() {
        try {
            server = new ServerSocket(8080);
            running.set(true);
            logger.info("Server started on port 8080");

            new Thread(() -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    while (true) {
                        String command = reader.readLine();
                        if ("exit".equalsIgnoreCase(command)) {
                            running.set(false);
                            logger.info("Shutting down server...");
                            server.close();
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error reading admin command", e);
                }
            }).start();

            while (running.get()) {
                Socket socket = server.accept();
                logger.info("New connection accepted from " + socket.getInetAddress());
                new Thread(() -> handleClientRequest(socket)).start();
            }

            logger.info("Server shutdown complete.");

        } catch (IOException e) {
            logger.error("Error occurred in server", e);
        }
    }
    public static void stop() {
        running.set(false);
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            logger.error("Error while stopping the server.", e);
        }
        logger.info("Server stopped");
    }
    private static void handleClientRequest(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String line = in.readLine();
            System.out.println("Received request: " + line);

            if (line != null) {
                if (line.startsWith("GET /images/")) {
                    String imageName = line.split(" ")[1].split("/")[2];
                    serveImage(socket, out, imageName);
                } else if (line.startsWith("GET / ")) {
                    serveHtml(out);
                } else {
                    send404Error(out);
                }
            } else {
                send404Error(out);
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serveImage(Socket socket, BufferedWriter out, String imageName) throws IOException {
        InputStream imageStream = Server.class.getClassLoader().getResourceAsStream("images/" + imageName);
        System.out.println("Serving: resources/images/" + imageName);

        if (imageStream == null) {
            send404Error(out);
            return;
        }

        byte[] imageBytes = new byte[imageStream.available()];
        imageStream.read(imageBytes);
        String mimeType = "image/" + getFileExtension(imageName);
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: " + mimeType + "\r\n");
        out.write("Content-Length: " + imageBytes.length + "\r\n");
        out.write("\r\n");
        out.flush();
        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
        dataOut.write(imageBytes);
        dataOut.flush();
    }
    private static String getMD5Checksum(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content);
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }
    private static void serveHtml(BufferedWriter out) throws IOException {
        InputStream htmlStream = Server.class.getClassLoader().getResourceAsStream("html/home.html");

        if (htmlStream == null) {
            out.write("Date: " + new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").format(new Date()) + "\r\n");
            out.write("Server: CustomJavaServer/1.0\r\n");
            out.write("HTTP/1.1 404 Not Found\r\n");
            out.write("Content-Type: text/html\r\n");
            out.write("\r\n");
            out.write("<html><body><h1>404 - File Not Found</h1></body></html>");
            out.flush();
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(htmlStream));
        StringBuilder htmlContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            htmlContent.append(line).append("\n");
        }
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");
        out.write(htmlContent.toString());
        out.flush();
    }

    private static void send404Error(BufferedWriter out) throws IOException {
        out.write("HTTP/1.1 404 Not Found\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("\r\n");
        out.write("<html><body><h1>404 - File Not Found</h1></body></html>");
        out.flush();
    }
}