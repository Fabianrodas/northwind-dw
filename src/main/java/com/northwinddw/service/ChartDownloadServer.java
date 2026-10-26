package com.northwinddw.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/** Serves one temporary chart image to a device on the same local network. */
public final class ChartDownloadServer implements AutoCloseable {
    public static final int DOWNLOAD_LIFETIME_MINUTES = 10;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern SAFE_PNG_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,79}\\.png");
    private static final int MAX_REQUEST_LINE_LENGTH = 2_048;
    private static final int MAX_HEADER_LINE_LENGTH = 2_048;
    private static final int MAX_HEADER_COUNT = 32;
    private static final int MAX_HEADER_BYTES = 16_384;
    private static final int REQUEST_TIMEOUT_MILLIS = 5_000;
    private static final int MAX_IMAGE_BYTES = 15 * 1024 * 1024;

    private final ServerSocket serverSocket;
    private final ExecutorService acceptExecutor;
    private final ThreadPoolExecutor requestExecutor;
    private final ScheduledExecutorService expiryExecutor;
    private final byte[] image;
    private final String fileName;
    private final String requestPath;
    private final URI downloadUri;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();

    private ChartDownloadServer(InetAddress bindAddress, byte[] image, String fileName,
                                Duration lifetime) throws IOException {
        validate(bindAddress, image, fileName, lifetime);
        this.image = image.clone();
        this.fileName = fileName;
        this.requestPath = "/download/" + randomToken() + "/" + fileName;

        ServerSocket listener = new ServerSocket();
        try {
            listener.setReuseAddress(false);
            listener.bind(new InetSocketAddress(bindAddress, 0), 16);
        } catch (IOException exception) {
            listener.close();
            throw exception;
        }
        this.serverSocket = listener;
        this.acceptExecutor = Executors.newSingleThreadExecutor(daemonThread("northwind-chart-accept"));
        this.requestExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(8), daemonThread("northwind-chart-request"),
                new ThreadPoolExecutor.AbortPolicy());
        this.expiryExecutor = Executors.newSingleThreadScheduledExecutor(daemonThread("northwind-chart-expiry"));

        String host = uriHost(bindAddress);
        this.downloadUri = URI.create("http://" + host + ":" + serverSocket.getLocalPort() + requestPath);
        try {
            expiryExecutor.schedule(this::close, lifetime.toNanos(), TimeUnit.NANOSECONDS);
            acceptExecutor.execute(this::acceptConnections);
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }
    }

    public static ChartDownloadServer startForLocalNetwork(byte[] image, String fileName) throws IOException {
        InetAddress localAddress = findLocalIpv4Address();
        return start(localAddress, image, fileName, Duration.ofMinutes(DOWNLOAD_LIFETIME_MINUTES));
    }

    static ChartDownloadServer start(InetAddress bindAddress, byte[] image, String fileName,
                                     Duration lifetime) throws IOException {
        return new ChartDownloadServer(bindAddress, image, fileName, lifetime);
    }

    public URI downloadUri() {
        return downloadUri;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Closing the listener is best effort; its accept loop exits either way.
            }
            for (Socket socket : activeSockets) {
                closeSocket(socket);
            }
            acceptExecutor.shutdownNow();
            for (Runnable pendingRequest : requestExecutor.shutdownNow()) {
                if (pendingRequest instanceof DownloadRequest downloadRequest) {
                    downloadRequest.closeSocket();
                }
            }
            expiryExecutor.shutdownNow();
        }
    }

    private void acceptConnections() {
        while (!closed.get()) {
            try {
                Socket socket = serverSocket.accept();
                activeSockets.add(socket);
                if (closed.get()) {
                    closeSocket(socket);
                    return;
                }
                DownloadRequest request = new DownloadRequest(socket);
                try {
                    requestExecutor.execute(request);
                } catch (RejectedExecutionException exception) {
                    request.closeSocket();
                }
            } catch (SocketException exception) {
                if (!closed.get()) {
                    close();
                }
                return;
            } catch (IOException exception) {
                if (!closed.get()) {
                    close();
                }
                return;
            }
        }
    }

    private void serve(Socket socket) {
        try (socket) {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REQUEST_TIMEOUT_MILLIS);
            InputStream input = new BufferedInputStream(socket.getInputStream());
            OutputStream output = socket.getOutputStream();
            String requestLine = readLine(socket, input, MAX_REQUEST_LINE_LENGTH, deadline);
            if (requestLine == null) {
                return;
            }
            String[] parts = requestLine.split(" ", 3);
            if (parts.length != 3 || !parts[0].matches("[A-Z]{1,10}")
                    || !("HTTP/1.0".equals(parts[2]) || "HTTP/1.1".equals(parts[2]))) {
                writeEmptyResponse(output, 400, "Bad Request", null);
                return;
            }
            if (!readHeaders(socket, input, deadline)) {
                writeEmptyResponse(output, 400, "Bad Request", null);
                return;
            }
            if (!"GET".equals(parts[0])) {
                writeEmptyResponse(output, 405, "Method Not Allowed", "Allow: GET\r\n");
                return;
            }
            if (!requestPath.equals(parts[1])) {
                writeEmptyResponse(output, 404, "Not Found", null);
                return;
            }
            writeImageResponse(output);
        } catch (IOException ignored) {
            // Drop incomplete requests, timeouts, and clients that disconnect mid-download.
        } finally {
            activeSockets.remove(socket);
        }
    }

    private static boolean readHeaders(Socket socket, InputStream input, long deadline) throws IOException {
        int totalBytes = 0;
        for (int headerCount = 0; headerCount < MAX_HEADER_COUNT; headerCount++) {
            String header = readLine(socket, input, MAX_HEADER_LINE_LENGTH, deadline);
            if (header == null) {
                return false;
            }
            totalBytes += header.length();
            if (totalBytes > MAX_HEADER_BYTES) {
                return false;
            }
            if (header.isEmpty()) {
                return true;
            }
            if (header.indexOf(':') <= 0) {
                return false;
            }
        }
        return false;
    }

    private static String readLine(Socket socket, InputStream input, int maximumLength, long deadline) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while (true) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new SocketTimeoutException("HTTP request exceeded its total time limit.");
            }
            long remainingMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
            socket.setSoTimeout((int) Math.min(REQUEST_TIMEOUT_MILLIS, remainingMillis));
            value = input.read();
            if (value == -1) {
                break;
            }
            if (value == '\n') {
                return line.toString(StandardCharsets.US_ASCII);
            }
            if (value != '\r') {
                if (line.size() >= maximumLength) {
                    throw new IOException("HTTP request line exceeds the configured limit.");
                }
                line.write(value);
            }
        }
        return line.size() == 0 ? null : line.toString(StandardCharsets.US_ASCII);
    }

    private void writeImageResponse(OutputStream output) throws IOException {
        String headers = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: image/png\r\n"
                + "Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n"
                + "Cache-Control: no-store\r\n"
                + "X-Content-Type-Options: nosniff\r\n"
                + "Content-Length: " + image.length + "\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(image);
        output.flush();
    }

    private static void writeEmptyResponse(OutputStream output, int status, String reason,
                                           String extraHeader) throws IOException {
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + (extraHeader == null ? "" : extraHeader)
                + "Cache-Control: no-store\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }

    private static InetAddress findLocalIpv4Address() throws IOException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        if (networkInterfaces == null) {
            throw new IOException("No se encontró una interfaz de red local.");
        }

        List<NetworkInterface> activeInterfaces = new ArrayList<>();
        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
            try {
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    activeInterfaces.add(networkInterface);
                }
            } catch (SocketException ignored) {
                // Skip interfaces that disappear while the list is being inspected.
            }
        }
        activeInterfaces.sort(Comparator.comparingInt(ChartDownloadServer::interfacePreference)
                .thenComparingInt(NetworkInterface::getIndex));

        for (NetworkInterface networkInterface : activeInterfaces) {
            if (interfacePreference(networkInterface) == 2) {
                continue;
            }
            for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                    return address;
                }
            }
        }
        throw new IOException("Conecta este equipo a una red Wi-Fi o Ethernet para generar un QR de descarga.");
    }

    private static int interfacePreference(NetworkInterface networkInterface) {
        String name = (networkInterface.getName() + " " + String.valueOf(networkInterface.getDisplayName()))
                .toLowerCase(Locale.ROOT);
        if (name.contains("vethernet") || name.contains("virtual") || name.contains("vmware")
                || name.contains("virtualbox") || name.contains("wsl") || name.contains("docker")
                || name.contains("loopback") || name.contains("vpn") || name.contains("wireguard")
                || name.contains("tailscale") || name.contains("zerotier") || name.contains("tunnel")) {
            return 2;
        }
        return name.contains("wi-fi") || name.contains("wifi") || name.contains("wlan")
                || name.contains("ethernet") || name.contains("en0") || name.matches(".*\\beth[0-9]+\\b.*")
                ? 0 : 1;
    }

    private static void validate(InetAddress bindAddress, byte[] image, String fileName, Duration lifetime) {
        if (bindAddress == null || bindAddress.isAnyLocalAddress() || image == null || image.length == 0
                || image.length > MAX_IMAGE_BYTES
                || fileName == null || !SAFE_PNG_NAME.matcher(fileName).matches()
                || lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("La descarga temporal del gráfico no tiene una configuración válida.");
        }
    }

    private static String randomToken() {
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        return java.util.HexFormat.of().formatHex(token);
    }

    private static String uriHost(InetAddress address) {
        String host = address.getHostAddress();
        return address instanceof Inet6Address ? "[" + host.replace("%", "%25") + "]" : host;
    }

    private static java.util.concurrent.ThreadFactory daemonThread(String name) {
        return task -> {
            Thread thread = new Thread(task, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    private void closeSocket(Socket socket) {
        activeSockets.remove(socket);
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing an inactive client socket is best effort.
        }
    }

    private final class DownloadRequest implements Runnable {
        private final Socket socket;

        private DownloadRequest(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            serve(socket);
        }

        private void closeSocket() {
            ChartDownloadServer.this.closeSocket(socket);
        }
    }
}
