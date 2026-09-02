package com.smp.smptools.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for validating and securing URL connections.
 * Prevents SSRF attacks by validating hostnames, resolved IPs,
 * and manually handling HTTP redirects to block internal redirects.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class URLValidator {

    private static final int MAX_REDIRECTS = 10;

    private static final String[] BLOCKED_HOSTS = {
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1",
        "169.254.169.254",
        "metadata.google.internal"
    };

    private static final String[] ALLOWED_PROTOCOLS = {
        "http",
        "https"
    };

    private URLValidator() {
        // Prevent instantiation
    }

    /**
     * Validates and creates a URL from a string.
     * Checks protocol, host, and resolved IP to prevent SSRF attacks.
     *
     * @param urlString the URL string to validate
     * @return the validated URL
     * @throws MalformedURLException if the URL is malformed
     * @throws IllegalArgumentException if the URL fails validation
     */
    public static URL validateAndCreate(String urlString) throws MalformedURLException {
        if (urlString == null || urlString.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        URL url = new URL(urlString);
        validateUrl(url);
        return url;
    }

    /**
     * Validates a URL for safety (protocol, host, resolved IP).
     */
    public static void validateUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("URL cannot be null");
        }
        String protocol = url.getProtocol().toLowerCase();
        boolean allowed = false;
        for (String p : ALLOWED_PROTOCOLS) {
            if (protocol.equals(p)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
        }

        String host = url.getHost().toLowerCase();
        for (String blocked : BLOCKED_HOSTS) {
            if (host.equals(blocked)) {
                throw new IllegalArgumentException("Access to internal hosts is not allowed");
            }
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                throw new IllegalArgumentException("Could not resolve host: " + host);
            }
            for (InetAddress addr : addresses) {
                if (isPrivateOrBlockedIp(addr)) {
                    throw new IllegalArgumentException("Resolved IP address is in a blocked range: " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host: " + host);
        }
    }

    /**
     * Checks if an InetAddress is private, loopback, link-local, CGNAT, benchmark, or in a reserved blocked range.
     *
     * @param address the InetAddress to check
     * @return true if the address is in a blocked range
     */
    public static boolean isPrivateOrBlockedIp(InetAddress address) {
        if (address == null) {
            return true;
        }

        if (address.isLoopbackAddress() ||
            address.isAnyLocalAddress() ||
            address.isLinkLocalAddress() ||
            address.isSiteLocalAddress() ||
            address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            int b2 = bytes[2] & 0xFF;

            // 0.0.0.0/8 (Current network)
            if (b0 == 0) return true;
            // 10.0.0.0/8 (Private-Use)
            if (b0 == 10) return true;
            // 100.64.0.0/10 (Shared Address Space / CGNAT)
            if (b0 == 100 && (b1 & 0xC0) == 64) return true;
            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) return true;
            // 169.254.0.0/16 (Link Local)
            if (b0 == 169 && b1 == 254) return true;
            // 172.16.0.0/12 (Private-Use)
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.0.0.0/24 (IETF Protocol Assignments)
            if (b0 == 192 && b1 == 0 && b2 == 0) return true;
            // 192.168.0.0/16 (Private-Use)
            if (b0 == 192 && b1 == 168) return true;
            // 198.18.0.0/15 (Benchmarking)
            if (b0 == 198 && (b1 & 0xFE) == 18) return true;
            // 240.0.0.0/4 (Reserved)
            if ((b0 & 0xF0) == 240) return true;
        } else if (bytes.length == 16) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // fc00::/7 (Unique Local Address / ULA)
            if ((b0 & 0xFE) == 0xFC) return true;
            // fe80::/10 (Link-Local)
            if (b0 == 0xFE && (b1 & 0xC0) == 0x80) return true;

            // ::ffff:0:0/96 (IPv4-mapped IPv6)
            boolean isIPv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) { isIPv4Mapped = false; break; }
            }
            if (isIPv4Mapped && bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF) {
                try {
                    byte[] v4 = new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]};
                    return isPrivateOrBlockedIp(InetAddress.getByAddress(v4));
                } catch (UnknownHostException ignored) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Opens a safe connection to a URL with timeouts and redirect validation.
     * Disables automatic redirects and validates each redirect target.
     *
     * @param url the URL to connect to
     * @return the URLConnection
     * @throws IOException if the connection fails, redirect target is blocked, or redirect limit is exceeded
     */
    public static URLConnection openConnection(URL url) throws IOException {
        validateUrl(url);

        URL currentUrl = url;
        int redirectCount = 0;
        HttpURLConnection conn = null;

        while (redirectCount < MAX_REDIRECTS) {
            String host = currentUrl.getHost();
            InetAddress[] addresses;
            try {
                addresses = InetAddress.getAllByName(host);
            } catch (UnknownHostException e) {
                throw new IOException("Could not resolve host: " + host, e);
            }
            if (addresses == null || addresses.length == 0) {
                throw new IOException("Could not resolve host: " + host);
            }
            for (InetAddress addr : addresses) {
                if (isPrivateOrBlockedIp(addr)) {
                    throw new IOException("Resolved IP address is in a blocked range: " + addr.getHostAddress());
                }
            }

            if ("https".equalsIgnoreCase(currentUrl.getProtocol())) {
                conn = (HttpURLConnection) currentUrl.openConnection(java.net.Proxy.NO_PROXY);
                conn.setConnectTimeout(Constants.URL_CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(Constants.URL_READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(false);
                if (conn instanceof javax.net.ssl.HttpsURLConnection httpsConn) {
                    httpsConn.setSSLSocketFactory(new PinningSSLSocketFactory(
                            (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault(),
                            addresses,
                            Constants.URL_CONNECT_TIMEOUT_MS));
                }
            } else {
                conn = new PinnedHttpURLConnection(currentUrl, addresses, Constants.URL_CONNECT_TIMEOUT_MS, Constants.URL_READ_TIMEOUT_MS);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 ||
                responseCode == 308) {

                String location = conn.getHeaderField("Location");
                if (location == null || location.isEmpty()) {
                    break;
                }

                URL redirectUrl = new URL(currentUrl, location);
                validateUrl(redirectUrl);

                conn.disconnect();
                currentUrl = redirectUrl;
                redirectCount++;
            } else {
                break;
            }
        }

        if (redirectCount >= MAX_REDIRECTS) {
            if (conn != null) {
                conn.disconnect();
            }
            throw new IOException("Too many redirects (exceeded " + MAX_REDIRECTS + " limit)");
        }

        return conn;
    }

    private static class PinningSSLSocketFactory extends javax.net.ssl.SSLSocketFactory {
        private final javax.net.ssl.SSLSocketFactory delegate;
        private final InetAddress[] pinnedAddresses;
        private final int connectTimeoutMs;

        public PinningSSLSocketFactory(javax.net.ssl.SSLSocketFactory delegate, InetAddress[] pinnedAddresses, int connectTimeoutMs) {
            this.delegate = delegate;
            this.pinnedAddresses = pinnedAddresses;
            this.connectTimeoutMs = connectTimeoutMs;
        }

        private Socket connectSocket(int port) throws IOException {
            IOException lastEx = null;
            for (InetAddress addr : pinnedAddresses) {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(addr, port), connectTimeoutMs);
                    return socket;
                } catch (IOException e) {
                    lastEx = e;
                }
            }
            throw lastEx != null ? lastEx : new IOException("Could not connect to any resolved IP address");
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            Socket socket = connectSocket(port);
            return delegate.createSocket(socket, host, port, true);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            Socket socket = new Socket();
            socket.bind(new InetSocketAddress(localHost, localPort));
            socket.connect(new InetSocketAddress(pinnedAddresses[0], port), connectTimeoutMs);
            return delegate.createSocket(socket, host, port, true);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            Socket socket = connectSocket(port);
            return delegate.createSocket(socket, host.getHostName(), port, true);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            Socket socket = new Socket();
            socket.bind(new InetSocketAddress(localAddress, localPort));
            socket.connect(new InetSocketAddress(pinnedAddresses[0], port), connectTimeoutMs);
            return delegate.createSocket(socket, address.getHostName(), port, true);
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            if (s != null && s.isConnected()) {
                if (isPrivateOrBlockedIp(s.getInetAddress())) {
                    throw new IOException("Connected socket address is in a blocked range: " + s.getInetAddress());
                }
                return delegate.createSocket(s, host, port, autoClose);
            }
            Socket connected = connectSocket(port);
            return delegate.createSocket(connected, host, port, true);
        }
    }

    private static class PinnedHttpURLConnection extends HttpURLConnection {
        private final InetAddress[] pinnedAddresses;
        private final int connectTimeoutMs;
        private final int readTimeoutMs;
        private Socket socket;
        private InputStream inputStream;
        private final Map<String, List<String>> headerFields = new LinkedHashMap<>();

        public PinnedHttpURLConnection(URL url, InetAddress[] pinnedAddresses, int connectTimeoutMs, int readTimeoutMs) {
            super(url);
            this.pinnedAddresses = pinnedAddresses;
            this.connectTimeoutMs = connectTimeoutMs;
            this.readTimeoutMs = readTimeoutMs;
        }

        @Override
        public void connect() throws IOException {
            if (connected) return;
            int port = url.getPort() != -1 ? url.getPort() : 80;
            IOException lastEx = null;
            for (InetAddress addr : pinnedAddresses) {
                try {
                    Socket s = new Socket();
                    s.setSoTimeout(readTimeoutMs);
                    s.connect(new InetSocketAddress(addr, port), connectTimeoutMs);
                    this.socket = s;
                    break;
                } catch (IOException e) {
                    lastEx = e;
                }
            }
            if (socket == null) {
                throw lastEx != null ? lastEx : new IOException("Could not connect to any resolved IP address");
            }

            String hostHeader = url.getHost() + (url.getPort() != -1 && url.getPort() != 80 ? ":" + url.getPort() : "");
            String file = url.getFile();
            if (file == null || file.isEmpty()) file = "/";

            OutputStream os = socket.getOutputStream();
            String request = "GET " + file + " HTTP/1.1\r\n" +
                             "Host: " + hostHeader + "\r\n" +
                             "User-Agent: SMPTools\r\n" +
                             "Connection: close\r\n\r\n";
            os.write(request.getBytes(StandardCharsets.US_ASCII));
            os.flush();

            InputStream rawIn = socket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(rawIn);
            String statusLine = readLine(bis);
            if (statusLine == null) {
                throw new IOException("Premature EOF from HTTP server");
            }
            String[] parts = statusLine.split(" ", 3);
            if (parts.length >= 2) {
                try {
                    this.responseCode = Integer.parseInt(parts[1]);
                    this.responseMessage = parts.length > 2 ? parts[2] : "";
                } catch (NumberFormatException e) {
                    this.responseCode = 200;
                }
            } else {
                this.responseCode = 200;
            }

            String line;
            while ((line = readLine(bis)) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String headerName = line.substring(0, colon).trim();
                    String headerVal = line.substring(colon + 1).trim();
                    headerFields.computeIfAbsent(headerName, k -> new ArrayList<>()).add(headerVal);
                }
            }

            this.inputStream = bis;
            this.connected = true;
        }

        private static String readLine(InputStream is) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                if (b == '\r') {
                    is.mark(1);
                    int next = is.read();
                    if (next != '\n' && next != -1) {
                        is.reset();
                    }
                    break;
                } else if (b == '\n') {
                    break;
                }
                baos.write(b);
            }
            if (b == -1 && baos.size() == 0) return null;
            return baos.toString(StandardCharsets.US_ASCII);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return inputStream;
        }

        @Override
        public int getResponseCode() throws IOException {
            connect();
            return responseCode;
        }

        @Override
        public String getHeaderField(String name) {
            try {
                connect();
            } catch (IOException ignored) {}
            for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    List<String> list = entry.getValue();
                    return (list != null && !list.isEmpty()) ? list.get(list.size() - 1) : null;
                }
            }
            return null;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            try {
                connect();
            } catch (IOException ignored) {}
            return Collections.unmodifiableMap(headerFields);
        }

        @Override
        public void disconnect() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }

        @Override
        public boolean usingProxy() {
            return false;
        }
    }
}
