package com.smp.smptools.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Utility class for validating and securing URL connections.
 * Prevents SSRF attacks by validating both hostnames and resolved IP addresses.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class URLValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "[::1]",
        "169.254.169.254",
        "metadata.google.internal"
    );

    private static final Set<String> ALLOWED_PROTOCOLS = Set.of(
        "http",
        "https"
    );

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

        // Check protocol
        if (!ALLOWED_PROTOCOLS.contains(url.getProtocol().toLowerCase())) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
        }

        // Check for blocked hostnames
        String host = url.getHost().toLowerCase();
        if (BLOCKED_HOSTS.contains(host)) {
            throw new IllegalArgumentException("Access to internal hosts is not allowed");
        }

        // Check for private IP ranges in hostname
        if (host.matches("^10\\..*") || host.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*") || host.matches("^192\\.168\\..*")) {
            throw new IllegalArgumentException("Access to private IP addresses is not allowed");
        }

        // Resolve hostname and check resolved IP (prevents DNS rebinding)
        try {
            InetAddress resolved = InetAddress.getByName(host);
            String resolvedIp = resolved.getHostAddress();
            
            if (isPrivateOrBlockedIp(resolvedIp)) {
                throw new IllegalArgumentException("Resolved IP address is in a blocked range: " + resolvedIp);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host: " + host);
        }

        return url;
    }

    /**
     * Checks if an IP address is private, loopback, or link-local.
     *
     * @param ip the IP address string to check
     * @return true if the IP is in a blocked range
     */
    private static boolean isPrivateOrBlockedIp(String ip) {
        // Check for loopback addresses
        if (ip.startsWith("127.") || ip.equals("0.0.0.0") || ip.equals("[::1]") || ip.equals("::1")) {
            return true;
        }

        // Check for link-local addresses (169.254.x.x)
        if (ip.startsWith("169.254.")) {
            return true;
        }

        // Check for private IP ranges
        if (ip.startsWith("10.") || 
            ip.startsWith("192.168.") ||
            ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) {
            return true;
        }

        // Check for IPv6 loopback
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return true;
        }

        return false;
    }

    /**
     * Opens a connection to a URL with timeouts.
     *
     * @param url the URL to connect to
     * @return the URLConnection
     * @throws IOException if the connection fails
     */
    public static URLConnection openConnection(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(Constants.URL_CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(Constants.URL_READ_TIMEOUT_MS);
        return conn;
    }
}
