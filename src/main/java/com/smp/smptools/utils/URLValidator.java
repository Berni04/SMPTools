package com.smp.smptools.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

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
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(Constants.URL_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(Constants.URL_READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(false);

        URL currentUrl = url;
        int redirectCount = 0;

        while (redirectCount < MAX_REDIRECTS) {
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
                conn = (HttpURLConnection) redirectUrl.openConnection();
                conn.setConnectTimeout(Constants.URL_CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(Constants.URL_READ_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(false);

                currentUrl = redirectUrl;
                redirectCount++;
            } else {
                break;
            }
        }

        if (redirectCount >= MAX_REDIRECTS) {
            conn.disconnect();
            throw new IOException("Too many redirects (exceeded " + MAX_REDIRECTS + " limit)");
        }

        return conn;
    }
}
